package io.github.mlkmn.ksef4j.internal.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.internal.http.EnvironmentEndpoints;
import io.github.mlkmn.ksef4j.internal.http.JdkHttpTransport;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpCertificateResolverTest {

  // TEST bundled certs are valid 2025-09-29 .. 2027-09-29.
  private static final Instant BEFORE_EXPIRY = Instant.parse("2026-06-29T00:00:00Z");
  private static final Instant NEAR_EXPIRY = Instant.parse("2027-09-29T06:00:00Z");

  private WireMockServer server;
  private JdkHttpTransport transport;
  private MutableClock clock;

  @BeforeEach
  void setUp() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    transport =
        new JdkHttpTransport(
            EnvironmentEndpoints.ofBaseUri(URI.create(server.baseUrl())),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5));
    clock = new MutableClock(BEFORE_EXPIRY);
  }

  @AfterEach
  void tearDown() {
    server.stop();
  }

  @Test
  void bundled_first_does_not_hit_the_network_when_bundled_is_fresh() {
    HttpCertificateResolver resolver =
        new HttpCertificateResolver(transport, clock, Duration.ofMinutes(2));

    PublicKey key = resolver.publicKey(Environment.TEST, KeyUsage.SYMMETRIC_KEY_ENCRYPTION);

    assertThat(key.getAlgorithm()).isEqualTo("RSA");
    assertThat(server.findAll(anyRequestedFor(urlEqualTo("/security/public-key-certificates"))))
        .isEmpty(); // never fetched
  }

  @Test
  void fetches_when_bundled_is_near_expiry() {
    clock.set(NEAR_EXPIRY);
    // Stub the endpoint with two entries so usage-selection is load-bearing:
    // the resolver must pick the SymmetricKeyEncryption entry, not the KsefTokenEncryption one.
    // refreshSkew of 20 min makes the bundled certs (expiring ~06:03 and ~06:17) appear near-expiry
    // at 06:00.
    String tokenBase64 = bundledBase64("/keys/test-token.pem");
    String symmetricBase64 = bundledBase64("/keys/test-symmetric.pem");
    server.stubFor(
        get(urlEqualTo("/security/public-key-certificates"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"certificate\":\""
                            + tokenBase64
                            + "\",\"usage\":[\"KsefTokenEncryption\"],"
                            + "\"validFrom\":\"2025-09-29T00:00:00+00:00\",\"validTo\":\"2027-09-29T23:59:59+00:00\"},"
                            + "{\"certificate\":\""
                            + symmetricBase64
                            + "\",\"usage\":[\"SymmetricKeyEncryption\"],"
                            + "\"validFrom\":\"2025-09-29T00:00:00+00:00\","
                            + "\"validTo\":\"2027-09-29T23:59:59+00:00\"}]")));
    HttpCertificateResolver resolver =
        new HttpCertificateResolver(transport, clock, Duration.ofMinutes(20));

    PublicKey key = resolver.publicKey(Environment.TEST, KeyUsage.SYMMETRIC_KEY_ENCRYPTION);

    List<LoggedRequest> requests =
        server.findAll(anyRequestedFor(urlEqualTo("/security/public-key-certificates")));
    assertThat(requests).hasSize(1); // fetched because bundled was near expiry
    // The returned key must be the symmetric one - proves correct usage selection, not just any RSA
    // key.
    assertThat(key)
        .isEqualTo(
            BundledCertificates.load(Environment.TEST, KeyUsage.SYMMETRIC_KEY_ENCRYPTION)
                .getPublicKey());
  }

  @Test
  void caches_after_first_resolution() {
    HttpCertificateResolver resolver =
        new HttpCertificateResolver(transport, clock, Duration.ofMinutes(2));
    resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);
    resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);
    assertThat(server.findAll(anyRequestedFor(urlEqualTo("/security/public-key-certificates"))))
        .isEmpty(); // bundled-first + cache, no network either time
  }

  @Test
  void falls_back_to_bundled_when_fetch_fails() {
    clock.set(NEAR_EXPIRY);
    server.stubFor(
        get(urlEqualTo("/security/public-key-certificates"))
            .willReturn(aResponse().withStatus(500).withBody("{}")));
    // refreshSkew of 20 min makes the bundled cert (expiring ~06:03) appear near-expiry at 06:00.
    HttpCertificateResolver resolver =
        new HttpCertificateResolver(transport, clock, Duration.ofMinutes(20));

    PublicKey key = resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);

    assertThat(key.getAlgorithm()).isEqualTo("RSA"); // bundled used as last resort
    assertThat(server.findAll(anyRequestedFor(urlEqualTo("/security/public-key-certificates"))))
        .hasSize(1); // it did try to fetch
  }

  private static String bundledBase64(String resource) {
    try (InputStream in = HttpCertificateResolverTest.class.getResourceAsStream(resource)) {
      String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return pem.replace("-----BEGIN CERTIFICATE-----", "")
          .replace("-----END CERTIFICATE-----", "")
          .replaceAll("\\s", "");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void set(Instant t) {
      this.now = t;
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}
