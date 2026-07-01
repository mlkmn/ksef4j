package io.github.mlkmn.ksef4j.smoke;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.UpoSignatureVerifier;
import io.github.mlkmn.ksef4j.invoice.Address;
import io.github.mlkmn.ksef4j.invoice.Buyer;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.Item;
import io.github.mlkmn.ksef4j.invoice.Seller;
import io.github.mlkmn.ksef4j.invoice.VatRate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live happy-path smoke test against the KSeF {@code test} environment. Opt-in:
 * excluded from {@code build}; run with {@code ./gradlew :ksef4j-core:smokeTest}
 * after exporting {@code KSEF_TOKEN} and {@code COMPANY_NIP}. Self-skips when those
 * are absent. For raw HTTP wire detail, rerun with
 * {@code -Djdk.httpclient.HttpClient.log=requests,headers}.
 */
@Tag("smoke")
class LiveKsefSmokeTest {

    @Test
    void sends_invoice_to_test_environment_and_receives_upo() {
        String token = System.getenv("KSEF_TOKEN");
        String nip = System.getenv("COMPANY_NIP");
        assumeTrue(token != null && !token.isBlank() && nip != null && !nip.isBlank(),
                "KSEF_TOKEN and COMPANY_NIP must be set to run the live smoke test");

        KsefClient client = KsefClient.builder()
                .environment(Environment.TEST)
                .tokenAuth(token, nip)
                .build();

        Invoice invoice = smokeInvoice(nip);
        System.out.println("[smoke] sending invoice " + invoice.invoiceNumber()
                + " as issuer NIP " + nip + " to KSeF TEST");

        Instant startSend = Instant.now();
        Upo upo;
        try (SendResult result = client.send(invoice)) {
            String invoiceRef = result.invoiceReferenceNumber();
            long sendMs = Duration.between(startSend, Instant.now()).toMillis();
            System.out.println("[smoke] send accepted; invoiceReferenceNumber=" + invoiceRef
                    + " (send phase " + sendMs + " ms)");

            Instant startUpo = Instant.now();
            upo = result.awaitUpo();
            long upoMs = Duration.between(startUpo, Instant.now()).toMillis();
            System.out.println("[smoke] UPO received (poll phase " + upoMs + " ms)");
        }

        assertThat(upo).isNotNull();
        assertThat(upo.ksefReferenceNumber()).isNotBlank();
        assertThat(upo.issuedAt()).isNotNull();
        assertThat(upo.xml()).isNotEmpty();

        // The bundled TEST signing cert must verify the real Ministry-signed UPO.
        new UpoSignatureVerifier().verify(upo.xml(), Environment.TEST);
        System.out.println("[smoke] confirmed: UPO Ministry signature verified against the bundled TEST cert");

        // Observed from the public result.
        System.out.println("[smoke] KSeF reference number: " + upo.ksefReferenceNumber());
        System.out.println("[smoke] UPO reference number:  " + upo.upoReferenceNumber());
        System.out.println("[smoke] UPO issued at:         " + upo.issuedAt());
        System.out.println("[smoke] UPO XML size (bytes):  " + upo.xml().length);
        // Inferred from a successful round-trip (logged, not asserted).
        System.out.println("[smoke] confirmed: TEST public-key cert accepted (authentication succeeded)");
        System.out.println("[smoke] confirmed: invoice encryption accepted by server (send succeeded)");
        System.out.println("[smoke] confirmed: session close succeeded (UPO requires it)");
        System.out.println("[smoke] confirmed: UPO pre-signed URL fetch succeeded");
        System.out.println("[smoke] for raw HTTP detail rerun with -Djdk.httpclient.HttpClient.log=requests,headers");
    }

    @Test
    void sends_eur_invoice_with_unit_and_pkwiu_to_test_environment_and_receives_upo() {
        String token = System.getenv("KSEF_TOKEN");
        String nip = System.getenv("COMPANY_NIP");
        assumeTrue(token != null && !token.isBlank() && nip != null && !nip.isBlank(),
                "KSEF_TOKEN and COMPANY_NIP must be set to run the live smoke test");

        KsefClient client = KsefClient.builder()
                .environment(Environment.TEST)
                .tokenAuth(token, nip)
                .build();

        Invoice invoice = eurSmokeInvoice(nip);
        System.out.println("[smoke-eur] sending EUR invoice " + invoice.invoiceNumber()
                + " (rate " + invoice.exchangeRate() + ") as issuer NIP " + nip + " to KSeF TEST");

        Upo upo;
        try (SendResult result = client.send(invoice)) {
            System.out.println("[smoke-eur] send accepted; invoiceReferenceNumber="
                    + result.invoiceReferenceNumber());
            upo = result.awaitUpo();
            System.out.println("[smoke-eur] UPO received");
        }

        assertThat(upo).isNotNull();
        assertThat(upo.ksefReferenceNumber()).isNotBlank();
        assertThat(upo.issuedAt()).isNotNull();
        assertThat(upo.xml()).isNotEmpty();

        System.out.println("[smoke-eur] KSeF reference number: " + upo.ksefReferenceNumber());
        System.out.println("[smoke-eur] UPO issued at:         " + upo.issuedAt());
        // The acceptance itself confirms KSeF took KodWaluty=EUR, KursWalutyZ, the
        // per-band P_14_xW VAT-in-PLN twin, the P_8A unit, and the PKWiU code.
        System.out.println("[smoke-eur] confirmed: EUR invoice (KursWalutyZ + P_14_xW + unit + PKWiU) accepted");
    }

    private static Invoice smokeInvoice(String sellerNip) {
        Address sellerAddress = new Address("PL", "ul. Marszalkowska 1/2", "00-000 Warszawa", null);
        Address buyerAddress = new Address("PL", "ul. Pulawska 100", null, null);
        return new Invoice(
                "ksef4j-smoke-" + System.currentTimeMillis(),
                LocalDate.now(),
                null,
                null,
                null,
                new Seller(sellerNip, "ksef4j smoke test seller", sellerAddress),
                new Buyer("1111111111", "ksef4j smoke test buyer", buyerAddress),
                List.of(new Item("Smoke test line item", new BigDecimal("1"),
                        new BigDecimal("100.00"), VatRate.VAT_23, null, null)));
    }

    /**
     * Mirrors the shape of the real motivating invoices: domestic B2B billed in EUR,
     * a single 23% line priced per hour, with a PKWiU code. The buyer NIP is fictional
     * ({@code 1111111111}) because the TEST environment forbids real third-party NIPs.
     */
    private static Invoice eurSmokeInvoice(String sellerNip) {
        Address sellerAddress = new Address("PL", "ul. Marszalkowska 1/2", "00-000 Warszawa", null);
        Address buyerAddress = new Address("PL", "ul. Pulawska 100", null, null);
        return new Invoice(
                "ksef4j-smoke-eur-" + System.currentTimeMillis(),
                LocalDate.now(),
                null,
                "EUR",
                new BigDecimal("4.2489"),
                new Seller(sellerNip, "ksef4j smoke test seller", sellerAddress),
                new Buyer("1111111111", "ksef4j smoke test buyer", buyerAddress),
                List.of(new Item("Software development services", new BigDecimal("10"),
                        new BigDecimal("100.00"), VatRate.VAT_23, "godzina", "62.01.11.0")));
    }
}
