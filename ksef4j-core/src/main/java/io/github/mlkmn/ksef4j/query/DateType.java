package io.github.mlkmn.ksef4j.query;

/** Which invoice date the query's date range filters on. */
public enum DateType {
    /** The invoice issue date (wire: {@code Issue}). */
    ISSUE,
    /** The date the invoice was registered in KSeF (wire: {@code Invoicing}). */
    INVOICING,
    /** The date the invoice entered permanent storage (wire: {@code PermanentStorage}). */
    PERMANENT_STORAGE
}
