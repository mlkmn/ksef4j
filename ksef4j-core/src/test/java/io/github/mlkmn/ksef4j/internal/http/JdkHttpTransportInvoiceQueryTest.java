package io.github.mlkmn.ksef4j.internal.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpTransportInvoiceQueryTest {

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
  void query_posts_filter_with_paging_and_bearer_then_parses() {
    wm.stubFor(
        post(urlPathEqualTo("/invoices/query/metadata"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"invoices\":[{\"ksefNumber\":\"KSEF-1\",\"invoiceNumber\":\"FV/1\","
                            + "\"issueDate\":\"2026-01-10\",\"currency\":\"PLN\"}],\"hasMore\":true}")));
    Requests.QueryMetadata filter =
        new Requests.QueryMetadata(
            "Subject1",
            new Requests.QueryMetadata.DateRange("Issue", "2026-01-01", "2026-01-31"),
            null,
            null,
            null,
            null,
            null,
            null);

    Responses.QueryMetadata out = transport.queryInvoiceMetadata(filter, 0, 100, "ACC");

    assertThat(out.hasMore()).isTrue();
    assertThat(out.invoices()).hasSize(1);
    assertThat(out.invoices().get(0).ksefNumber()).isEqualTo("KSEF-1");
    LoggedRequest req =
        wm.findAll(postRequestedFor(urlPathEqualTo("/invoices/query/metadata"))).get(0);
    assertThat(req.getMethod().value()).isEqualTo("POST");
    assertThat(req.getUrl()).contains("pageOffset=0").contains("pageSize=100");
    assertThat(req.getHeader("Authorization")).isEqualTo("Bearer ACC");
    assertThat(req.getBodyAsString()).contains("Subject1").contains("Issue");
  }

  @Test
  void business_error_envelope_maps_to_business_exception() {
    wm.stubFor(
        post(urlPathEqualTo("/invoices/query/metadata"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21405,"
                            + "\"exceptionDescription\":\"bad filter\"}],\"serviceCode\":\"svc\"}}")));
    Requests.QueryMetadata filter =
        new Requests.QueryMetadata(
            "Subject1",
            new Requests.QueryMetadata.DateRange("Issue", "2026-01-01", "2026-01-31"),
            null,
            null,
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> transport.queryInvoiceMetadata(filter, 0, 100, "ACC"))
        .isInstanceOf(io.github.mlkmn.ksef4j.error.KsefBusinessException.class);
  }
}
