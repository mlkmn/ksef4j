package io.github.mlkmn.ksef4j.test.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import io.github.mlkmn.ksef4j.invoice.Money;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import java.math.BigDecimal;
import java.util.List;

/**
 * Reverse of QueryMetadataMapper: public InvoiceMetadata -> wire query response JSON. Not public
 * API.
 */
public final class QueryWire {

  private QueryWire() {}

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static String toJson(List<InvoiceMetadata> metas, boolean hasMore, boolean truncated) {
    List<Responses.QueryMetadata.Entry> entries = metas.stream().map(QueryWire::toEntry).toList();
    Responses.QueryMetadata wire = new Responses.QueryMetadata(entries, hasMore, truncated);
    try {
      return MAPPER.writeValueAsString(wire);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize mock query response", e);
    }
  }

  private static Responses.QueryMetadata.Entry toEntry(InvoiceMetadata m) {
    Responses.QueryMetadata.Party seller =
        m.seller() == null
            ? null
            : new Responses.QueryMetadata.Party(m.seller().nip(), m.seller().name());
    Responses.QueryMetadata.Buyer buyer =
        m.buyer() == null
            ? null
            : new Responses.QueryMetadata.Buyer(
                new Responses.QueryMetadata.Identifier("Nip", m.buyer().nip()), m.buyer().name());
    Responses.QueryMetadata.FormCode formCode =
        m.schema() == null ? null : new Responses.QueryMetadata.FormCode(m.schema(), "1-0E", "FA");
    String currency = m.grossAmount() == null ? null : m.grossAmount().currency();
    return new Responses.QueryMetadata.Entry(
        m.ksefNumber(),
        m.invoiceNumber(),
        m.issueDate() == null ? null : m.issueDate().toString(),
        m.acquisitionDate() == null ? null : m.acquisitionDate().toString(),
        m.permanentStorageDate() == null ? null : m.permanentStorageDate().toString(),
        seller,
        buyer,
        amount(m.netAmount()),
        amount(m.grossAmount()),
        amount(m.vatAmount()),
        currency,
        m.invoiceType(),
        formCode,
        m.invoiceHash());
  }

  private static BigDecimal amount(Money money) {
    return money == null ? null : money.amount();
  }
}
