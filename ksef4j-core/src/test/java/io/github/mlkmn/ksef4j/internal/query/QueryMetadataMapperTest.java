package io.github.mlkmn.ksef4j.internal.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.KsefTransportException;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryMetadataMapperTest {

  @Test
  void maps_wire_entry_to_public_metadata() {
    Responses.QueryMetadata.Entry entry =
        new Responses.QueryMetadata.Entry(
            "KSEF-1",
            "FV/1",
            "2026-01-10",
            "2026-01-10T09:30:00Z",
            null,
            new Responses.QueryMetadata.Party("1111111111", "Seller Co"),
            new Responses.QueryMetadata.Buyer(
                new Responses.QueryMetadata.Identifier("Nip", "2222222222"), "Buyer Co"),
            new BigDecimal("100.00"),
            new BigDecimal("123.00"),
            new BigDecimal("23.00"),
            "PLN",
            "VAT",
            new Responses.QueryMetadata.FormCode("FA (3)", "1-0E", "FA"),
            "hash-1");
    Responses.QueryMetadata wire = new Responses.QueryMetadata(List.of(entry), true, false);

    InvoiceMetadataPage page = QueryMetadataMapper.toPage(wire, 0, 100);

    assertThat(page.hasMore()).isTrue();
    assertThat(page.truncated()).isFalse();
    assertThat(page.pageOffset()).isZero();
    assertThat(page.pageSize()).isEqualTo(100);
    assertThat(page.invoices()).hasSize(1);
    var m = page.invoices().get(0);
    assertThat(m.ksefNumber()).isEqualTo("KSEF-1");
    assertThat(m.issueDate()).isEqualTo(LocalDate.of(2026, 1, 10));
    assertThat(m.acquisitionDate()).isEqualTo(Instant.parse("2026-01-10T09:30:00Z"));
    assertThat(m.permanentStorageDate()).isNull();
    assertThat(m.seller().nip()).isEqualTo("1111111111");
    assertThat(m.buyer().nip()).isEqualTo("2222222222");
    assertThat(m.buyer().name()).isEqualTo("Buyer Co");
    assertThat(m.grossAmount().amount()).isEqualByComparingTo("123.00");
    assertThat(m.grossAmount().currency()).isEqualTo("PLN");
    assertThat(m.invoiceType()).isEqualTo("VAT");
    assertThat(m.schema()).isEqualTo("FA (3)");
  }

  @Test
  void empty_page_maps_to_empty_list() {
    Responses.QueryMetadata wire = new Responses.QueryMetadata(List.of(), false, false);

    InvoiceMetadataPage page = QueryMetadataMapper.toPage(wire, 200, 50);

    assertThat(page.invoices()).isEmpty();
    assertThat(page.hasMore()).isFalse();
    assertThat(page.pageOffset()).isEqualTo(200);
  }

  @Test
  void maps_truncated_flag() {
    Responses.QueryMetadata wire = new Responses.QueryMetadata(List.of(), false, true);

    InvoiceMetadataPage page = QueryMetadataMapper.toPage(wire, 0, 100);

    assertThat(page.truncated()).isTrue();
  }

  @Test
  void unparseable_date_becomes_transport_exception() {
    Responses.QueryMetadata.Entry bad =
        new Responses.QueryMetadata.Entry(
            "KSEF-1",
            "FV/1",
            "not-a-date",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "PLN",
            "VAT",
            null,
            "hash-1");
    Responses.QueryMetadata wire = new Responses.QueryMetadata(List.of(bad), false, false);

    assertThatThrownBy(() -> QueryMetadataMapper.toPage(wire, 0, 100))
        .isInstanceOf(KsefTransportException.class);
  }
}
