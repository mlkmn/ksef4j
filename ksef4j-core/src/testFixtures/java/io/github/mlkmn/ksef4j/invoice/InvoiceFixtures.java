package io.github.mlkmn.ksef4j.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Programmatic Invoice fixtures shared across tests. */
public final class InvoiceFixtures {

  private static final Address SELLER_ADDRESS =
      new Address("PL", "ul. Marszalkowska 1/2", "00-000 Warszawa", null);
  private static final Address BUYER_ADDRESS = new Address("PL", "ul. Pulawska 100", null, null);

  private InvoiceFixtures() {}

  public static Invoice singleLineVat23() {
    return new Invoice(
        "FV/2026/05/001",
        LocalDate.of(2026, 5, 9),
        null,
        null,
        null,
        new Seller("5260250274", "Example Sp. z o.o.", SELLER_ADDRESS),
        new Buyer("1234567890", "Customer Sp. z o.o.", BUYER_ADDRESS),
        List.of(
            new Item(
                "Consulting services, March 2026",
                new BigDecimal("1"),
                new BigDecimal("10000.00"),
                VatRate.VAT_23,
                null,
                null)));
  }

  public static Invoice eurSingleLineVat23() {
    return new Invoice(
        "FV/2026/05/010",
        LocalDate.of(2026, 5, 9),
        null,
        "EUR",
        new BigDecimal("4.2489"),
        new Seller("5260250274", "Example Sp. z o.o.", SELLER_ADDRESS),
        new Buyer("1234567890", "Customer Sp. z o.o.", BUYER_ADDRESS),
        List.of(
            new Item(
                "Consulting services",
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                "godzina",
                "62.01.11.0")));
  }

  public static Invoice eurTwoTaxedBands() {
    return new Invoice(
        "FV/2026/05/011",
        LocalDate.of(2026, 5, 9),
        null,
        "EUR",
        new BigDecimal("4.0000"),
        new Seller("5260250274", "Example Sp. z o.o.", SELLER_ADDRESS),
        new Buyer("1234567890", "Customer Sp. z o.o.", BUYER_ADDRESS),
        List.of(
            new Item(
                "Item 23%",
                new BigDecimal("10"), new BigDecimal("100.00"), VatRate.VAT_23, null, null),
            new Item(
                "Item 8%",
                new BigDecimal("10"), new BigDecimal("50.00"), VatRate.VAT_8, null, null)));
  }

  public static Invoice multiLineMixedRates() {
    return new Invoice(
        "FV/2026/05/002",
        LocalDate.of(2026, 5, 9),
        LocalDate.of(2026, 5, 8),
        "PLN",
        null,
        new Seller("5260250274", "Example Sp. z o.o.", SELLER_ADDRESS),
        new Buyer("1234567890", "Customer Sp. z o.o.", BUYER_ADDRESS),
        List.of(
            new Item(
                "Item A 23%",
                new BigDecimal("2"), new BigDecimal("100.00"), VatRate.VAT_23, null, null),
            new Item(
                "Item B 8%",
                new BigDecimal("3"), new BigDecimal("50.00"), VatRate.VAT_8, null, null),
            new Item(
                "Item C 0%",
                new BigDecimal("1"), new BigDecimal("75.00"), VatRate.VAT_0, null, null)));
  }
}
