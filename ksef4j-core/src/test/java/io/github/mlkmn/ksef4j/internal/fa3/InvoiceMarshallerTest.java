package io.github.mlkmn.ksef4j.internal.fa3;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class InvoiceMarshallerTest {

  private final InvoiceMarshaller marshaller = new InvoiceMarshaller();

  @Test
  void marshals_empty_faktura_to_utf8_xml_with_root_element() {
    Faktura faktura = new Faktura();

    byte[] xml = marshaller.marshal(faktura);

    String s = new String(xml, StandardCharsets.UTF_8);
    assertThat(s).startsWith("<?xml").contains("encoding=\"UTF-8\"").contains("Faktura");
  }
}
