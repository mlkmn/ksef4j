package io.github.mlkmn.ksef4j.internal.fa3;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import io.github.mlkmn.ksef4j.invoice.Address;
import io.github.mlkmn.ksef4j.invoice.Buyer;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import io.github.mlkmn.ksef4j.invoice.Item;
import io.github.mlkmn.ksef4j.invoice.Seller;
import io.github.mlkmn.ksef4j.invoice.VatRate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceValidatorTest {

  @Test
  void valid_fixture_invoice_passes() {
    assertThatCode(() -> InvoiceValidator.validate(InvoiceFixtures.singleLineVat23()))
        .doesNotThrowAnyException();
  }

  @Test
  void seller_nip_must_be_ten_digits() {
    Invoice invoice = invoiceWithSellerNip("123");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller NIP")
        .hasMessageContaining("123");
  }

  @Test
  void seller_nip_must_be_numeric() {
    Invoice invoice = invoiceWithSellerNip("abcdefghij");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller NIP");
  }

  @Test
  void buyer_nip_must_be_ten_digits() {
    Invoice invoice = invoiceWithBuyerNip("99");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Buyer NIP");
  }

  @Test
  void invoice_number_must_not_be_empty() {
    Invoice invoice = withInvoiceNumber("");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Invoice number");
  }

  @Test
  void invoice_number_must_not_be_blank() {
    Invoice invoice = withInvoiceNumber("   ");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Invoice number");
  }

  @Test
  void seller_name_must_not_be_blank() {
    Invoice invoice = withSellerName("");

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller name");
  }

  @Test
  void buyer_name_max_length_is_512() {
    String tooLong = "x".repeat(513);
    Invoice invoice = withBuyerName(tooLong);

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Buyer name")
        .hasMessageContaining("512");
  }

  @Test
  void seller_address_line1_must_not_be_blank() {
    Invoice invoice = withSellerAddress(new Address("PL", "  ", null, null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller address");
  }

  @Test
  void address_line2_if_present_max_length_is_512() {
    String tooLong = "x".repeat(513);
    Invoice invoice = withSellerAddress(new Address("PL", "ul. X 1", tooLong, null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller address line 2");
  }

  @Test
  void item_description_must_not_be_blank() {
    Invoice invoice =
        withSingleItem(
            new Item("", BigDecimal.ONE, new BigDecimal("10.00"), VatRate.VAT_23, null, null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Item 1 description");
  }

  @Test
  void item_quantity_must_be_positive() {
    Invoice invoice =
        withSingleItem(
            new Item("Desc", BigDecimal.ZERO, new BigDecimal("10.00"), VatRate.VAT_23, null, null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Item 1 quantity");
  }

  @Test
  void item_unit_price_must_be_non_negative() {
    Invoice invoice =
        withSingleItem(
            new Item("Desc", BigDecimal.ONE, new BigDecimal("-1.00"), VatRate.VAT_23, null, null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Item 1 unit price");
  }

  @Test
  void non_pln_currency_requires_exchange_rate() {
    Invoice invoice = withCurrencyAndRate("EUR", null);

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("EUR")
        .hasMessageContaining("exchange rate");
  }

  @Test
  void non_pln_exchange_rate_must_be_positive() {
    Invoice invoice = withCurrencyAndRate("EUR", BigDecimal.ZERO);

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("exchange rate");
  }

  @Test
  void pln_invoice_must_not_carry_exchange_rate() {
    Invoice invoice = withCurrencyAndRate("PLN", new BigDecimal("4.2489"));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("PLN");
  }

  @Test
  void eur_invoice_with_positive_rate_passes() {
    assertThatCode(() -> InvoiceValidator.validate(InvoiceFixtures.eurSingleLineVat23()))
        .doesNotThrowAnyException();
  }

  @Test
  void item_unit_max_length_is_256() {
    Invoice invoice =
        withSingleItem(
            new Item(
                "Desc",
                BigDecimal.ONE,
                new BigDecimal("10.00"),
                VatRate.VAT_23,
                "x".repeat(257),
                null));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Item 1 unit")
        .hasMessageContaining("256");
  }

  @Test
  void item_pkwiu_max_length_is_50() {
    Invoice invoice =
        withSingleItem(
            new Item(
                "Desc",
                BigDecimal.ONE,
                new BigDecimal("10.00"),
                VatRate.VAT_23,
                null,
                "9".repeat(51)));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Item 1 PKWiU")
        .hasMessageContaining("50");
  }

  @Test
  void empty_items_is_rejected() {
    Invoice base = InvoiceFixtures.singleLineVat23();
    Invoice empty =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            base.currency(),
            base.exchangeRate(),
            base.seller(),
            base.buyer(),
            List.of());

    assertThatThrownBy(() -> InvoiceValidator.validate(empty))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("at least one item");
  }

  @Test
  void null_seller_address_is_rejected_without_npe() {
    Invoice base = InvoiceFixtures.singleLineVat23();
    Seller noAddress = new Seller(base.seller().nip(), base.seller().name(), null);
    Invoice invoice =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            base.currency(),
            base.exchangeRate(),
            noAddress,
            base.buyer(),
            base.items());

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller address");
  }

  @Test
  void unsupported_currency_code_is_rejected() {
    Invoice base = InvoiceFixtures.singleLineVat23();
    Invoice invoice =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            "XYZ",
            new BigDecimal("4.30"),
            base.seller(),
            base.buyer(),
            base.items());

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("currency");
  }

  @Test
  void unsupported_country_code_is_rejected() {
    Invoice base = InvoiceFixtures.singleLineVat23();
    Seller badCountry =
        new Seller(
            base.seller().nip(),
            base.seller().name(),
            new Address("ZZ", "ul. Testowa 1", null, null));
    Invoice invoice =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            base.currency(),
            base.exchangeRate(),
            badCountry,
            base.buyer(),
            base.items());

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("country code");
  }

  @Test
  void null_item_vat_rate_is_rejected() {
    Invoice base = InvoiceFixtures.singleLineVat23();
    Item noVat = new Item("Widget", new BigDecimal("1"), new BigDecimal("10.00"), null, null, null);
    Invoice invoice =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            base.currency(),
            base.exchangeRate(),
            base.seller(),
            base.buyer(),
            List.of(noVat));

    assertThatThrownBy(() -> InvoiceValidator.validate(invoice))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("VAT rate");
  }

  // ---- Fixture helpers ----

  private static Invoice withCurrencyAndRate(String currency, BigDecimal rate) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        currency,
        rate,
        base.seller(),
        base.buyer(),
        base.items());
  }

  private static Invoice invoiceWithSellerNip(String nip) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        new Seller(nip, base.seller().name(), base.seller().address()),
        base.buyer(),
        base.items());
  }

  private static Invoice invoiceWithBuyerNip(String nip) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        base.seller(),
        new Buyer(nip, base.buyer().name(), base.buyer().address()),
        base.items());
  }

  private static Invoice withInvoiceNumber(String number) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        number,
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        base.seller(),
        base.buyer(),
        base.items());
  }

  private static Invoice withSellerName(String name) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        new Seller(base.seller().nip(), name, base.seller().address()),
        base.buyer(),
        base.items());
  }

  private static Invoice withBuyerName(String name) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        base.seller(),
        new Buyer(base.buyer().nip(), name, base.buyer().address()),
        base.items());
  }

  private static Invoice withSellerAddress(Address address) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        new Seller(base.seller().nip(), base.seller().name(), address),
        base.buyer(),
        base.items());
  }

  private static Invoice withSingleItem(Item item) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        null,
        base.seller(),
        base.buyer(),
        List.of(item));
  }
}
