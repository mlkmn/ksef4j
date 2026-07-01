package io.github.mlkmn.ksef4j.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class WireMockSmokeTest {

  @Test
  void wiremock_serves_a_stub_on_a_dynamic_port() throws Exception {
    WireMockServer wm = new WireMockServer(options().dynamicPort());
    wm.start();
    try {
      wm.stubFor(get(urlEqualTo("/ping")).willReturn(aResponse().withStatus(200).withBody("pong")));
      HttpResponse<String> resp =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(URI.create(wm.baseUrl() + "/ping")).build(),
                  HttpResponse.BodyHandlers.ofString());
      assertThat(resp.statusCode()).isEqualTo(200);
      assertThat(resp.body()).isEqualTo("pong");
    } finally {
      wm.stop();
    }
  }
}
