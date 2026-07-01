package io.github.mlkmn.ksef4j.archive;

/**
 * Identifies an archived send by KSeF reference and issuer NIP.
 *
 * @param ksefReferenceNumber the reference KSeF assigned to the accepted invoice
 * @param issuerNip           NIP of the seller / context taxpayer
 */
public record ArchiveKey(String ksefReferenceNumber, String issuerNip) {
}
