package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpoSignatureVerifierTest {

    @Test
    void verify_throws_when_environment_certificate_not_bundled() {
        byte[] anyXml = "<Potwierdzenie/>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new UpoSignatureVerifier().verify(anyXml, Environment.DEMO))
                .isInstanceOf(UpoVerificationException.class)
                .hasMessageContaining("not bundled");
    }
}
