package io.github.mlkmn.ksef4j.archive;

/**
 * Identifies an archived send by KSeF reference and issuer NIP.
 *
 * @param ksefNumber the reference KSeF assigned to the accepted invoice
 * @param issuerNip NIP of the seller / context taxpayer
 */
public record ArchiveKey(String ksefNumber, String issuerNip) {}
