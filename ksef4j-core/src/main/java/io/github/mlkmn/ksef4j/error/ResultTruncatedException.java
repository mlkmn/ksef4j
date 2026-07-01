package io.github.mlkmn.ksef4j.error;

/**
 * A query matched more invoices than KSeF's per-query cap (10000 records), so the result set was
 * truncated. Thrown by {@code KsefClient.streamInvoices(...)} when it reaches a truncated page,
 * rather than silently yielding a partial set; narrow the date range or filters, or page explicitly
 * with {@code KsefClient.queryInvoices(...)} and inspect {@code InvoiceMetadataPage.truncated()}.
 */
public final class ResultTruncatedException extends KsefException {

  private static final long serialVersionUID = 1L;

  public ResultTruncatedException(String message) {
    super(message);
  }
}
