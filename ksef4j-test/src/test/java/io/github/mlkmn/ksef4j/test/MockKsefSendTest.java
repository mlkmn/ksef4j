package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;

class MockKsefSendTest {

  @Test
  void real_client_sends_and_receives_a_upo_with_the_echoed_hash() {
    try (MockKsef ksef = MockKsef.create()) {
      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(ksef.baseUrl())
              .tokenAuth("any-token", "5260250274")
              .build();

      Upo upo;
      try (SendResult result = client.send(InvoiceFixtures.singleLineVat23())) {
        upo = result.awaitUpo();
      }

      assertThat(upo).isNotNull();
      assertThat(upo.ksefNumber()).isNotBlank();
      // The UPO's documentHash must equal the SHA-256 the client submitted - proving the echo
      // wiring (awaitUpo() would have thrown UpoVerificationException otherwise).
      assertThat(upo.documentHash()).isNotBlank();
    }
  }
}
