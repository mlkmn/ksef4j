package io.github.mlkmn.ksef4j.invoice;

import java.math.BigDecimal;

/**
 * Currency-tagged decimal amount. Used by InvoiceMapper for computed totals.
 *
 * @param amount the decimal amount
 * @param currency ISO 4217 currency code (e.g. {@code "PLN"})
 */
public record Money(BigDecimal amount, String currency) {}
