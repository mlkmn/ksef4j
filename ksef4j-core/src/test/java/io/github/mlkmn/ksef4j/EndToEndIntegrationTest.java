package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import io.github.mlkmn.ksef4j.internal.http.FakeKsef;
import io.github.mlkmn.ksef4j.internal.http.KsefHappyPath;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndToEndIntegrationTest {

    @Test
    void full_happy_path_sends_invoice_polls_upo_and_archives(@TempDir Path archiveDir) throws Exception {
        try (FakeKsef fake = new FakeKsef()) {
            KsefHappyPath.install(fake);
            KsefClient client = KsefClient.builder()
                    .environment(Environment.TEST)
                    .baseUrl(fake.baseUri())
                    .tokenAuth("test-token", KsefHappyPath.NIP)
                    .archiveDirectory(archiveDir)
                    .build();

            Upo upo;
            try (SendResult result = client.send(InvoiceFixtures.singleLineVat23())) {
                assertThat(result.invoiceReferenceNumber()).isEqualTo(KsefHappyPath.INVOICE_REF);
                upo = result.awaitUpo();
            }

            assertThat(upo.ksefReferenceNumber()).isEqualTo(KsefHappyPath.KSEF_NUMBER);
            assertThat(upo.upoReferenceNumber()).isEqualTo(KsefHappyPath.UPO_PAGE_REF);
            assertThat(upo.issuedAt()).isEqualTo(Instant.parse(KsefHappyPath.UPO_ISSUED_AT));
            assertThat(new String(upo.xml(), StandardCharsets.UTF_8)).contains(KsefHappyPath.KSEF_NUMBER);

            Path entryDir = archiveDir.resolve(KsefHappyPath.NIP).resolve(KsefHappyPath.KSEF_NUMBER);
            assertThat(entryDir.resolve("fa3.xml")).exists();
            assertThat(entryDir.resolve("upo.xml")).exists();
            assertThat(entryDir.resolve("metadata.json")).exists();
            assertThat(Files.readAllBytes(entryDir.resolve("upo.xml"))).isEqualTo(upo.xml());

            List<String> paths = fake.requests.stream().map(FakeKsef.RecordedRequest::path).toList();
            assertThat(paths).containsSubsequence(
                    "/auth/challenge", "/auth/ksef-token", "/auth/REF1", "/auth/token/redeem",
                    "/sessions/online", "/sessions/online/SESS1/invoices",
                    "/sessions/online/SESS1/close",
                    "/sessions/SESS1/invoices/INV1",
                    "/sessions/SESS1", "/sessions/SESS1", "/upo/U1");
            assertThat(paths).filteredOn("/sessions/SESS1"::equals).hasSizeGreaterThanOrEqualTo(2);

            FakeKsef.RecordedRequest openSession = fake.requests.stream()
                    .filter(r -> r.path().equals("/sessions/online"))
                    .findFirst()
                    .orElseThrow();
            assertThat(openSession.headers()).containsKey("Authorization");
        }
    }

    @Test
    void business_error_on_send_propagates_and_does_not_archive(@TempDir Path archiveDir) throws Exception {
        try (FakeKsef fake = new FakeKsef()) {
            KsefHappyPath.install(fake);
            // Override send-invoice with a KSeF business-error envelope (HTTP 400).
            fake.stubJson("/sessions/online/" + KsefHappyPath.SESSION_REF + "/invoices", 400,
                    "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21401,"
                            + "\"exceptionDescription\":\"Invoice rejected\",\"details\":[]}],"
                            + "\"serviceCode\":\"SVC\",\"timestamp\":\"x\"}}");

            KsefClient client = KsefClient.builder()
                    .environment(Environment.TEST)
                    .baseUrl(fake.baseUri())
                    .tokenAuth("test-token", KsefHappyPath.NIP)
                    .archiveDirectory(archiveDir)
                    .build();

            assertThatThrownBy(() -> client.send(InvoiceFixtures.singleLineVat23()))
                    .isInstanceOf(KsefBusinessException.class)
                    .hasMessageContaining("21401");

            // Best-effort session close was still attempted after the failed send.
            List<String> paths = fake.requests.stream().map(FakeKsef.RecordedRequest::path).toList();
            assertThat(paths).contains("/sessions/online/" + KsefHappyPath.SESSION_REF + "/close");

            // Nothing archived on failure (archive happens only during awaitUpo()).
            assertThat(archiveDir.resolve(KsefHappyPath.NIP)).doesNotExist();
        }
    }

    @Test
    void eur_send_archives_fa3_upo_and_metadata_to_disk(@TempDir Path archiveDir) throws Exception {
        try (FakeKsef fake = KsefHappyPath.install(new FakeKsef())) {
            KsefClient client = KsefClient.builder()
                    .environment(Environment.TEST)
                    .baseUrl(fake.baseUri())
                    .tokenAuth("tkn", KsefHappyPath.NIP)
                    .archiveDirectory(archiveDir)
                    .build();

            Upo upo;
            try (SendResult result = client.send(InvoiceFixtures.eurSingleLineVat23())) {
                upo = result.awaitUpo();
            }

            Path entryDir = archiveDir.resolve(KsefHappyPath.NIP).resolve(upo.ksefReferenceNumber());
            String fa3 = Files.readString(entryDir.resolve("fa3.xml"));
            assertThat(fa3).contains("EUR").contains("KursWalutyZ");
            assertThat(Files.size(entryDir.resolve("upo.xml"))).isPositive();
            String metadata = Files.readString(entryDir.resolve("metadata.json"));
            assertThat(metadata)
                    .contains(upo.ksefReferenceNumber())
                    .contains(KsefHappyPath.INVOICE_NUMBER)
                    .contains(upo.documentHash())
                    .contains(KsefHappyPath.NIP);
        }
    }

    @Test
    void send_throws_when_upo_document_hash_does_not_match(@TempDir Path archiveDir) throws Exception {
        try (FakeKsef fake = KsefHappyPath.install(new FakeKsef())) {
            // Override the UPO with a wrong document hash.
            fake.stubDynamic("/upo/" + KsefHappyPath.UPO_PAGE_REF, requests ->
                    FakeKsef.Stub.bytes(200, ("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <Potwierdzenie xmlns="http://upo.schematy.mf.gov.pl/KSeF/v4-3">
                              <Dokument>
                                <NumerKSeFDokumentu>%s</NumerKSeFDokumentu>
                                <NumerFaktury>%s</NumerFaktury>
                                <DataNadaniaNumeruKSeF>%s</DataNadaniaNumeruKSeF>
                                <SkrotDokumentu>TUlTTUFUQ0gtSEFTSA==</SkrotDokumentu>
                              </Dokument>
                            </Potwierdzenie>
                            """).formatted(KsefHappyPath.KSEF_NUMBER, KsefHappyPath.INVOICE_NUMBER,
                            KsefHappyPath.UPO_ISSUED_AT).getBytes(StandardCharsets.UTF_8),
                            "application/xml"));

            KsefClient client = KsefClient.builder()
                    .environment(Environment.TEST)
                    .baseUrl(fake.baseUri())
                    .tokenAuth("tkn", KsefHappyPath.NIP)
                    .archiveDirectory(archiveDir)
                    .build();

            try (SendResult result = client.send(InvoiceFixtures.eurSingleLineVat23())) {
                assertThatThrownBy(result::awaitUpo).isInstanceOf(UpoVerificationException.class);
            }
            // Nothing archived: the per-NIP directory was never created.
            assertThat(archiveDir.resolve(KsefHappyPath.NIP)).doesNotExist();
        }
    }
}
