package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * One invoice line. Maps to FA(3) {@code FaWiersz}.
 *
 * @param description line description (P_7)
 * @param quantity line quantity (P_8B)
 * @param unitPrice net unit price (P_9A)
 * @param vatRate VAT rate (P_12)
 * @param unit unit of measure (P_8A); when null or blank the mapper uses "szt."
 * @param pkwiu PKWiU classification code (PKWiU element); omitted when null
 */
public record Item(
    @JsonProperty(required = true) String description,
    @JsonProperty(required = true) BigDecimal quantity,
    @JsonProperty(required = true) BigDecimal unitPrice,
    @JsonProperty(required = true) VatRate vatRate,
    String unit,
    String pkwiu) {

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link Item}. No validation; the whole invoice is validated at build. */
  public static final class Builder {
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private VatRate vatRate;
    private String unit;
    private String pkwiu;

    private Builder() {}

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder quantity(BigDecimal quantity) {
      this.quantity = quantity;
      return this;
    }

    public Builder quantity(String quantity) {
      this.quantity = new BigDecimal(quantity);
      return this;
    }

    public Builder unitPrice(BigDecimal unitPrice) {
      this.unitPrice = unitPrice;
      return this;
    }

    public Builder unitPrice(String unitPrice) {
      this.unitPrice = new BigDecimal(unitPrice);
      return this;
    }

    public Builder vatRate(VatRate vatRate) {
      this.vatRate = vatRate;
      return this;
    }

    public Builder vat(int percent) {
      this.vatRate = VatRate.ofPercent(percent);
      return this;
    }

    public Builder unit(String unit) {
      this.unit = unit;
      return this;
    }

    public Builder pkwiu(String pkwiu) {
      this.pkwiu = pkwiu;
      return this;
    }

    public Item build() {
      return new Item(description, quantity, unitPrice, vatRate, unit, pkwiu);
    }
  }
}
