package io.github.mlkmn.ksef4j.internal.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import java.net.URI;
import java.time.Duration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpTransportAuthTest {

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
  void fetch_challenge_posts_and_parses() {
    wm.stubFor(
        post(urlEqualTo("/auth/challenge"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"challenge\":\"abc\",\"timestamp\":\"2026-06-28T00:00:00Z\",\"timestampMs\":1717000000000,\"clientIp\":\"1.2.3.4\"}")));

    Responses.Challenge c = transport.fetchChallenge();

    assertThat(c.challenge()).isEqualTo("abc");
    assertThat(c.timestampMs()).isEqualTo(1717000000000L);
    LoggedRequest req = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(req.getMethod().value()).isEqualTo("POST");
    assertThat(req.getUrl()).isEqualTo("/auth/challenge");
  }

  @Test
  void submit_ksef_token_sends_context_and_encrypted_token() {
    wm.stubFor(
        post(urlEqualTo("/auth/ksef-token"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"authenticationToken\":{\"token\":\"jwt\",\"validUntil\":\"2026-06-28T00:10:00Z\"},\"referenceNumber\":\"REF1\"}")));

    Responses.AuthSubmit r = transport.submitKsefTokenAuth("abc", "5260250274", "ENCRYPTED_B64");

    assertThat(r.referenceNumber()).isEqualTo("REF1");
    assertThat(r.authenticationToken().token()).isEqualTo("jwt");
    String body = wm.findAll(anyRequestedFor(anyUrl())).get(0).getBodyAsString();
    assertThat(body).contains("\"challenge\":\"abc\"");
    assertThat(body).contains("\"nip\"").contains("\"5260250274\"");
    assertThat(body).contains("\"encryptedToken\":\"ENCRYPTED_B64\"");
  }

  @Test
  void poll_auth_status_sends_bearer_and_parses_status_code() {
    wm.stubFor(
        get(urlEqualTo("/auth/REF1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}")));

    Responses.AuthStatus s = transport.pollAuthStatus("REF1", "AUTHJWT");

    assertThat(s.status().code()).isEqualTo(200);
    assertThat(wm.findAll(anyRequestedFor(anyUrl())).get(0).getHeader("Authorization"))
        .isEqualTo("Bearer AUTHJWT");
  }

  @Test
  void redeem_tokens_parses_access_and_refresh() {
    wm.stubFor(
        post(urlEqualTo("/auth/token/redeem"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"accessToken\":{\"token\":\"ACC\",\"validUntil\":\"x\"},\"refreshToken\":{\"token\":\"REF\",\"validUntil\":\"y\"}}")));

    Responses.TokenPair p = transport.redeemTokens("AUTHJWT");

    assertThat(p.accessToken().token()).isEqualTo("ACC");
    assertThat(p.refreshToken().token()).isEqualTo("REF");
    assertThat(wm.findAll(anyRequestedFor(anyUrl())).get(0).getHeader("Authorization"))
        .isEqualTo("Bearer AUTHJWT");
  }

  @Test
  void refresh_token_parses_new_access_token() {
    wm.stubFor(
        post(urlEqualTo("/auth/token/refresh"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"accessToken\":{\"token\":\"NEWACC\",\"validUntil\":\"x\"}}")));

    Responses.AccessToken a = transport.refreshToken("REFJWT");

    assertThat(a.accessToken().token()).isEqualTo("NEWACC");
    assertThat(wm.findAll(anyRequestedFor(anyUrl())).get(0).getHeader("Authorization"))
        .isEqualTo("Bearer REFJWT");
  }

  @Test
  void http_401_maps_to_authentication_exception() {
    wm.stubFor(
        post(urlEqualTo("/auth/token/redeem"))
            .willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21301,\"exceptionDescription\":\"Authentication failure\",\"details\":[]}],\"serviceCode\":\"SVC1\",\"timestamp\":\"x\"}}")));

    assertThatThrownBy(() -> transport.redeemTokens("BAD"))
        .isInstanceOf(KsefAuthenticationException.class)
        .hasMessageContaining("Authentication failure");
  }

  @Test
  void http_400_maps_to_business_exception_with_service_code() {
    wm.stubFor(
        post(urlEqualTo("/auth/ksef-token"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21405,\"exceptionDescription\":\"Input validation failure\",\"details\":[]}],\"serviceCode\":\"SVC2\",\"timestamp\":\"x\"}}")));

    assertThatThrownBy(() -> transport.submitKsefTokenAuth("abc", "nip", "tok"))
        .isInstanceOf(KsefBusinessException.class)
        .hasMessageContaining("21405")
        .hasMessageContaining("SVC2")
        .asInstanceOf(InstanceOfAssertFactories.type(KsefBusinessException.class))
        .satisfies(ex -> assertThat(ex.code()).isEqualTo("21405"));
  }

  @Test
  void http_500_maps_to_transport_exception() {
    wm.stubFor(
        post(urlEqualTo("/auth/challenge"))
            .willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "text/html")
                    .withBody("<html>oops</html>")));

    assertThatThrownBy(() -> transport.fetchChallenge()).isInstanceOf(KsefTransportException.class);
  }

  @Test
  void fetch_challenge_sends_no_authorization_header() {
    wm.stubFor(
        post(urlEqualTo("/auth/challenge"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"challenge\":\"abc\",\"timestamp\":\"x\",\"timestampMs\":1,\"clientIp\":\"y\"}")));

    transport.fetchChallenge();

    assertThat(wm.findAll(anyRequestedFor(anyUrl())).get(0).containsHeader("Authorization"))
        .isFalse();
  }
}
