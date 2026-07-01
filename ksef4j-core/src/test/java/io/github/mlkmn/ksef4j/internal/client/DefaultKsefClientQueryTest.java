package io.github.mlkmn.ksef4j.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.ResultTruncatedException;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DefaultKsefClientQueryTest {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 1, 31);
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-28T10:15:30Z"), ZoneOffset.UTC);

  private DefaultKsefClient client(FakeQueryTransport transport) {
    return new DefaultKsefClient(
        new FakeAuthSession("access-token"),
        new FakeInteractiveSession(),
        new FakeUpoPoller(null),
        new RecordingArchive(),
        clock,
        Duration.ofSeconds(45),
        null,
        transport);
  }

  @Test
  void query_invoices_returns_mapped_page() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("KSEF-1")), false, false));

    InvoiceMetadataPage page =
        client(transport).queryInvoices(InvoiceQuery.asSeller().issuedBetween(FROM, TO).build());

    assertThat(page.invoices()).extracting(InvoiceMetadata::ksefNumber).containsExactly("KSEF-1");
    assertThat(transport.lastPageOffset).isZero();
    assertThat(transport.lastPageSize).isEqualTo(100);
  }

  @Test
  void stream_invoices_pages_through_all_results() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("A"), entry("B")), true, false));
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("C")), false, false));

    List<String> all;
    try (Stream<InvoiceMetadata> stream =
        client(transport)
            .streamInvoices(InvoiceQuery.asSeller().pageSize(10).issuedBetween(FROM, TO).build())) {
      all = stream.map(InvoiceMetadata::ksefNumber).toList();
    }

    assertThat(all).containsExactly("A", "B", "C");
    assertThat(transport.queryCalls).isEqualTo(2);
  }

  @Test
  void stream_invoices_is_lazy() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("A"), entry("B")), true, false));
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("C")), false, false));

    Stream<InvoiceMetadata> stream =
        client(transport)
            .streamInvoices(InvoiceQuery.asSeller().pageSize(10).issuedBetween(FROM, TO).build());
    String first = stream.map(InvoiceMetadata::ksefNumber).findFirst().orElseThrow();

    assertThat(first).isEqualTo("A");
    assertThat(transport.queryCalls).isEqualTo(1);
  }

  @Test
  void stream_invoices_makes_no_network_call_before_terminal_operation() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("A")), false, false));

    Stream<InvoiceMetadata> stream =
        client(transport).streamInvoices(InvoiceQuery.asSeller().issuedBetween(FROM, TO).build());

    assertThat(transport.queryCalls).isZero();
    assertThat(stream.count()).isEqualTo(1);
    assertThat(transport.queryCalls).isEqualTo(1);
  }

  @Test
  void query_invoices_exposes_truncated_flag_without_throwing() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("A")), false, true));

    InvoiceMetadataPage page =
        client(transport).queryInvoices(InvoiceQuery.asSeller().issuedBetween(FROM, TO).build());

    assertThat(page.truncated()).isTrue();
  }

  @Test
  void stream_invoices_throws_when_result_is_truncated() {
    FakeQueryTransport transport = new FakeQueryTransport();
    transport.pages.add(new Responses.QueryMetadata(List.of(entry("A"), entry("B")), false, true));
    InvoiceQuery query = InvoiceQuery.asSeller().pageSize(10).issuedBetween(FROM, TO).build();

    assertThatThrownBy(() -> client(transport).streamInvoices(query).forEach(x -> {}))
        .isInstanceOf(ResultTruncatedException.class);
  }

  private static Responses.QueryMetadata.Entry entry(String ksef) {
    return new Responses.QueryMetadata.Entry(
        ksef,
        "FV/1",
        "2026-01-10",
        null,
        null,
        null,
        null,
        new BigDecimal("1.00"),
        new BigDecimal("1.00"),
        new BigDecimal("0.00"),
        "PLN",
        "VAT",
        null,
        "h");
  }
}
