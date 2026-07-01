package io.github.mlkmn.ksef4j.query;

import java.util.List;

/**
 * One page of invoice metadata, mirroring KSeF's offset-based pagination.
 *
 * @param invoices   the invoices on this page (possibly empty)
 * @param hasMore    true if more pages follow this one
 * @param truncated  true if the full result set hit KSeF's 10000-record per-query cap (there are
 *                   more matches than can be retrieved; narrow the date range or filters)
 * @param pageOffset the offset this page was requested at
 * @param pageSize   the page size this page was requested with
 */
public record InvoiceMetadataPage(
        List<InvoiceMetadata> invoices,
        boolean hasMore,
        boolean truncated,
        int pageOffset,
        int pageSize) {

    public InvoiceMetadataPage {
        invoices = List.copyOf(invoices);
    }

    /** The offset to request the next page at (this page's offset plus its item count). */
    public int nextOffset() {
        return pageOffset + invoices.size();
    }
}
