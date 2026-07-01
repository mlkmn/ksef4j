package io.github.mlkmn.ksef4j.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PartyBuilderTest {

  @Test
  void seller_builds_equal_to_record() {
    Address address = new Address("PL", "ul. Glowna 1", null, null);
    Seller built =
        Seller.builder().nip("5260250274").name("Example Sp. z o.o.").address(address).build();

    assertThat(built).isEqualTo(new Seller("5260250274", "Example Sp. z o.o.", address));
  }

  @Test
  void seller_lambda_address_matches_prebuilt() {
    Seller viaLambda =
        Seller.builder()
            .nip("5260250274")
            .name("Example Sp. z o.o.")
            .address(a -> a.countryCode("PL").line1("ul. Glowna 1"))
            .build();

    Seller viaPrebuilt =
        Seller.builder()
            .nip("5260250274")
            .name("Example Sp. z o.o.")
            .address(new Address("PL", "ul. Glowna 1", null, null))
            .build();

    assertThat(viaLambda).isEqualTo(viaPrebuilt);
  }

  @Test
  void buyer_builds_equal_to_record() {
    Buyer viaLambda =
        Buyer.builder()
            .nip("1234567890")
            .name("Customer Sp. z o.o.")
            .address(a -> a.countryCode("PL").line1("ul. Boczna 2"))
            .build();

    assertThat(viaLambda)
        .isEqualTo(
            new Buyer(
                "1234567890",
                "Customer Sp. z o.o.",
                new Address("PL", "ul. Boczna 2", null, null)));
  }
}
