package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.function.Consumer;

/**
 * Issuer of the invoice. Maps to FA(3) {@code Podmiot1}.
 *
 * @param nip 10-digit NIP (no separators)
 * @param name registered company name
 * @param address postal address (FA(3) {@code Adres}); required
 */
public record Seller(
    @JsonProperty(required = true) String nip,
    @JsonProperty(required = true) String name,
    @JsonProperty(required = true) Address address) {

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link Seller}. No validation; the whole invoice is validated at build. */
  public static final class Builder {
    private String nip;
    private String name;
    private Address address;

    private Builder() {}

    public Builder nip(String nip) {
      this.nip = nip;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder address(Address address) {
      this.address = address;
      return this;
    }

    public Builder address(Consumer<Address.Builder> spec) {
      Address.Builder b = Address.builder();
      spec.accept(b);
      this.address = b.build();
      return this;
    }

    public Seller build() {
      return new Seller(nip, name, address);
    }
  }
}
