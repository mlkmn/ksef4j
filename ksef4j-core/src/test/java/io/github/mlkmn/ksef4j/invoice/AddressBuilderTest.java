package io.github.mlkmn.ksef4j.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressBuilderTest {

  @Test
  void builds_address_equal_to_record() {
    Address built =
        Address.builder().countryCode("PL").line1("ul. Glowna 1").line2("m. 5").gln("590").build();

    assertThat(built).isEqualTo(new Address("PL", "ul. Glowna 1", "m. 5", "590"));
  }

  @Test
  void optional_fields_default_to_null() {
    Address built = Address.builder().countryCode("PL").line1("ul. Glowna 1").build();

    assertThat(built.line2()).isNull();
    assertThat(built.gln()).isNull();
  }
}
