package io.github.mlkmn.ksef4j.internal.session;

/**
 * Internal: the references produced by one interactive send.
 *
 * @param sessionReferenceNumber the session the invoice was sent in (now closed)
 * @param invoiceReferenceNumber the per-invoice reference for status/UPO polling
 */
public record SendReceipt(String sessionReferenceNumber, String invoiceReferenceNumber) {
}
