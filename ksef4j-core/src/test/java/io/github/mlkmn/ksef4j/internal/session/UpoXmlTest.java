package io.github.mlkmn.ksef4j.internal.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.KsefTransportException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class UpoXmlTest {

  @Test
  void extracts_ksef_number_and_issued_at() {
    byte[] xml =
        ("<upo:Potwierdzenie xmlns:upo=\"http://upo.schematy.mf.gov.pl/KSeF/v4-3\">"
                + "<upo:NumerKSeFDokumentu>1234567890-20260628-ABCDEF-01</upo:NumerKSeFDokumentu>"
                + "<upo:DataNadaniaNumeruKSeF>2026-06-28T10:15:30Z</upo:DataNadaniaNumeruKSeF>"
                + "</upo:Potwierdzenie>")
            .getBytes(StandardCharsets.UTF_8);

    UpoXml.Parsed parsed = UpoXml.parse(xml);

    assertThat(parsed.ksefReferenceNumber()).isEqualTo("1234567890-20260628-ABCDEF-01");
    assertThat(parsed.issuedAt()).isEqualTo(Instant.parse("2026-06-28T10:15:30Z"));
  }

  @Test
  void missing_element_throws_transport_exception() {
    byte[] xml =
        ("<upo:P xmlns:upo=\"urn:x\"><upo:Other>x</upo:Other></upo:P>")
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> UpoXml.parse(xml))
        .isInstanceOf(KsefTransportException.class)
        .hasMessageContaining("NumerKSeFDokumentu");
  }

  @Test
  void malformed_xml_throws_transport_exception() {
    assertThatThrownBy(() -> UpoXml.parse("not xml".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(KsefTransportException.class);
  }

  @Test
  void unparseable_date_throws_transport_exception() {
    byte[] xml =
        ("<upo:P xmlns:upo=\"urn:x\">"
                + "<upo:NumerKSeFDokumentu>REF-1</upo:NumerKSeFDokumentu>"
                + "<upo:DataNadaniaNumeruKSeF>not-a-date</upo:DataNadaniaNumeruKSeF>"
                + "</upo:P>")
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> UpoXml.parse(xml)).isInstanceOf(KsefTransportException.class);
  }

  @Test
  void parses_all_fields_from_a_real_upo() throws Exception {
    byte[] xml =
        Objects.requireNonNull(
                UpoXmlTest.class.getResourceAsStream("/upo/real-upo-v4-3.xml"),
                "fixture /upo/real-upo-v4-3.xml not found")
            .readAllBytes();

    UpoXml.Parsed parsed = UpoXml.parse(xml);

    assertThat(parsed.ksefReferenceNumber()).isEqualTo("1111111111-20260629-FIXTURE000000-AA");
    assertThat(parsed.issuedAt()).isNotNull();
    assertThat(parsed.documentHash()).isEqualTo("RklYVFVSRS1IQVNILUJBU0U2NC1WQUxVRT09");
    assertThat(parsed.invoiceNumber()).isEqualTo("FV/FIXTURE/001");
  }
}
