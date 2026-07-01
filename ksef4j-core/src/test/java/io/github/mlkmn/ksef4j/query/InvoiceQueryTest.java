package io.github.mlkmn.ksef4j.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceQueryTest {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 1, 31);

  @Test
  void builds_seller_query_with_defaults() {
    InvoiceQuery q = InvoiceQuery.asSeller().issuedBetween(FROM, TO).build();

    assertThat(q.role()).isEqualTo(SubjectRole.SELLER);
    assertThat(q.dateType()).isEqualTo(DateType.ISSUE);
    assertThat(q.from()).isEqualTo(FROM);
    assertThat(q.to()).isEqualTo(TO);
    assertThat(q.pageOffset()).isZero();
    assertThat(q.pageSize()).isEqualTo(100);
    assertThat(q.counterpartyNip()).isNull();
  }

  @Test
  void buyer_and_optional_filters_are_carried() {
    InvoiceQuery q =
        InvoiceQuery.asBuyer()
            .receivedBetween(FROM, TO)
            .counterpartyNip("1234567890")
            .pageSize(50)
            .pageOffset(50)
            .build();

    assertThat(q.role()).isEqualTo(SubjectRole.BUYER);
    assertThat(q.dateType()).isEqualTo(DateType.INVOICING);
    assertThat(q.counterpartyNip()).isEqualTo("1234567890");
    assertThat(q.pageSize()).isEqualTo(50);
    assertThat(q.pageOffset()).isEqualTo(50);
  }

  @Test
  void missing_date_range_is_rejected() {
    assertThatThrownBy(() -> InvoiceQuery.asSeller().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("date range");
  }

  @Test
  void inverted_range_is_rejected() {
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(TO, FROM).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void range_exceeding_max_span_is_rejected() {
    LocalDate tooFar = FROM.plusMonths(InvoiceQuery.MAX_RANGE_MONTHS).plusDays(1);
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(FROM, tooFar).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void range_of_exactly_three_months_is_rejected() {
    // The end date is inclusive (wire sends end-of-day), so an exactly-3-month range is one day
    // over KSeF's cap; the live server rejects it, so build() must too.
    LocalDate threeMonths = FROM.plusMonths(InvoiceQuery.MAX_RANGE_MONTHS);
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(FROM, threeMonths).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void range_of_three_months_minus_one_day_is_accepted() {
    LocalDate maxTo = FROM.plusMonths(InvoiceQuery.MAX_RANGE_MONTHS).minusDays(1);
    InvoiceQuery query = InvoiceQuery.asSeller().issuedBetween(FROM, maxTo).build();
    assertThat(query.to()).isEqualTo(maxTo);
  }

  @Test
  void page_size_out_of_bounds_is_rejected() {
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(FROM, TO).pageSize(0).build())
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(FROM, TO).pageSize(251).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negative_page_offset_is_rejected() {
    assertThatThrownBy(() -> InvoiceQuery.asSeller().issuedBetween(FROM, TO).pageOffset(-1).build())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
