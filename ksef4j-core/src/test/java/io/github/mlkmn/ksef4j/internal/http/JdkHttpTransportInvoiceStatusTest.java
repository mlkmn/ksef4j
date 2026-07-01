package io.github.mlkmn.ksef4j.internal.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JdkHttpTransportInvoiceStatusTest {

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
    void fetch_invoice_status_parses_status_and_sends_bearer() {
        fake.stubJson("/sessions/SESS1/invoices/INV1", 200,
                "{\"status\":{\"code\":200,\"description\":\"Przyjeto\",\"details\":[]},"
                        + "\"referenceNumber\":\"INV1\",\"ordinalNumber\":1}");

        Responses.InvoiceStatus s = transport.fetchInvoiceStatus("SESS1", "INV1", "ACC");

        assertThat(s.status().code()).isEqualTo(200);
        assertThat(s.status().description()).isEqualTo("Przyjeto");
        assertThat(fake.requests.get(0).path()).isEqualTo("/sessions/SESS1/invoices/INV1");
        assertThat(fake.requests.get(0).method()).isEqualTo("GET");
        assertThat(fake.requests.get(0).headers().get("Authorization")).isEqualTo("Bearer ACC");
    }
}
