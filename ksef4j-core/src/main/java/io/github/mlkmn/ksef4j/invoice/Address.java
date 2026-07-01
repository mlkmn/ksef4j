package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Postal address attached to a party (seller or buyer). Maps to FA(3) {@code TAdres}.
 *
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "PL"). Required.
 * @param line1       address line 1 (street, number; or full address up to 512 chars). Required.
 * @param line2       address line 2 (apartment, district). Optional; may be {@code null}.
 * @param gln         Global Location Number. Optional; may be {@code null}.
 */
public record Address(
        @JsonProperty(required = true) String countryCode,
        @JsonProperty(required = true) String line1,
        String line2,
        String gln) {
}
