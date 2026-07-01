package io.github.mlkmn.ksef4j.internal.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkHttpTransportInvoiceQueryTest {

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
    void query_posts_filter_with_paging_and_bearer_then_parses() {
        fake.stubJson("/invoices/query/metadata", 200,
                "{\"invoices\":[{\"ksefNumber\":\"KSEF-1\",\"invoiceNumber\":\"FV/1\","
                        + "\"issueDate\":\"2026-01-10\",\"currency\":\"PLN\"}],\"hasMore\":true}");
        Requests.QueryMetadata filter = new Requests.QueryMetadata(
                "Subject1", new Requests.QueryMetadata.DateRange("Issue", "2026-01-01", "2026-01-31"),
                null, null, null, null, null, null);

        Responses.QueryMetadata out = transport.queryInvoiceMetadata(filter, 0, 100, "ACC");

        assertThat(out.hasMore()).isTrue();
        assertThat(out.invoices()).hasSize(1);
        assertThat(out.invoices().get(0).ksefNumber()).isEqualTo("KSEF-1");
        FakeKsef.RecordedRequest req = fake.requests.get(0);
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo("/invoices/query/metadata");
        assertThat(req.query()).contains("pageOffset=0").contains("pageSize=100");
        assertThat(req.headers().get("Authorization")).isEqualTo("Bearer ACC");
        assertThat(req.body()).contains("Subject1").contains("Issue");
    }

    @Test
    void business_error_envelope_maps_to_business_exception() {
        fake.stubJson("/invoices/query/metadata", 400,
                "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":21405,"
                        + "\"exceptionDescription\":\"bad filter\"}],\"serviceCode\":\"svc\"}}");
        Requests.QueryMetadata filter = new Requests.QueryMetadata(
                "Subject1", new Requests.QueryMetadata.DateRange("Issue", "2026-01-01", "2026-01-31"),
                null, null, null, null, null, null);

        assertThatThrownBy(() -> transport.queryInvoiceMetadata(filter, 0, 100, "ACC"))
                .isInstanceOf(io.github.mlkmn.ksef4j.error.KsefBusinessException.class);
    }
}
