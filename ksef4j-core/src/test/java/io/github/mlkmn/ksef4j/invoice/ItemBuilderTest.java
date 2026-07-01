package io.github.mlkmn.ksef4j.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ItemBuilderTest {

  @Test
  void builds_item_equal_to_record() {
    Item built =
        Item.builder()
            .description("Consulting")
            .quantity(new BigDecimal("2"))
            .unitPrice(new BigDecimal("100.00"))
            .vatRate(VatRate.VAT_23)
            .unit("h")
            .pkwiu("62.01")
            .build();

    assertThat(built)
        .isEqualTo(
            new Item(
                "Consulting",
                new BigDecimal("2"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                "h",
                "62.01"));
  }

  @Test
  void string_and_int_overloads_match_typed_setters() {
    Item viaOverloads =
        Item.builder().description("Consulting").quantity("2").unitPrice("100.00").vat(23).build();

    Item viaTyped =
        Item.builder()
            .description("Consulting")
            .quantity(new BigDecimal("2"))
            .unitPrice(new BigDecimal("100.00"))
            .vatRate(VatRate.VAT_23)
            .build();

    assertThat(viaOverloads).isEqualTo(viaTyped);
  }

  @Test
  void optional_fields_default_to_null() {
    Item built = Item.builder().description("x").quantity("1").unitPrice("1.00").vat(0).build();

    assertThat(built.unit()).isNull();
    assertThat(built.pkwiu()).isNull();
  }
}
