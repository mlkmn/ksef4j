package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.error.ResultTruncatedException;
import io.github.mlkmn.ksef4j.invoice.Money;
import io.github.mlkmn.ksef4j.query.Counterparty;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MockKsefQueryTest {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 1, 31);

  private static InvoiceMetadata meta(String ksef) {
    return new InvoiceMetadata(
        ksef,
        "FV/1",
        LocalDate.of(2026, 1, 10),
        null,
        null,
        new Counterparty("1111111111", "Seller"),
        new Counterparty("2222222222", "Buyer"),
        new Money(new BigDecimal("123.00"), "PLN"),
        new Money(new BigDecimal("100.00"), "PLN"),
        new Money(new BigDecimal("23.00"), "PLN"),
        "Vat",
        "FA (3)",
        "hash");
  }

  @Test
  void query_returns_scripted_invoices() {
    try (MockKsef ksef = MockKsef.create()) {
      ksef.onQuery().returns(meta("K1"), meta("K2"));
      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(ksef.baseUrl())
              .tokenAuth("t", "5260250274")
              .build();

      InvoiceMetadataPage page =
          client.queryInvoices(InvoiceQuery.asSeller().issuedBetween(FROM, TO).build());

      assertThat(page.invoices())
          .extracting(InvoiceMetadata::ksefNumber)
          .containsExactly("K1", "K2");
      assertThat(page.truncated()).isFalse();
    }
  }

  @Test
  void truncated_query_makes_stream_throw() {
    try (MockKsef ksef = MockKsef.create()) {
      ksef.onQuery().returnsTruncated(meta("K1"));
      KsefClient client =
          KsefClient.builder()
              .environment(Environment.TEST)
              .baseUrl(ksef.baseUrl())
              .tokenAuth("t", "5260250274")
              .build();

      assertThatThrownBy(
              () ->
                  client
                      .streamInvoices(InvoiceQuery.asSeller().issuedBetween(FROM, TO).build())
                      .forEach(x -> {}))
          .isInstanceOf(ResultTruncatedException.class);
    }
  }
}
