package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.internal.http.FakeKsef;
import io.github.mlkmn.ksef4j.internal.http.KsefHappyPath;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEndToEndTest {

    private FakeKsef fake;
    private KsefClient client;

    @BeforeEach
    void setUp() throws Exception {
        fake = new FakeKsef();
        client = KsefClient.builder()
                .environment(Environment.TEST)
                .baseUrl(fake.baseUri())
                .tokenAuth("test-token", KsefHappyPath.NIP)
                .build();
    }

    @AfterEach
    void tearDown() {
        fake.close();
    }

    @Test
    void streams_metadata_across_pages_over_the_wire() {
        KsefHappyPath.stubQueryPages(fake,
                "{\"invoices\":[{\"ksefNumber\":\"K1\",\"invoiceNumber\":\"FV/1\",\"issueDate\":\"2026-01-05\","
                        + "\"currency\":\"PLN\"},{\"ksefNumber\":\"K2\",\"invoiceNumber\":\"FV/2\","
                        + "\"issueDate\":\"2026-01-06\",\"currency\":\"PLN\"}],\"hasMore\":true}",
                "{\"invoices\":[{\"ksefNumber\":\"K3\",\"invoiceNumber\":\"FV/3\",\"issueDate\":\"2026-01-07\","
                        + "\"currency\":\"PLN\"}],\"hasMore\":false}");

        InvoiceQuery query = InvoiceQuery.asSeller()
                .issuedBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .pageSize(2)
                .build();

        List<String> ksefNumbers;
        try (Stream<InvoiceMetadata> stream = client.streamInvoices(query)) {
            ksefNumbers = stream.map(InvoiceMetadata::ksefNumber).toList();
        }

        assertThat(ksefNumbers).containsExactly("K1", "K2", "K3");
        long queryCalls = fake.requests.stream()
                .filter(r -> r.path().equals("/invoices/query/metadata")).count();
        assertThat(queryCalls).isEqualTo(2);
    }
}
