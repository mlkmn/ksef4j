package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceValidator;
import io.github.mlkmn.ksef4j.internal.yaml.YamlInvoiceLoader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * English-named, YAML-friendly invoice DTO. Loaded via {@link #fromYaml(Path)} (or the {@code
 * InputStream} / {@code String} overloads) and translated to FA(3) by {@code InvoiceMapper}
 * (internal).
 *
 * <p>The compact constructor applies two schema defaults: {@code saleDate} defaults to {@code
 * issueDate}, and {@code currency} defaults to {@code "PLN"}. The {@code items} list is defensively
 * copied.
 *
 * <p>A non-null {@code exchangeRate} is required for non-PLN invoices and is emitted as the FA(3)
 * {@code KursWalutyZ}.
 */
public record Invoice(
    @JsonProperty(required = true) String invoiceNumber,
    @JsonProperty(required = true) LocalDate issueDate,
    LocalDate saleDate,
    String currency,
    BigDecimal exchangeRate,
    @JsonProperty(required = true) Seller seller,
    @JsonProperty(required = true) Buyer buyer,
    @JsonProperty(required = true) List<Item> items) {

  public Invoice {
    if (saleDate == null) {
      saleDate = issueDate;
    }
    if (currency == null) {
      currency = "PLN";
    }
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static Invoice fromYaml(Path path) {
    return YamlInvoiceLoader.load(path);
  }

  public static Invoice fromYaml(InputStream input) {
    return YamlInvoiceLoader.load(input);
  }

  public static Invoice fromYaml(String yaml) {
    return YamlInvoiceLoader.load(yaml);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link Invoice}. {@link #build()} assembles the record (applying the same
   * {@code saleDate}/{@code currency} defaults) and validates it, throwing {@link
   * io.github.mlkmn.ksef4j.error.InvoiceValidationException} if it is incomplete or invalid.
   */
  public static final class Builder {
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate saleDate;
    private String currency;
    private BigDecimal exchangeRate;
    private Seller seller;
    private Buyer buyer;
    private final List<Item> items = new ArrayList<>();

    private Builder() {}

    public Builder invoiceNumber(String invoiceNumber) {
      this.invoiceNumber = invoiceNumber;
      return this;
    }

    public Builder issueDate(LocalDate issueDate) {
      this.issueDate = issueDate;
      return this;
    }

    public Builder saleDate(LocalDate saleDate) {
      this.saleDate = saleDate;
      return this;
    }

    public Builder currency(String currency) {
      this.currency = currency;
      return this;
    }

    public Builder exchangeRate(BigDecimal exchangeRate) {
      this.exchangeRate = exchangeRate;
      return this;
    }

    public Builder exchangeRate(String exchangeRate) {
      this.exchangeRate = new BigDecimal(exchangeRate);
      return this;
    }

    public Builder seller(Seller seller) {
      this.seller = seller;
      return this;
    }

    public Builder seller(Consumer<Seller.Builder> spec) {
      Seller.Builder b = Seller.builder();
      spec.accept(b);
      this.seller = b.build();
      return this;
    }

    public Builder buyer(Buyer buyer) {
      this.buyer = buyer;
      return this;
    }

    public Builder buyer(Consumer<Buyer.Builder> spec) {
      Buyer.Builder b = Buyer.builder();
      spec.accept(b);
      this.buyer = b.build();
      return this;
    }

    public Builder addItem(Item item) {
      this.items.add(item);
      return this;
    }

    public Builder addItem(Consumer<Item.Builder> spec) {
      Item.Builder b = Item.builder();
      spec.accept(b);
      this.items.add(b.build());
      return this;
    }

    /**
     * Replaces the current item list with {@code items} (clears, then adds), unlike the additive
     * {@link #addItem(Item)}.
     */
    public Builder items(List<Item> items) {
      this.items.clear();
      if (items != null) {
        this.items.addAll(items);
      }
      return this;
    }

    public Invoice build() {
      Invoice invoice =
          new Invoice(
              invoiceNumber, issueDate, saleDate, currency, exchangeRate, seller, buyer, items);
      InvoiceValidator.validate(invoice);
      return invoice;
    }
  }
}
