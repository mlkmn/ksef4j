package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;

/** Scripts the UPO document the mock serves. */
public final class UpoScenario {
  private final WireMockServer server;

  UpoScenario(WireMockServer server) {
    this.server = server;
  }

  /**
   * The next UPO download returns exactly {@code xml} (bypasses the hash-echo transformer). Useful
   * for testing UPO integrity-check failures.
   */
  public void returnsXml(String xml) {
    server.stubFor(
        get(urlEqualTo("/upo/" + MockKsefDefaults.UPO_PAGE_REF))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(xml)));
  }
}
