package io.github.mlkmn.ksef4j.internal.query;

import io.github.mlkmn.ksef4j.error.KsefTransportException;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import io.github.mlkmn.ksef4j.invoice.Money;
import io.github.mlkmn.ksef4j.query.Counterparty;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Internal: converts query-metadata wire DTOs into public read types. Not supported API. */
public final class QueryMetadataMapper {

  private QueryMetadataMapper() {}

  public static InvoiceMetadataPage toPage(
      Responses.QueryMetadata wire, int pageOffset, int pageSize) {
    List<InvoiceMetadata> invoices =
        wire.invoices().stream().map(QueryMetadataMapper::toMetadata).toList();
    return new InvoiceMetadataPage(
        invoices, wire.hasMore(), wire.isTruncated(), pageOffset, pageSize);
  }

  private static InvoiceMetadata toMetadata(Responses.QueryMetadata.Entry e) {
    try {
      return new InvoiceMetadata(
          e.ksefNumber(),
          e.invoiceNumber(),
          parseDate(e.issueDate()),
          parseInstant(e.acquisitionDate()),
          parseInstant(e.permanentStorageDate()),
          seller(e.seller()),
          buyer(e.buyer()),
          money(e.grossAmount(), e.currency()),
          money(e.netAmount(), e.currency()),
          money(e.vatAmount(), e.currency()),
          e.invoiceType(),
          e.formCode() == null ? null : e.formCode().systemCode(),
          e.invoiceHash());
    } catch (DateTimeParseException ex) {
      throw new KsefTransportException("Could not parse a date in the KSeF query response", ex);
    }
  }

  private static Counterparty seller(Responses.QueryMetadata.Party p) {
    return p == null ? null : new Counterparty(p.nip(), p.name());
  }

  private static Counterparty buyer(Responses.QueryMetadata.Buyer b) {
    if (b == null) {
      return null;
    }
    String nip =
        (b.identifier() != null && "Nip".equals(b.identifier().type()))
            ? b.identifier().value()
            : null;
    return new Counterparty(nip, b.name());
  }

  private static Money money(BigDecimal amount, String currency) {
    return amount == null ? null : new Money(amount, currency);
  }

  private static LocalDate parseDate(String value) {
    return value == null ? null : LocalDate.parse(value);
  }

  private static Instant parseInstant(String value) {
    return value == null ? null : Instant.parse(value);
  }
}
