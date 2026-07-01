package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.test.internal.QueryWire;
import java.util.List;

/** Scripts the response to {@code POST /invoices/query/metadata}. */
public final class QueryScenario {

  private static final String QUERY_PAGES_SCENARIO = "query-pages";

  private final WireMockServer server;

  QueryScenario(WireMockServer server) {
    this.server = server;
  }

  /** The next query returns exactly these invoices (hasMore=false, not truncated). */
  public void returns(InvoiceMetadata... invoices) {
    stub(List.of(invoices), false);
  }

  /** The next query returns these invoices flagged truncated (KSeF's 10000-record cap). */
  public void returnsTruncated(InvoiceMetadata... invoices) {
    stub(List.of(invoices), true);
  }

  /**
   * Script a multi-page query sequence: successive POST calls return successive raw JSON bodies.
   * Each body must be a complete query response JSON (with {@code hasMore} set correctly). At least
   * one body is required; the last body is served on every subsequent call.
   *
   * <p>This is the stringly-typed escape hatch for pagination/{@code hasMore} edge cases; for the
   * common truncation case prefer the typed {@link #returnsTruncated(InvoiceMetadata...)}.
   */
  public void returnsRawPages(String firstPage, String... remainingPages) {
    server.stubFor(
        post(urlPathEqualTo("/invoices/query/metadata"))
            .inScenario(QUERY_PAGES_SCENARIO)
            .whenScenarioStateIs(STARTED)
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(firstPage))
            .willSetStateTo(remainingPages.length > 0 ? "page-0" : STARTED));
    for (int i = 0; i < remainingPages.length; i++) {
      String currentState = "page-" + i;
      String nextState = (i + 1 < remainingPages.length) ? "page-" + (i + 1) : "page-" + i;
      server.stubFor(
          post(urlPathEqualTo("/invoices/query/metadata"))
              .inScenario(QUERY_PAGES_SCENARIO)
              .whenScenarioStateIs(currentState)
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody(remainingPages[i]))
              .willSetStateTo(nextState));
    }
  }

  private void stub(List<InvoiceMetadata> invoices, boolean truncated) {
    server.stubFor(
        post(urlPathEqualTo("/invoices/query/metadata"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(QueryWire.toJson(invoices, false, truncated))));
  }
}
