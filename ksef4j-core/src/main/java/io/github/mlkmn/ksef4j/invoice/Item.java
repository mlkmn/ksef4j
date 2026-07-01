package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * One invoice line. Maps to FA(3) {@code FaWiersz}.
 *
 * @param description line description (P_7)
 * @param quantity    line quantity (P_8B)
 * @param unitPrice   net unit price (P_9A)
 * @param vatRate     VAT rate (P_12)
 * @param unit        unit of measure (P_8A); when null or blank the mapper uses "szt."
 * @param pkwiu       PKWiU classification code (PKWiU element); omitted when null
 */
public record Item(
        @JsonProperty(required = true) String description,
        @JsonProperty(required = true) BigDecimal quantity,
        @JsonProperty(required = true) BigDecimal unitPrice,
        @JsonProperty(required = true) VatRate vatRate,
        String unit,
        String pkwiu) {
}
