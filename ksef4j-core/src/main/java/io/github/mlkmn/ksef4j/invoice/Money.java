package io.github.mlkmn.ksef4j.invoice;

import java.math.BigDecimal;

/** Currency-tagged decimal amount. Used by InvoiceMapper for computed totals. */
public record Money(BigDecimal amount, String currency) {
}
