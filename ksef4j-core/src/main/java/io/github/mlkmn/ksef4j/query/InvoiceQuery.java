package io.github.mlkmn.ksef4j.query;

import java.time.LocalDate;

/**
 * An immutable, validated invoice metadata query. Build via {@link #asSeller()} or
 * {@link #asBuyer()}, set a required date range, then {@link Builder#build()}.
 *
 * <p>Invalid queries (missing/inverted/over-long date range, out-of-bounds paging)
 * throw {@link IllegalArgumentException} at build time - they are caller errors,
 * not remote failures.
 */
public final class InvoiceQuery {

    /** Maximum permitted span of the date range, in calendar months (the KSeF limit). */
    public static final int MAX_RANGE_MONTHS = 3;

    private static final int MAX_PAGE_SIZE = 250;
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final SubjectRole role;
    private final DateType dateType;
    private final LocalDate from;
    private final LocalDate to;
    private final String counterpartyNip;
    private final String invoiceNumber;
    private final String ksefNumber;
    private final String currency;
    private final String invoiceType;
    private final int pageOffset;
    private final int pageSize;

    private InvoiceQuery(Builder b) {
        this.role = b.role;
        this.dateType = b.dateType;
        this.from = b.from;
        this.to = b.to;
        this.counterpartyNip = b.counterpartyNip;
        this.invoiceNumber = b.invoiceNumber;
        this.ksefNumber = b.ksefNumber;
        this.currency = b.currency;
        this.invoiceType = b.invoiceType;
        this.pageOffset = b.pageOffset;
        this.pageSize = b.pageSize;
    }

    /** Start a query where the authenticated NIP is the seller (Subject1). */
    public static Builder asSeller() {
        return new Builder(SubjectRole.SELLER);
    }

    /** Start a query where the authenticated NIP is the buyer (Subject2). */
    public static Builder asBuyer() {
        return new Builder(SubjectRole.BUYER);
    }

    /** The subject role (seller or buyer) for this query. */
    public SubjectRole role() { return role; }
    /** The date type being filtered on (issue, invoicing, or permanent storage). */
    public DateType dateType() { return dateType; }
    /** The start of the date range (inclusive). */
    public LocalDate from() { return from; }
    /** The end of the date range (inclusive). */
    public LocalDate to() { return to; }
    /** The counterparty NIP filter, or null if not set. */
    public String counterpartyNip() { return counterpartyNip; }
    /** The invoice number filter, or null if not set. */
    public String invoiceNumber() { return invoiceNumber; }
    /** The KSeF number filter, or null if not set. */
    public String ksefNumber() { return ksefNumber; }
    /** The currency filter, or null if not set. */
    public String currency() { return currency; }
    /** The invoice type filter, or null if not set. */
    public String invoiceType() { return invoiceType; }
    /** The zero-based offset for pagination. */
    public int pageOffset() { return pageOffset; }
    /** The page size for pagination. */
    public int pageSize() { return pageSize; }

    /** Builder for {@link InvoiceQuery}. */
    public static final class Builder {

        private final SubjectRole role;
        private DateType dateType;
        private LocalDate from;
        private LocalDate to;
        private String counterpartyNip;
        private String invoiceNumber;
        private String ksefNumber;
        private String currency;
        private String invoiceType;
        private int pageOffset = 0;
        private int pageSize = DEFAULT_PAGE_SIZE;

        private Builder(SubjectRole role) {
            this.role = role;
        }

        /** Filter by an explicit date type and inclusive range. */
        public Builder dateRange(DateType type, LocalDate from, LocalDate to) {
            this.dateType = type;
            this.from = from;
            this.to = to;
            return this;
        }

        /** Convenience: filter by issue date between {@code from} and {@code to}. */
        public Builder issuedBetween(LocalDate from, LocalDate to) {
            return dateRange(DateType.ISSUE, from, to);
        }

        /** Convenience: filter by the KSeF invoicing (registration) date between {@code from} and {@code to}. */
        public Builder receivedBetween(LocalDate from, LocalDate to) {
            return dateRange(DateType.INVOICING, from, to);
        }

        public Builder counterpartyNip(String nip) { this.counterpartyNip = nip; return this; }
        public Builder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public Builder ksefNumber(String ksefNumber) { this.ksefNumber = ksefNumber; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder invoiceType(String invoiceType) { this.invoiceType = invoiceType; return this; }
        public Builder pageOffset(int pageOffset) { this.pageOffset = pageOffset; return this; }
        public Builder pageSize(int pageSize) { this.pageSize = pageSize; return this; }

        /**
         * Validate and build the query.
         *
         * @throws IllegalArgumentException if the date range is missing, inverted, longer than
         *         {@link #MAX_RANGE_MONTHS}, or paging is out of bounds
         */
        public InvoiceQuery build() {
            if (dateType == null || from == null || to == null) {
                throw new IllegalArgumentException("A date range is required (call issuedBetween/receivedBetween/dateRange)");
            }
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Date range start " + from + " is after end " + to);
            }
            // KSeF caps the queryable range at MAX_RANGE_MONTHS calendar months measured on the
            // datetimes sent. Because `to` is sent as end-of-day (so the last day is inclusive), the
            // maximum inclusive range is 3 months minus one day: a from..from.plusMonths(3) range
            // serializes ~1 day over the cap and the server rejects it (HTTP 400, code 21405).
            // Boundary confirmed against the live KSeF test environment.
            if (to.isAfter(from.plusMonths(MAX_RANGE_MONTHS).minusDays(1))) {
                throw new IllegalArgumentException(
                        "Date range may span at most " + MAX_RANGE_MONTHS + " calendar months minus one day "
                                + "(the end date is inclusive); got " + from + " to " + to);
            }
            if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
            }
            if (pageOffset < 0) {
                throw new IllegalArgumentException("pageOffset must not be negative");
            }
            return new InvoiceQuery(this);
        }
    }
}
