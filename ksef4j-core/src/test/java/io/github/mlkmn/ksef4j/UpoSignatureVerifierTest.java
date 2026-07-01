package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UpoSignatureVerifierTest {

  @Test
  void verify_throws_when_environment_certificate_not_bundled() {
    byte[] anyXml = "<Potwierdzenie/>".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> new UpoSignatureVerifier().verify(anyXml, Environment.PROD))
        .isInstanceOf(UpoVerificationException.class)
        .hasMessageContaining("not bundled");
  }
}
