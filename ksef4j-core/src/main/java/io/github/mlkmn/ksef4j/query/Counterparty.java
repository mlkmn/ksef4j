package io.github.mlkmn.ksef4j.query;

/**
 * Lightweight party reference carried in invoice metadata: NIP and name only
 * (the full address lives on the downloaded invoice, not in metadata).
 *
 * @param nip  the party's NIP, or null if the party has no NIP
 * @param name the party's registered name
 */
public record Counterparty(String nip, String name) {
}
