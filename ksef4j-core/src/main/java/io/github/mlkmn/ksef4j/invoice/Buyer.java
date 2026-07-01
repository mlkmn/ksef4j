package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Recipient of the invoice. Maps to FA(3) {@code Podmiot2}.
 *
 * @param nip     10-digit NIP (no separators)
 * @param name    registered company name
 * @param address postal address (FA(3) {@code Adres}); required
 */
public record Buyer(
        @JsonProperty(required = true) String nip,
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) Address address) {
}
