package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.Environment;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentEndpointsTest {

    @Test
    void test_environment_uses_api_test_base() {
        EnvironmentEndpoints e = EnvironmentEndpoints.of(Environment.TEST);
        assertThat(e.challenge()).isEqualTo(URI.create("https://api-test.ksef.mf.gov.pl/v2/auth/challenge"));
    }

    @Test
    void demo_and_prod_bases() {
        assertThat(EnvironmentEndpoints.of(Environment.DEMO).challenge())
                .isEqualTo(URI.create("https://api-demo.ksef.mf.gov.pl/v2/auth/challenge"));
        assertThat(EnvironmentEndpoints.of(Environment.PROD).challenge())
                .isEqualTo(URI.create("https://api.ksef.mf.gov.pl/v2/auth/challenge"));
        assertThat(EnvironmentEndpoints.of(Environment.PROD).tokenRedeem())
                .isEqualTo(URI.create("https://api.ksef.mf.gov.pl/v2/auth/token/redeem"));
    }

    @Test
    void builds_all_paths_from_base() {
        EnvironmentEndpoints e = EnvironmentEndpoints.ofBaseUri(URI.create("http://localhost:8080"));
        assertThat(e.ksefTokenAuth()).isEqualTo(URI.create("http://localhost:8080/auth/ksef-token"));
        assertThat(e.authStatus("REF1")).isEqualTo(URI.create("http://localhost:8080/auth/REF1"));
        assertThat(e.tokenRedeem()).isEqualTo(URI.create("http://localhost:8080/auth/token/redeem"));
        assertThat(e.tokenRefresh()).isEqualTo(URI.create("http://localhost:8080/auth/token/refresh"));
        assertThat(e.openSession()).isEqualTo(URI.create("http://localhost:8080/sessions/online"));
        assertThat(e.sendInvoice("S1")).isEqualTo(URI.create("http://localhost:8080/sessions/online/S1/invoices"));
        assertThat(e.sessionStatus("S1")).isEqualTo(URI.create("http://localhost:8080/sessions/S1"));
        assertThat(e.closeSession("S1")).isEqualTo(URI.create("http://localhost:8080/sessions/online/S1/close"));
    }

    @Test
    void trailing_slash_on_base_is_normalised() {
        EnvironmentEndpoints e = EnvironmentEndpoints.ofBaseUri(URI.create("http://localhost:8080/"));
        assertThat(e.challenge()).isEqualTo(URI.create("http://localhost:8080/auth/challenge"));
    }

    @Test
    void strips_multiple_trailing_slashes() {
        EnvironmentEndpoints e = EnvironmentEndpoints.ofBaseUri(URI.create("http://localhost:8080//"));
        assertThat(e.challenge()).isEqualTo(URI.create("http://localhost:8080/auth/challenge"));
    }
}
