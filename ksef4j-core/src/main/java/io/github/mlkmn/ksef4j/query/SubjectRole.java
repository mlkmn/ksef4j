package io.github.mlkmn.ksef4j.query;

/** Which party the authenticated NIP plays in the queried invoices. */
public enum SubjectRole {
    /** The authenticated NIP is the seller (issuer); maps to KSeF {@code Subject1}. */
    SELLER,
    /** The authenticated NIP is the buyer (recipient); maps to KSeF {@code Subject2}. */
    BUYER
}
