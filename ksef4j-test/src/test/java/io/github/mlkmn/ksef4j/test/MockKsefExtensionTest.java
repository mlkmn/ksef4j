package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MockKsefExtensionTest {

  @RegisterExtension static final MockKsefExtension ksef = MockKsefExtension.create();

  @Test
  void send_then_verify_recorded_request() {
    KsefClient client =
        KsefClient.builder()
            .environment(Environment.TEST)
            .baseUrl(ksef.baseUrl())
            .tokenAuth("t", "5260250274")
            .build();
    try (var result = client.send(InvoiceFixtures.singleLineVat23())) {
      result.awaitUpo();
    }
    assertThat(ksef.sentInvoiceHashes()).hasSize(1);
    assertThat(ksef.requestCount("/sessions/online")).isEqualTo(1);
  }
}
