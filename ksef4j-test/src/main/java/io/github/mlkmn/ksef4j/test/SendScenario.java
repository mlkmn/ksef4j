package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mlkmn.ksef4j.test.internal.KsefPayloads;

/** Scripts the response to sending an invoice. */
public final class SendScenario {
  private final WireMockServer server;

  SendScenario(WireMockServer server) {
    this.server = server;
  }

  /** The next send is rejected with a KSeF business-error envelope (HTTP 400, given code). */
  public void reject(int code, String message) {
    server.stubFor(
        post(urlEqualTo("/sessions/online/" + KsefPayloads.SESSION_REF + "/invoices"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(KsefPayloads.errorEnvelope(code, message))));
  }
}
