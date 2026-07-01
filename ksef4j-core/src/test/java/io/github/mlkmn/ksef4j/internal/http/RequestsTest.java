package io.github.mlkmn.ksef4j.internal.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class RequestsTest {

  @Test
  void send_invoice_factory_base64_encodes_crypto_outputs() {
    byte[] ciphertext = "cipher".getBytes(StandardCharsets.UTF_8);
    byte[] ptHash = "ptHash".getBytes(StandardCharsets.UTF_8);
    byte[] ctHash = "ctHash".getBytes(StandardCharsets.UTF_8);
    EncryptedInvoice enc = new EncryptedInvoice(ciphertext, ptHash, 11L, ctHash, 16L);

    Requests.SendInvoice req = Requests.SendInvoice.from(enc);

    Base64.Encoder b64 = Base64.getEncoder();
    assertThat(req.invoiceHash()).isEqualTo(b64.encodeToString(ptHash));
    assertThat(req.invoiceSize()).isEqualTo(11L);
    assertThat(req.encryptedInvoiceHash()).isEqualTo(b64.encodeToString(ctHash));
    assertThat(req.encryptedInvoiceSize()).isEqualTo(16L);
    assertThat(req.encryptedInvoiceContent()).isEqualTo(b64.encodeToString(ciphertext));
    assertThat(req.offlineMode()).isFalse();
  }

  @Test
  void open_session_factory_base64_encodes_key_and_iv() {
    byte[] wrappedKey = "wrapped".getBytes(StandardCharsets.UTF_8);
    byte[] iv = "iv".getBytes(StandardCharsets.UTF_8);
    Requests.FormCode form = new Requests.FormCode("FA (3)", "1-0E", "FA");

    Requests.OpenSession req = Requests.OpenSession.from(wrappedKey, iv, form);

    Base64.Encoder b64 = Base64.getEncoder();
    assertThat(req.encryption().encryptedSymmetricKey()).isEqualTo(b64.encodeToString(wrappedKey));
    assertThat(req.encryption().initializationVector()).isEqualTo(b64.encodeToString(iv));
    assertThat(req.formCode()).isEqualTo(form);
  }
}
