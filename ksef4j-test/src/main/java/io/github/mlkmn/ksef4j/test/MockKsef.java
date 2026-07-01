package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.mlkmn.ksef4j.test.internal.KsefPayloads;
import io.github.mlkmn.ksef4j.test.internal.QueryWire;
import io.github.mlkmn.ksef4j.test.internal.UpoEchoTransformer;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-process mock KSeF server for testing a {@code KsefClient} integration offline. Pre-wires the
 * full happy path (auth, send, UPO, query); override with {@code onSend()}, {@code onQuery()},
 * {@code onAuth()}. {@code AutoCloseable}; for JUnit 5 use {@code MockKsefExtension}.
 */
public final class MockKsef implements AutoCloseable {

  private final WireMockServer server;

  private MockKsef(WireMockServer server) {
    this.server = server;
  }

  /** Start a mock with the default happy path wired. */
  public static MockKsef create() {
    WireMockServer server =
        new WireMockServer(
            options()
                .dynamicPort()
                .extensions(services -> List.of(new UpoEchoTransformer(services.getAdmin()))));
    server.start();
    MockKsef mock = new MockKsef(server);
    mock.registerDefaults();
    return mock;
  }

  /** Base URL to pass to {@code KsefClient.builder().baseUrl(...)}. */
  public URI baseUrl() {
    return URI.create(server.baseUrl());
  }

  private void registerDefaults() {
    server.stubFor(post(urlEqualTo("/auth/challenge")).willReturn(okJson(KsefPayloads.CHALLENGE)));
    server.stubFor(
        post(urlEqualTo("/auth/ksef-token")).willReturn(jsonStatus(202, KsefPayloads.KSEF_TOKEN)));
    server.stubFor(get(urlEqualTo("/auth/REF1")).willReturn(okJson(KsefPayloads.AUTH_STATUS_OK)));
    server.stubFor(
        post(urlEqualTo("/auth/token/redeem")).willReturn(okJson(KsefPayloads.TOKEN_REDEEM)));

    // Send flow defaults.
    String sess = MockKsefDefaults.SESSION_REF;
    server.stubFor(
        post(urlEqualTo("/sessions/online")).willReturn(okJson(KsefPayloads.OPEN_SESSION)));
    server.stubFor(
        post(urlEqualTo("/sessions/online/" + sess + "/invoices"))
            .willReturn(jsonStatus(202, KsefPayloads.SEND_INVOICE_ACCEPTED)));
    server.stubFor(
        post(urlEqualTo("/sessions/online/" + sess + "/close"))
            .willReturn(aResponse().withStatus(204)));

    // Per-invoice status: immediately accepted (phase 1 of the UPO poller).
    server.stubFor(
        get(urlEqualTo("/sessions/" + sess + "/invoices/" + MockKsefDefaults.INVOICE_REF))
            .willReturn(okJson(KsefPayloads.invoiceAccepted())));

    // Session status flips in-progress -> UPO-ready across two poll calls (phase 2).
    String upoUrl = server.baseUrl() + "/upo/" + MockKsefDefaults.UPO_PAGE_REF;
    server.stubFor(
        get(urlEqualTo("/sessions/" + sess))
            .inScenario("upo")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(okJson(KsefPayloads.sessionInProgress()))
            .willSetStateTo("upo-ready"));
    server.stubFor(
        get(urlEqualTo("/sessions/" + sess))
            .inScenario("upo")
            .whenScenarioStateIs("upo-ready")
            .willReturn(okJson(KsefPayloads.sessionUpoReady(upoUrl))));

    // UPO download: the echo transformer fills SkrotDokumentu from the submitted invoiceHash.
    server.stubFor(
        get(urlEqualTo("/upo/" + MockKsefDefaults.UPO_PAGE_REF))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody("PLACEHOLDER")
                    .withTransformers(UpoEchoTransformer.NAME)));

    // Default query stub: empty page (unscripted queries return empty, not 404).
    server.stubFor(
        post(urlPathEqualTo("/invoices/query/metadata"))
            .willReturn(okJson(QueryWire.toJson(List.of(), false, false))));

    // Invoice download: any KSeF number returns a canned FA(3) XML body.
    server.stubFor(
        get(urlPathMatching("/invoices/ksef/.+"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(KsefPayloads.DOWNLOADED_INVOICE_XML)));
  }

  /** Returns a {@link QueryScenario} for scripting the next query response. */
  public QueryScenario onQuery() {
    return new QueryScenario(server);
  }

  /** Returns a {@link UpoScenario} for scripting the UPO document the mock serves. */
  public UpoScenario onUpo() {
    return new UpoScenario(server);
  }

  /** Returns a {@link SendScenario} for scripting the next invoice-send response. */
  public SendScenario onSend() {
    return new SendScenario(server);
  }

  /** Returns an {@link AuthScenario} for scripting the authentication handshake. */
  public AuthScenario onAuth() {
    return new AuthScenario(server);
  }

  /**
   * Reset stubs, journal, and scenarios to the default happy path (used by the JUnit extension).
   */
  void reset() {
    server.resetAll();
    registerDefaults();
  }

  /** The plaintext invoiceHash values the client submitted, in order. */
  public List<String> sentInvoiceHashes() {
    ObjectMapper mapper = new ObjectMapper();
    return server
        .findAll(
            postRequestedFor(
                urlEqualTo("/sessions/online/" + MockKsefDefaults.SESSION_REF + "/invoices")))
        .stream()
        .map(
            r -> {
              try {
                return mapper.readTree(r.getBodyAsString()).path("invoiceHash").asText("");
              } catch (Exception e) {
                throw new IllegalStateException(
                    "Mock received a send request whose body is not valid JSON: "
                        + r.getBodyAsString(),
                    e);
              }
            })
        .toList();
  }

  /** Number of requests received for the given path (path-only match). */
  public int requestCount(String path) {
    return server.findAll(anyRequestedFor(urlPathEqualTo(path))).size();
  }

  /**
   * All request URLs (path plus any query string) received by the mock, in chronological order.
   * Useful for {@code containsSubsequence} assertions on the full request flow.
   */
  public List<String> requestedUrls() {
    List<ServeEvent> events = server.getAllServeEvents();
    // getAllServeEvents() returns newest-first; reverse to get chronological order.
    return events.reversed().stream().map(ServeEvent::getRequest).map(r -> r.getUrl()).toList();
  }

  /**
   * Headers of the first request the mock received for {@code path} (path-only match, consistent
   * with requestCount). Returns an empty map if no such request was recorded.
   */
  public Map<String, String> firstRequestHeaders(String path) {
    return server.findAll(anyRequestedFor(urlPathEqualTo(path))).stream()
        .findFirst()
        .map(
            r -> {
              Map<String, String> headers = new LinkedHashMap<>();
              r.getHeaders().all().forEach(h -> headers.put(h.key(), String.join(",", h.values())));
              return headers;
            })
        .orElse(Map.of());
  }

  private static ResponseDefinitionBuilder okJson(String body) {
    return jsonStatus(200, body);
  }

  private static ResponseDefinitionBuilder jsonStatus(int status, String body) {
    return aResponse()
        .withStatus(status)
        .withHeader("Content-Type", "application/json")
        .withBody(body);
  }

  @Override
  public void close() {
    server.stop();
  }
}
