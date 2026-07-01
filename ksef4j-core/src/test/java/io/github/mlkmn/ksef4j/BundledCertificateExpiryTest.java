package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tripwire: fails while there is still lead time before a bundled certificate expires, so rotation
 * is never silent. The UPO-signing leafs are pinned and static (no dynamic refresh), so this guard
 * is their only automated expiry warning.
 */
class BundledCertificateExpiryTest {

  private static final Duration MIN_REMAINING = Duration.ofDays(180);

  private static final List<String> BUNDLED_CERTS =
      List.of(
          "/keys/test-token.pem",
          "/keys/test-symmetric.pem",
          "/keys/test-upo-signing.pem",
          "/keys/demo-token.pem",
          "/keys/demo-symmetric.pem",
          "/keys/demo-upo-signing.pem",
          "/keys/prod-token.pem",
          "/keys/prod-symmetric.pem",
          "/keys/prod-upo-signing.pem");

  @Test
  void every_bundled_certificate_has_at_least_180_days_before_expiry() throws Exception {
    Instant threshold = Instant.now().plus(MIN_REMAINING);
    CertificateFactory factory = CertificateFactory.getInstance("X.509");

    for (String resource : BUNDLED_CERTS) {
      try (InputStream in = BundledCertificateExpiryTest.class.getResourceAsStream(resource)) {
        assertThat(in).as("bundled cert resource %s must exist", resource).isNotNull();
        X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
        Instant notAfter = cert.getNotAfter().toInstant();
        assertThat(notAfter)
            .as(
                "%s expires %s - within %d days; rotate the bundled cert",
                resource, notAfter, MIN_REMAINING.toDays())
            .isAfter(threshold);
      }
    }
  }
}
