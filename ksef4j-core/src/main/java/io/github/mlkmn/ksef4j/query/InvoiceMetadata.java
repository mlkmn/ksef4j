package io.github.mlkmn.ksef4j.query;

import io.github.mlkmn.ksef4j.invoice.Money;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Header-level facts about one invoice, as returned by a metadata query. Does not
 * include line items or the full FA(3) body; download the invoice by KSeF number for those.
 *
 * @param ksefNumber           the KSeF-assigned identifier
 * @param invoiceNumber        the issuer's own invoice number
 * @param issueDate            the invoice issue date
 * @param acquisitionDate      when KSeF received the invoice; null if not yet acquired
 * @param permanentStorageDate when the invoice entered permanent storage; null until then
 * @param seller               the seller (Subject1)
 * @param buyer                the buyer (Subject2)
 * @param grossAmount          the gross total
 * @param netAmount            the net total
 * @param vatAmount            the VAT total
 * @param invoiceType          KSeF invoice type code (kept as String for forward-compatibility)
 * @param schema               the form/schema code, e.g. "FA (3)"
 * @param invoiceHash          the invoice hash reported by KSeF
 */
public record InvoiceMetadata(
        String ksefNumber,
        String invoiceNumber,
        LocalDate issueDate,
        Instant acquisitionDate,
        Instant permanentStorageDate,
        Counterparty seller,
        Counterparty buyer,
        Money grossAmount,
        Money netAmount,
        Money vatAmount,
        String invoiceType,
        String schema,
        String invoiceHash) {
}
