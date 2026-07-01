package io.github.mlkmn.ksef4j.internal.fa3;

import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Marshals a {@link Faktura} JAXB tree into UTF-8 XML bytes. Thread-safe: {@link JAXBContext} is
 * built once in the constructor and reused; {@link Marshaller} instances are short-lived and
 * per-call, per the JAXB spec.
 */
public final class InvoiceMarshaller {

  private final JAXBContext context;

  public InvoiceMarshaller() {
    try {
      this.context = JAXBContext.newInstance(Faktura.class);
    } catch (JAXBException e) {
      throw new IllegalStateException("Failed to build JAXBContext for FA(3) Faktura", e);
    }
  }

  /** Marshal {@code faktura} to UTF-8 XML bytes. */
  public byte[] marshal(Faktura faktura) {
    try {
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      var out = new ByteArrayOutputStream();
      marshaller.marshal(faktura, out);
      return out.toByteArray();
    } catch (JAXBException e) {
      throw new IllegalStateException("FA(3) marshalling failed", e);
    }
  }
}
