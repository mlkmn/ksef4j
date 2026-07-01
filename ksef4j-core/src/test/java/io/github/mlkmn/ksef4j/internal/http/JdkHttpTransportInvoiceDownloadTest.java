package io.github.mlkmn.ksef4j.internal.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpTransportInvoiceDownloadTest {

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
  void download_gets_xml_bytes_with_bearer_and_accept_header() {
    String ksef = "7811838663-20260701-0DA443000000-05";
    String xml = "<Faktura>demo</Faktura>";
    wm.stubFor(
        get(urlEqualTo("/invoices/ksef/" + ksef))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(xml)));

    byte[] out = transport.downloadInvoice(ksef, "ACC");

    assertThat(new String(out, StandardCharsets.UTF_8)).isEqualTo(xml);
    LoggedRequest req = wm.findAll(getRequestedFor(urlEqualTo("/invoices/ksef/" + ksef))).get(0);
    assertThat(req.getMethod().value()).isEqualTo("GET");
    assertThat(req.getHeader("Authorization")).isEqualTo("Bearer ACC");
    assertThat(req.getHeader("Accept")).isEqualTo("application/xml");
  }

  @Test
  void not_found_maps_to_business_exception() {
    String ksef = "MISSING";
    wm.stubFor(
        get(urlEqualTo("/invoices/ksef/" + ksef))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21000,"
                            + "\"exceptionDescription\":\"not found\"}]}}")));

    assertThatThrownBy(() -> transport.downloadInvoice(ksef, "ACC"))
        .isInstanceOf(KsefBusinessException.class);
  }
}
