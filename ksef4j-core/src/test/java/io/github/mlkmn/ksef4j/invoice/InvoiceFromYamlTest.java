package io.github.mlkmn.ksef4j.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceFromYamlTest {

  @Test
  void single_line_fixture_parses_to_expected_invoice() throws Exception {
    Path yaml = fixturePath("/yaml/single-line-vat23.yaml");

    Invoice actual = Invoice.fromYaml(yaml);

    assertThat(actual).isEqualTo(InvoiceFixtures.singleLineVat23());
  }

  @Test
  void multi_line_fixture_parses_to_expected_invoice() throws Exception {
    Path yaml = fixturePath("/yaml/multi-line-mixed-rates.yaml");

    Invoice actual = Invoice.fromYaml(yaml);

    assertThat(actual).isEqualTo(InvoiceFixtures.multiLineMixedRates());
  }

  @Test
  void defaults_applied_when_omitted() throws Exception {
    Path yaml = fixturePath("/yaml/defaults-applied.yaml");

    Invoice actual = Invoice.fromYaml(yaml);

    assertThat(actual.saleDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    assertThat(actual.currency()).isEqualTo("PLN");
  }

  @Test
  void all_three_overloads_produce_equal_invoices() throws Exception {
    Path yaml = fixturePath("/yaml/single-line-vat23.yaml");
    String content = Files.readString(yaml);

    Invoice fromPath = Invoice.fromYaml(yaml);
    Invoice fromString = Invoice.fromYaml(content);
    Invoice fromStream;
    try (InputStream in = openClasspathStream("/yaml/single-line-vat23.yaml")) {
      fromStream = Invoice.fromYaml(in);
    }

    assertThat(fromPath).isEqualTo(fromString).isEqualTo(fromStream);
  }

  @Test
  void missing_top_level_required_field_fails() throws Exception {
    Path yaml = fixturePath("/yaml/missing-invoice-number.yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("invoiceNumber");
  }

  @Test
  void missing_nested_required_field_fails() throws Exception {
    Path yaml = fixturePath("/yaml/missing-seller-address.yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("address");
  }

  @Test
  void unknown_property_fails() throws Exception {
    Path yaml = fixturePath("/yaml/unknown-key.yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("taxRegime");
  }

  @Test
  void unsupported_vat_rate_fails() throws Exception {
    Path yaml = fixturePath("/yaml/bad-vat-rate.yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("VAT rate");
  }

  @Test
  void malformed_yaml_fails_with_location() throws Exception {
    Path yaml = fixturePath("/yaml/malformed.yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining("line");
  }

  @Test
  void missing_file_fails() {
    Path yaml = Path.of("does-not-exist-" + System.nanoTime() + ".yaml");

    assertThatThrownBy(() -> Invoice.fromYaml(yaml))
        .isInstanceOf(InvoiceValidationException.class)
        .hasMessageContaining(yaml.toString());
  }

  private static Path fixturePath(String classpathResource) throws Exception {
    var url = InvoiceFromYamlTest.class.getResource(classpathResource);
    if (url == null) {
      throw new IllegalStateException("Missing test fixture on classpath: " + classpathResource);
    }
    return Paths.get(url.toURI());
  }

  private static InputStream openClasspathStream(String classpathResource) throws IOException {
    var in = InvoiceFromYamlTest.class.getResourceAsStream(classpathResource);
    if (in == null) {
      throw new IOException("Missing test fixture on classpath: " + classpathResource);
    }
    return in;
  }
}
