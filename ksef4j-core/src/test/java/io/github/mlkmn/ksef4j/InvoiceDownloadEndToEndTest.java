package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.test.MockKsefExtension;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class InvoiceDownloadEndToEndTest {

  @RegisterExtension static final MockKsefExtension mock = MockKsefExtension.create();

  @Test
  void downloads_invoice_xml_over_the_wire() {
    KsefClient client =
        KsefClient.builder()
            .environment(Environment.TEST)
            .baseUrl(mock.baseUrl())
            .tokenAuth("test-token", "5260250274")
            .build();

    byte[] xml = client.downloadInvoice("7811838663-20260701-0DA443000000-05");

    assertThat(xml).isNotEmpty();
    assertThat(new String(xml, StandardCharsets.UTF_8)).contains("Faktura");
    assertThat(mock.requestCount("/invoices/ksef/7811838663-20260701-0DA443000000-05"))
        .isEqualTo(1);
  }
}
