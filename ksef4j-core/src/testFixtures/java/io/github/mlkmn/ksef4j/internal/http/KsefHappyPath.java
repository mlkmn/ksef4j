package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.internal.http.FakeKsef.RecordedRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wires a full happy-path KSeF conversation onto a {@link FakeKsef}: challenge ->
 * ksef-token -> auth-status -> redeem -> open-session -> send-invoice -> close ->
 * session-status (in-progress then UPO-ready) -> UPO download. Auth status is ready
 * on the first poll; session status returns one HTTP-200 "in progress" with empty
 * UPO pages before the ready response, exercising the real poll loop and the
 * populate-after-200 guard.
 */
public final class KsefHappyPath {

    public static final String NIP = "5260250274";
    public static final String SESSION_REF = "SESS1";
    public static final String INVOICE_REF = "INV1";
    public static final String UPO_PAGE_REF = "U1";
    public static final String KSEF_NUMBER = "5260250274-20260629-010001234567-AB";
    public static final String UPO_ISSUED_AT = "2026-06-29T10:15:30Z";
    public static final String INVOICE_NUMBER = "FV/E2E/001";

    private static final String VALID_UNTIL = "2027-01-01T00:00:00Z";
    private static final Pattern INVOICE_HASH = Pattern.compile("\"invoiceHash\"\\s*:\\s*\"([^\"]+)\"");

    private static final String UPO_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Potwierdzenie xmlns="http://upo.schematy.mf.gov.pl/KSeF/v4-3">
              <Dokument>
                <NumerKSeFDokumentu>%s</NumerKSeFDokumentu>
                <NumerFaktury>%s</NumerFaktury>
                <DataNadaniaNumeruKSeF>%s</DataNadaniaNumeruKSeF>
                <SkrotDokumentu>%s</SkrotDokumentu>
              </Dokument>
            </Potwierdzenie>
            """;

    private KsefHappyPath() {
    }

    /** Install all happy-path stubs onto {@code fake}; returns it for chaining. */
    public static FakeKsef install(FakeKsef fake) {
        installAuth(fake);
        fake.stubJson("/sessions/online", 200,
                "{\"referenceNumber\":\"" + SESSION_REF + "\",\"validUntil\":\"" + VALID_UNTIL + "\"}");
        fake.stubJson("/sessions/online/" + SESSION_REF + "/invoices", 202,
                "{\"referenceNumber\":\"" + INVOICE_REF + "\"}");
        fake.stubBytes("/sessions/online/" + SESSION_REF + "/close", 204, new byte[0], "application/json");
        fake.stubJson("/sessions/" + SESSION_REF + "/invoices/" + INVOICE_REF, 200,
                "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}");
        fake.stubSequence("/sessions/" + SESSION_REF,
                FakeKsef.Stub.json(200,
                        "{\"status\":{\"code\":200,\"description\":\"in progress\",\"details\":[]},"
                                + "\"invoiceCount\":0,\"upo\":{\"pages\":[]}}"),
                FakeKsef.Stub.json(200,
                        "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]},"
                                + "\"invoiceCount\":1,\"upo\":{\"pages\":[{\"referenceNumber\":\"" + UPO_PAGE_REF
                                + "\",\"downloadUrl\":\"" + fake.baseUri() + "/upo/" + UPO_PAGE_REF
                                + "\",\"downloadUrlExpirationDate\":\"" + VALID_UNTIL + "\"}]}}"));
        fake.stubDynamic("/upo/" + UPO_PAGE_REF, requests -> {
            String hash = submittedInvoiceHash(requests);
            String xml = UPO_TEMPLATE.formatted(KSEF_NUMBER, INVOICE_NUMBER, UPO_ISSUED_AT, hash);
            return FakeKsef.Stub.bytes(200, xml.getBytes(StandardCharsets.UTF_8), "application/xml");
        });
        return fake;
    }

    /**
     * Stub the auth endpoints needed to obtain an access token, then the metadata query endpoint
     * returning {@code pageJsonBodies} in sequence (the last body is sticky).
     */
    public static void stubQueryPages(FakeKsef fake, String... pageJsonBodies) {
        installAuth(fake);
        FakeKsef.Stub[] pages = new FakeKsef.Stub[pageJsonBodies.length];
        for (int i = 0; i < pageJsonBodies.length; i++) {
            pages[i] = FakeKsef.Stub.json(200, pageJsonBodies[i]);
        }
        fake.stubSequence("/invoices/query/metadata", pages);
    }

    private static void installAuth(FakeKsef fake) {
        fake.stubJson("/auth/challenge", 200,
                "{\"challenge\":\"abc\",\"timestamp\":\"2026-06-29T10:00:00Z\","
                        + "\"timestampMs\":1782000000000,\"clientIp\":\"1.2.3.4\"}");
        fake.stubJson("/auth/ksef-token", 202,
                "{\"authenticationToken\":{\"token\":\"AUTHJWT\",\"validUntil\":\"" + VALID_UNTIL + "\"},"
                        + "\"referenceNumber\":\"REF1\"}");
        fake.stubJson("/auth/REF1", 200,
                "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}");
        fake.stubJson("/auth/token/redeem", 200,
                "{\"accessToken\":{\"token\":\"ACC\",\"validUntil\":\"" + VALID_UNTIL + "\"},"
                        + "\"refreshToken\":{\"token\":\"REF\",\"validUntil\":\"" + VALID_UNTIL + "\"}}");
    }

    private static String submittedInvoiceHash(List<RecordedRequest> requests) {
        for (int i = requests.size() - 1; i >= 0; i--) {
            RecordedRequest r = requests.get(i);
            if ("POST".equals(r.method()) && r.path().endsWith("/invoices")) {
                Matcher m = INVOICE_HASH.matcher(r.body());
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        throw new IllegalStateException("No invoice submission recorded before UPO fetch");
    }
}
