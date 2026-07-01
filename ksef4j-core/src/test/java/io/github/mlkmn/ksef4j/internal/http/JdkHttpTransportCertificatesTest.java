package io.github.mlkmn.ksef4j.internal.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpTransportCertificatesTest {

  private WireMockServer wm;
  private JdkHttpTransport transport;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(options().dynamicPort());
    wm.start();
    transport =
        new JdkHttpTransport(
            EnvironmentEndpoints.ofBaseUri(URI.create(wm.baseUrl())),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5));
  }

  @AfterEach
  void tearDown() {
    wm.stop();
  }

  @Test
  void fetch_certificates_parses_list_and_sends_no_auth_header() {
    wm.stubFor(
        get(urlEqualTo("/security/public-key-certificates"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"certificate\":\"AAAA\",\"usage\":[\"KsefTokenEncryption\"],"
                            + "\"validFrom\":\"2025-09-29T06:03:19+00:00\",\"validTo\":\"2027-09-29T06:03:18+00:00\"},"
                            + "{\"certificate\":\"BBBB\",\"usage\":[\"SymmetricKeyEncryption\"],"
                            + "\"validFrom\":\"2025-09-29T06:17:45+00:00\",\"validTo\":\"2027-09-29T06:17:44+00:00\"}]")));

    var certs = transport.fetchCertificates();

    assertThat(certs).hasSize(2);
    assertThat(certs.get(0).usage()).containsExactly("KsefTokenEncryption");
    assertThat(certs.get(1).certificate()).isEqualTo("BBBB");
    LoggedRequest req = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(req.getUrl()).isEqualTo("/security/public-key-certificates");
    assertThat(req.containsHeader("Authorization")).isFalse();
  }
}
