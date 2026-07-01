package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import io.github.mlkmn.ksef4j.test.MockKsef;
import io.github.mlkmn.ksef4j.test.MockKsefDefaults;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndToEndIntegrationTest {

  private static final String NIP = "5260250274";

  @Test
  void full_happy_path_sends_invoice_polls_upo_and_archives(@TempDir Path archiveDir)
      throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(mock.baseUrl())
              .tokenAuth("test-token", NIP)
              .archiveDirectory(archiveDir)
              .build();

      Upo upo;
      try (SendResult result = client.send(InvoiceFixtures.singleLineVat23())) {
        assertThat(result.invoiceReferenceNumber()).isEqualTo(MockKsefDefaults.INVOICE_REF);
        upo = result.awaitUpo();
      }

      assertThat(upo.ksefNumber()).isEqualTo(MockKsefDefaults.KSEF_NUMBER);
      assertThat(upo.upoReferenceNumber()).isEqualTo(MockKsefDefaults.UPO_PAGE_REF);
      assertThat(upo.issuedAt()).isEqualTo(Instant.parse(MockKsefDefaults.UPO_ISSUED_AT));
      assertThat(new String(upo.xml(), StandardCharsets.UTF_8))
          .contains(MockKsefDefaults.KSEF_NUMBER);

      Path entryDir = archiveDir.resolve(NIP).resolve(MockKsefDefaults.KSEF_NUMBER);
      assertThat(entryDir.resolve("fa3.xml")).exists();
      assertThat(entryDir.resolve("upo.xml")).exists();
      assertThat(entryDir.resolve("metadata.json")).exists();
      assertThat(Files.readAllBytes(entryDir.resolve("upo.xml"))).isEqualTo(upo.xml());

      List<String> paths = mock.requestedUrls();
      assertThat(paths)
          .containsSubsequence(
              "/auth/challenge",
              "/auth/ksef-token",
              "/auth/REF1",
              "/auth/token/redeem",
              "/sessions/online",
              "/sessions/online/" + MockKsefDefaults.SESSION_REF + "/invoices",
              "/sessions/online/" + MockKsefDefaults.SESSION_REF + "/close",
              "/sessions/"
                  + MockKsefDefaults.SESSION_REF
                  + "/invoices/"
                  + MockKsefDefaults.INVOICE_REF,
              "/sessions/" + MockKsefDefaults.SESSION_REF,
              "/sessions/" + MockKsefDefaults.SESSION_REF,
              "/upo/" + MockKsefDefaults.UPO_PAGE_REF);
      assertThat(paths)
          .filteredOn(("/sessions/" + MockKsefDefaults.SESSION_REF)::equals)
          .hasSizeGreaterThanOrEqualTo(2);

      assertThat(mock.firstRequestHeaders("/sessions/online")).containsKey("authorization");
    }
  }

  @Test
  void business_error_on_send_propagates_and_does_not_archive(@TempDir Path archiveDir)
      throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      mock.onSend().reject(21401, "Invoice rejected");

      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(mock.baseUrl())
              .tokenAuth("test-token", NIP)
              .archiveDirectory(archiveDir)
              .build();

      assertThatThrownBy(() -> client.send(InvoiceFixtures.singleLineVat23()))
          .isInstanceOf(KsefBusinessException.class)
          .hasMessageContaining("21401");

      // Best-effort session close was still attempted after the failed send.
      List<String> paths = mock.requestedUrls();
      assertThat(paths).contains("/sessions/online/" + MockKsefDefaults.SESSION_REF + "/close");

      // Nothing archived on failure (archive happens only during awaitUpo()).
      assertThat(archiveDir.resolve(NIP)).doesNotExist();
    }
  }

  @Test
  void eur_send_archives_fa3_upo_and_metadata_to_disk(@TempDir Path archiveDir) throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(mock.baseUrl())
              .tokenAuth("tkn", NIP)
              .archiveDirectory(archiveDir)
              .build();

      Upo upo;
      try (SendResult result = client.send(InvoiceFixtures.eurSingleLineVat23())) {
        upo = result.awaitUpo();
      }

      Path entryDir = archiveDir.resolve(NIP).resolve(upo.ksefNumber());
      String fa3 = Files.readString(entryDir.resolve("fa3.xml"));
      assertThat(fa3).contains("EUR").contains("KursWalutyZ");
      assertThat(Files.size(entryDir.resolve("upo.xml"))).isPositive();
      String metadata = Files.readString(entryDir.resolve("metadata.json"));
      assertThat(metadata).contains(upo.ksefNumber()).contains(upo.documentHash()).contains(NIP);
    }
  }

  @Test
  void send_throws_when_upo_document_hash_does_not_match(@TempDir Path archiveDir)
      throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      // Override the UPO with a wrong document hash.
      mock.onUpo()
          .returnsXml(
              """
              <?xml version="1.0" encoding="UTF-8"?>
              <Potwierdzenie xmlns="http://upo.schematy.mf.gov.pl/KSeF/v4-3">
                <Dokument>
                  <NumerKSeFDokumentu>%s</NumerKSeFDokumentu>
                  <NumerFaktury>FV/MOCK/001</NumerFaktury>
                  <DataNadaniaNumeruKSeF>%s</DataNadaniaNumeruKSeF>
                  <SkrotDokumentu>TUlTTUFUQ0gtSEFTSA==</SkrotDokumentu>
                </Dokument>
              </Potwierdzenie>
              """
                  .formatted(MockKsefDefaults.KSEF_NUMBER, MockKsefDefaults.UPO_ISSUED_AT));

      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(mock.baseUrl())
              .tokenAuth("tkn", NIP)
              .archiveDirectory(archiveDir)
              .build();

      try (SendResult result = client.send(InvoiceFixtures.eurSingleLineVat23())) {
        assertThatThrownBy(result::awaitUpo).isInstanceOf(UpoVerificationException.class);
      }
      // Nothing archived: the per-NIP directory was never created.
      assertThat(archiveDir.resolve(NIP)).doesNotExist();
    }
  }
}
