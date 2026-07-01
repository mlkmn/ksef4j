package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Postal address attached to a party (seller or buyer). Maps to FA(3) {@code TAdres}.
 *
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "PL"). Required.
 * @param line1 address line 1 (street, number; or full address up to 512 chars). Required.
 * @param line2 address line 2 (apartment, district). Optional; may be {@code null}.
 * @param gln Global Location Number. Optional; may be {@code null}.
 */
public record Address(
    @JsonProperty(required = true) String countryCode,
    @JsonProperty(required = true) String line1,
    String line2,
    String gln) {

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link Address}. No validation; the whole invoice is validated at build. */
  public static final class Builder {
    private String countryCode;
    private String line1;
    private String line2;
    private String gln;

    private Builder() {}

    public Builder countryCode(String countryCode) {
      this.countryCode = countryCode;
      return this;
    }

    public Builder line1(String line1) {
      this.line1 = line1;
      return this;
    }

    public Builder line2(String line2) {
      this.line2 = line2;
      return this;
    }

    public Builder gln(String gln) {
      this.gln = gln;
      return this;
    }

    public Address build() {
      return new Address(countryCode, line1, line2, gln);
    }
  }
}
