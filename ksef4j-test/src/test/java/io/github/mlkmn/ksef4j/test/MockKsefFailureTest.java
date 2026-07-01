package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;

class MockKsefFailureTest {

  private KsefClient client(MockKsef ksef) {
    return KsefClient.builder()
        .environment(Environment.TEST)
        .baseUrl(ksef.baseUrl())
        .tokenAuth("t", "5260250274")
        .build();
  }

  @Test
  void rejected_send_surfaces_business_exception() {
    try (MockKsef ksef = MockKsef.create()) {
      ksef.onSend().reject(21405, "Blad walidacji danych");
      assertThatThrownBy(() -> client(ksef).send(InvoiceFixtures.singleLineVat23()))
          .isInstanceOf(KsefBusinessException.class);
    }
  }

  @Test
  void failed_auth_surfaces_authentication_exception() {
    try (MockKsef ksef = MockKsef.create()) {
      ksef.onAuth().fail();
      assertThatThrownBy(() -> client(ksef).send(InvoiceFixtures.singleLineVat23()))
          .isInstanceOf(KsefAuthenticationException.class);
    }
  }
}
