package io.github.mlkmn.ksef4j.internal.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JdkHttpTransportCertificatesTest {

    private FakeKsef fake;
    private JdkHttpTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        fake = new FakeKsef();
        transport = new JdkHttpTransport(
                EnvironmentEndpoints.ofBaseUri(fake.baseUri()), Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        fake.close();
    }

    @Test
    void fetch_certificates_parses_list_and_sends_no_auth_header() {
        fake.stubJson("/security/public-key-certificates", 200,
                "[{\"certificate\":\"AAAA\",\"usage\":[\"KsefTokenEncryption\"],"
                        + "\"validFrom\":\"2025-09-29T06:03:19+00:00\",\"validTo\":\"2027-09-29T06:03:18+00:00\"},"
                        + "{\"certificate\":\"BBBB\",\"usage\":[\"SymmetricKeyEncryption\"],"
                        + "\"validFrom\":\"2025-09-29T06:17:45+00:00\",\"validTo\":\"2027-09-29T06:17:44+00:00\"}]");

        var certs = transport.fetchCertificates();

        assertThat(certs).hasSize(2);
        assertThat(certs.get(0).usage()).containsExactly("KsefTokenEncryption");
        assertThat(certs.get(1).certificate()).isEqualTo("BBBB");
        assertThat(fake.requests.get(0).path()).isEqualTo("/security/public-key-certificates");
        assertThat(fake.requests.get(0).headers()).doesNotContainKey("Authorization");
    }
}
