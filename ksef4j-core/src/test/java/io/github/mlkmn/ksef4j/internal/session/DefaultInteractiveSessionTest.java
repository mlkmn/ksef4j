package io.github.mlkmn.ksef4j.internal.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import io.github.mlkmn.ksef4j.internal.auth.KeyResolver;
import io.github.mlkmn.ksef4j.internal.auth.KeyUsage;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultInteractiveSessionTest {

  private static final byte[] FA3 = "<Faktura/>".getBytes(StandardCharsets.UTF_8);
  private static final String ACCESS = "ACCESS";

  private FakeInteractiveTransport transport;
  private DefaultInteractiveSession session;
  private KeyUsage[] requestedUsage;

  @BeforeEach
  void setUp() throws Exception {
    transport = new FakeInteractiveTransport();
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    PublicKey publicKey = gen.generateKeyPair().getPublic();
    requestedUsage = new KeyUsage[1];
    KeyResolver keyResolver =
        (env, usage) -> {
          requestedUsage[0] = usage;
          return publicKey;
        };
    session =
        new DefaultInteractiveSession(transport, keyResolver, Environment.TEST, new SecureRandom());
  }

  @Test
  void opens_sends_closes_in_order_and_returns_refs() {
    SendReceipt receipt = session.send(FA3, ACCESS);

    assertThat(receipt.sessionReferenceNumber()).isEqualTo("SESS1");
    assertThat(receipt.invoiceReferenceNumber()).isEqualTo("INV1");
    assertThat(transport.calls).containsExactly("open", "send", "close");
    assertThat(transport.closedSessionRef).isEqualTo("SESS1");
    assertThat(transport.sentSessionRef).isEqualTo("SESS1");
    assertThat(transport.lastOpenRequest.formCode())
        .isEqualTo(new Requests.FormCode("FA (3)", "1-0E", "FA"));
    assertThat(transport.lastOpenRequest.encryption().encryptedSymmetricKey()).isNotBlank();
    assertThat(transport.lastOpenRequest.encryption().initializationVector()).isNotBlank();
    assertThat(transport.lastSendRequest.encryptedInvoiceContent()).isNotBlank();
    assertThat(requestedUsage[0]).isEqualTo(KeyUsage.SYMMETRIC_KEY_ENCRYPTION);
  }

  @Test
  void send_failure_triggers_cleanup_close_and_propagates() {
    transport.sendError = new KsefBusinessException("21405", 400, "rejected");

    assertThatThrownBy(() -> session.send(FA3, ACCESS)).isInstanceOf(KsefBusinessException.class);

    assertThat(transport.calls)
        .containsExactly("open", "send", "close"); // cleanup close after the failed send
    assertThat(transport.closeCount).isEqualTo(1);
  }

  @Test
  void close_failure_propagates() {
    transport.closeError = new KsefTransportException("close failed");

    assertThatThrownBy(() -> session.send(FA3, ACCESS)).isInstanceOf(KsefTransportException.class);
  }

  @Test
  void each_send_uses_a_fresh_session_key() {
    session.send(FA3, ACCESS);
    String firstKey = transport.openRequests.get(0).encryption().encryptedSymmetricKey();

    session.send(FA3, ACCESS);
    String secondKey = transport.openRequests.get(1).encryption().encryptedSymmetricKey();

    assertThat(secondKey).isNotEqualTo(firstKey);
  }

  @Test
  void open_failure_propagates_without_send_or_close() {
    transport.openError = new KsefTransportException("open failed");

    assertThatThrownBy(() -> session.send(FA3, ACCESS)).isInstanceOf(KsefTransportException.class);

    assertThat(transport.calls).containsExactly("open");
    assertThat(transport.closeCount).isZero();
  }
}
