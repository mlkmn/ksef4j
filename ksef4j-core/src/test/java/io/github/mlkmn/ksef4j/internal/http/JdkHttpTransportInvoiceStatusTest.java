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

class JdkHttpTransportInvoiceStatusTest {

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
  void fetch_invoice_status_parses_status_and_sends_bearer() {
    wm.stubFor(
        get(urlEqualTo("/sessions/SESS1/invoices/INV1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"status\":{\"code\":200,\"description\":\"Przyjeto\",\"details\":[]},"
                            + "\"referenceNumber\":\"INV1\",\"ordinalNumber\":1}")));

    Responses.InvoiceStatus s = transport.fetchInvoiceStatus("SESS1", "INV1", "ACC");

    assertThat(s.status().code()).isEqualTo(200);
    assertThat(s.status().description()).isEqualTo("Przyjeto");
    LoggedRequest req = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(req.getUrl()).isEqualTo("/sessions/SESS1/invoices/INV1");
    assertThat(req.getMethod().value()).isEqualTo("GET");
    assertThat(req.getHeader("Authorization")).isEqualTo("Bearer ACC");
  }
}
