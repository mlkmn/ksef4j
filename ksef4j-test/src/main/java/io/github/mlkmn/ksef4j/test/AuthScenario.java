package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mlkmn.ksef4j.test.internal.KsefPayloads;

/** Scripts the authentication handshake. */
public final class AuthScenario {
  private final WireMockServer server;

  AuthScenario(WireMockServer server) {
    this.server = server;
  }

  /** The auth handshake fails (the token-submit step returns HTTP 400). */
  public void fail() {
    server.stubFor(
        post(urlEqualTo("/auth/ksef-token"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(KsefPayloads.errorEnvelope(21301, "Authentication failure"))));
  }
}
