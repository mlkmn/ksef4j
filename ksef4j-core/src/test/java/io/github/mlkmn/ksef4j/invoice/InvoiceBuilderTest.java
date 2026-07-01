package io.github.mlkmn.ksef4j.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceValidator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceBuilderTest {

  private static Invoice.Builder validPln() {
    return Invoice.builder()
        .invoiceNumber("FV/2026/1")
        .issueDate(LocalDate.of(2026, 6, 30))
        .seller(
            s ->
                s.nip("5260250274")
                    .name("Example Sp. z o.o.")
                    .address(a -> a.countryCode("PL").line1("ul. Glowna 1")))
        .buyer(
            b ->
                b.nip("1234567890")
                    .name("Customer Sp. z o.o.")
                    .address(a -> a.countryCode("PL").line1("ul. Boczna 2")))
        .addItem(i -> i.description("Consulting").quantity("1").unitPrice("100.00").vat(23));
  }

  @Test
  void builds_a_valid_pln_invoice() {
    Invoice invoice = validPln().build();

    assertThat(invoice.invoiceNumber()).isEqualTo("FV/2026/1");
    assertThat(invoice.currency()).isEqualTo("PLN");
    assertThat(invoice.saleDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(invoice.items()).hasSize(1);
    assertThat(invoice.items().get(0).vatRate()).isEqualTo(VatRate.VAT_23);
  }

  @Test
  void exchange_rate_string_overload_builds_eur_invoice() {
    Invoice invoice = validPln().currency("EUR").exchangeRate("4.30").build();

    assertThat(invoice.currency()).isEqualTo("EUR");
    assertThat(invoice.exchangeRate()).isEqualByComparingTo("4.30");
  }

  @Test
  void build_rejects_invoice_with_no_items() {
    Invoice.Builder noItems =
        Invoice.builder()
            .invoiceNumber("FV/2026/1")
            .issueDate(LocalDate.of(2026, 6, 30))
            .seller(
                s ->
                    s.nip("5260250274")
                        .name("Example Sp. z o.o.")
                        .address(a -> a.countryCode("PL").line1("ul. Glowna 1")))
            .buyer(
                b ->
                    b.nip("1234567890")
                        .name("Customer Sp. z o.o.")
                        .address(a -> a.countryCode("PL").line1("ul. Boczna 2")));

    assertThatThrownBy(noItems::build)
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("at least one item");
  }

  @Test
  void build_rejects_bad_nip() {
    Invoice.Builder badNip =
        validPln()
            .seller(
                s ->
                    s.nip("123")
                        .name("Example Sp. z o.o.")
                        .address(a -> a.countryCode("PL").line1("ul. Glowna 1")));

    assertThatThrownBy(badNip::build)
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("Seller NIP");
  }

  @Test
  void items_list_replaces_accumulated_items() {
    Item one = Item.builder().description("A").quantity("1").unitPrice("1.00").vat(23).build();
    Item two = Item.builder().description("B").quantity("2").unitPrice("2.00").vat(23).build();

    Invoice invoice = validPln().items(List.of(one, two)).build();

    assertThat(invoice.items()).containsExactly(one, two);
  }

  @Test
  void prebuilt_and_lambda_nested_setters_are_equivalent() {
    Invoice viaLambda = validPln().build();

    Invoice viaPrebuilt =
        Invoice.builder()
            .invoiceNumber("FV/2026/1")
            .issueDate(LocalDate.of(2026, 6, 30))
            .seller(
                Seller.builder()
                    .nip("5260250274")
                    .name("Example Sp. z o.o.")
                    .address(new Address("PL", "ul. Glowna 1", null, null))
                    .build())
            .buyer(
                Buyer.builder()
                    .nip("1234567890")
                    .name("Customer Sp. z o.o.")
                    .address(new Address("PL", "ul. Boczna 2", null, null))
                    .build())
            .addItem(
                Item.builder()
                    .description("Consulting")
                    .quantity("1")
                    .unitPrice("100.00")
                    .vat(23)
                    .build())
            .build();

    assertThat(viaLambda).isEqualTo(viaPrebuilt);
  }

  @Test
  void built_invoice_passes_the_send_time_validator() {
    assertThatCode(() -> InvoiceValidator.validate(validPln().build())).doesNotThrowAnyException();
  }

  @Test
  void mutating_builder_after_build_does_not_affect_built_invoice() {
    Invoice.Builder builder = validPln();
    Invoice first = builder.build();
    int sizeBefore = first.items().size();

    builder.addItem(i -> i.description("Extra").quantity("1").unitPrice("1.00").vat(23));
    builder.build();

    assertThat(first.items()).hasSize(sizeBefore);
  }
}
