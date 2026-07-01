package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkHttpTransportAuthTest {

    private FakeKsef fake;
    private JdkHttpTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        fake = new FakeKsef();
        transport = new JdkHttpTransport(
                EnvironmentEndpoints.ofBaseUri(fake.baseUri()),
                Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        fake.close();
    }

    @Test
    void fetch_challenge_posts_and_parses() {
        fake.stubJson("/auth/challenge", 200,
                "{\"challenge\":\"abc\",\"timestamp\":\"2026-06-28T00:00:00Z\",\"timestampMs\":1717000000000,\"clientIp\":\"1.2.3.4\"}");

        Responses.Challenge c = transport.fetchChallenge();

        assertThat(c.challenge()).isEqualTo("abc");
        assertThat(c.timestampMs()).isEqualTo(1717000000000L);
        FakeKsef.RecordedRequest req = fake.requests.get(0);
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo("/auth/challenge");
    }

    @Test
    void submit_ksef_token_sends_context_and_encrypted_token() {
        fake.stubJson("/auth/ksef-token", 202,
                "{\"authenticationToken\":{\"token\":\"jwt\",\"validUntil\":\"2026-06-28T00:10:00Z\"},\"referenceNumber\":\"REF1\"}");

        Responses.AuthSubmit r = transport.submitKsefTokenAuth("abc", "5260250274", "ENCRYPTED_B64");

        assertThat(r.referenceNumber()).isEqualTo("REF1");
        assertThat(r.authenticationToken().token()).isEqualTo("jwt");
        String body = fake.requests.get(0).body();
        assertThat(body).contains("\"challenge\":\"abc\"");
        assertThat(body).contains("\"nip\"").contains("\"5260250274\"");
        assertThat(body).contains("\"encryptedToken\":\"ENCRYPTED_B64\"");
    }

    @Test
    void poll_auth_status_sends_bearer_and_parses_status_code() {
        fake.stubJson("/auth/REF1", 200, "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}");

        Responses.AuthStatus s = transport.pollAuthStatus("REF1", "AUTHJWT");

        assertThat(s.status().code()).isEqualTo(200);
        assertThat(fake.requests.get(0).headers().get("Authorization")).isEqualTo("Bearer AUTHJWT");
    }

    @Test
    void redeem_tokens_parses_access_and_refresh() {
        fake.stubJson("/auth/token/redeem", 200,
                "{\"accessToken\":{\"token\":\"ACC\",\"validUntil\":\"x\"},\"refreshToken\":{\"token\":\"REF\",\"validUntil\":\"y\"}}");

        Responses.TokenPair p = transport.redeemTokens("AUTHJWT");

        assertThat(p.accessToken().token()).isEqualTo("ACC");
        assertThat(p.refreshToken().token()).isEqualTo("REF");
        assertThat(fake.requests.get(0).headers().get("Authorization")).isEqualTo("Bearer AUTHJWT");
    }

    @Test
    void refresh_token_parses_new_access_token() {
        fake.stubJson("/auth/token/refresh", 200, "{\"accessToken\":{\"token\":\"NEWACC\",\"validUntil\":\"x\"}}");

        Responses.AccessToken a = transport.refreshToken("REFJWT");

        assertThat(a.accessToken().token()).isEqualTo("NEWACC");
        assertThat(fake.requests.get(0).headers().get("Authorization")).isEqualTo("Bearer REFJWT");
    }

    @Test
    void http_401_maps_to_authentication_exception() {
        fake.stubJson("/auth/token/redeem", 401,
                "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21301,\"exceptionDescription\":\"Authentication failure\",\"details\":[]}],\"serviceCode\":\"SVC1\",\"timestamp\":\"x\"}}");

        assertThatThrownBy(() -> transport.redeemTokens("BAD"))
                .isInstanceOf(KsefAuthenticationException.class)
                .hasMessageContaining("Authentication failure");
    }

    @Test
    void http_400_maps_to_business_exception_with_service_code() {
        fake.stubJson("/auth/ksef-token", 400,
                "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21405,\"exceptionDescription\":\"Input validation failure\",\"details\":[]}],\"serviceCode\":\"SVC2\",\"timestamp\":\"x\"}}");

        assertThatThrownBy(() -> transport.submitKsefTokenAuth("abc", "nip", "tok"))
                .isInstanceOf(KsefBusinessException.class)
                .hasMessageContaining("21405")
                .hasMessageContaining("SVC2")
                .asInstanceOf(InstanceOfAssertFactories.type(KsefBusinessException.class))
                .satisfies(ex -> assertThat(ex.code()).isEqualTo("21405"));
    }

    @Test
    void http_500_maps_to_transport_exception() {
        fake.stubJson("/auth/challenge", 500, "<html>oops</html>");

        assertThatThrownBy(() -> transport.fetchChallenge())
                .isInstanceOf(KsefTransportException.class);
    }

    @Test
    void fetch_challenge_sends_no_authorization_header() {
        fake.stubJson("/auth/challenge", 200,
                "{\"challenge\":\"abc\",\"timestamp\":\"x\",\"timestampMs\":1,\"clientIp\":\"y\"}");

        transport.fetchChallenge();

        assertThat(fake.requests.get(0).headers()).doesNotContainKey("Authorization");
    }
}
