package io.github.mlkmn.ksef4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import io.github.mlkmn.ksef4j.test.MockKsefExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class QueryEndToEndTest {

  @RegisterExtension static final MockKsefExtension mock = MockKsefExtension.create();

  @Test
  void streams_metadata_across_pages_over_the_wire() {
    mock.onQuery()
        .returnsRawPages(
            "{\"invoices\":[{\"ksefNumber\":\"K1\",\"invoiceNumber\":\"FV/1\","
                + "\"issueDate\":\"2026-01-05\",\"currency\":\"PLN\"},"
                + "{\"ksefNumber\":\"K2\",\"invoiceNumber\":\"FV/2\","
                + "\"issueDate\":\"2026-01-06\",\"currency\":\"PLN\"}],\"hasMore\":true}",
            "{\"invoices\":[{\"ksefNumber\":\"K3\",\"invoiceNumber\":\"FV/3\","
                + "\"issueDate\":\"2026-01-07\",\"currency\":\"PLN\"}],\"hasMore\":false}");

    KsefClient client =
        KsefClient.builder()
            .environment(Environment.TEST)
            .baseUrl(mock.baseUrl())
            .tokenAuth("test-token", "5260250274")
            .build();

    InvoiceQuery query =
        InvoiceQuery.asSeller()
            .issuedBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
            .pageSize(2)
            .build();

    List<String> ksefNumbers;
    try (Stream<InvoiceMetadata> stream = client.streamInvoices(query)) {
      ksefNumbers = stream.map(InvoiceMetadata::ksefNumber).toList();
    }

    assertThat(ksefNumbers).containsExactly("K1", "K2", "K3");
    assertThat(mock.requestCount("/invoices/query/metadata")).isEqualTo(2);
  }
}
