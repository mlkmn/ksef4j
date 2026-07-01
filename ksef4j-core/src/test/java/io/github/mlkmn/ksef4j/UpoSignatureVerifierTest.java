package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UpoSignatureVerifierTest {

  @Test
  void verify_with_bundled_prod_cert_reaches_signature_stage() {
    // PROD's signing certificate is bundled, so verification gets past cert loading and fails on
    // the missing signature rather than reporting the certificate as absent.
    byte[] unsigned = "<Potwierdzenie/>".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> new UpoSignatureVerifier().verify(unsigned, Environment.PROD))
        .isInstanceOf(UpoVerificationException.class)
        .hasMessageContaining("no XML signature");
  }
}
