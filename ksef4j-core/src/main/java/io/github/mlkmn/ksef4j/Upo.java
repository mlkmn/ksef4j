package io.github.mlkmn.ksef4j;

import java.time.Instant;

/**
 * Receipt of acceptance (Urzedowe Potwierdzenie Odbioru) for a sent invoice.
 *
 * @param ksefReferenceNumber KSeF-assigned reference for the accepted invoice
 * @param upoReferenceNumber KSeF-assigned reference for the UPO document itself
 * @param issuedAt instant the UPO was issued (server time, UTC)
 * @param documentHash base64 SHA-256 of the FA(3) (UPO SkrotDokumentu); null if the UPO omits it
 * @param invoiceNumber the invoice number the UPO confirms (UPO NumerFaktury); null if absent
 * @param xml raw UPO XML returned by KSeF
 */
public record Upo(
    String ksefReferenceNumber,
    String upoReferenceNumber,
    Instant issuedAt,
    String documentHash,
    String invoiceNumber,
    byte[] xml) {}
