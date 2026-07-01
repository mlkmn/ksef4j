package io.github.mlkmn.ksef4j.internal.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

class UpoSigningCertificatesTest {

  @Test
  void loads_bundled_test_signing_certificate() {
    X509Certificate cert = UpoSigningCertificates.load(Environment.TEST);

    assertThat(cert).isNotNull();
    assertThat(cert.getSubjectX500Principal().getName()).contains("Ministerstwo");
    // The bundled pin must currently be within its validity window.
    Date now = Date.from(Instant.now());
    assertThat(cert.getNotBefore()).isBeforeOrEqualTo(now);
    assertThat(cert.getNotAfter()).isAfterOrEqualTo(now);
  }

  @Test
  void loads_bundled_demo_signing_certificate() {
    X509Certificate cert = UpoSigningCertificates.load(Environment.DEMO);

    assertThat(cert).isNotNull();
    assertThat(cert.getSubjectX500Principal().getName()).contains("Ministerstwo");
    // The bundled pin must currently be within its validity window.
    Date now = Date.from(Instant.now());
    assertThat(cert.getNotBefore()).isBeforeOrEqualTo(now);
    assertThat(cert.getNotAfter()).isAfterOrEqualTo(now);
  }

  @Test
  void prod_signing_certificate_is_not_bundled() {
    assertThatThrownBy(() -> UpoSigningCertificates.load(Environment.PROD))
        .isInstanceOf(UpoVerificationException.class)
        .hasMessageContaining("not bundled")
        .hasMessageContaining("PROD");
  }
}
