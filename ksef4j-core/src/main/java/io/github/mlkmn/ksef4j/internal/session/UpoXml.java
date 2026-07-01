package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.error.KsefTransportException;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Internal: extracts the invoice KSeF number and issue time from a UPO XML document. Matches by
 * element local name (namespace-tolerant). The element names follow the UpoV4_3 schema (validated
 * by the deferred live smoke test).
 */
final class UpoXml {

  private UpoXml() {}

  record Parsed(String ksefNumber, Instant issuedAt, String documentHash, String invoiceNumber) {}

  static Parsed parse(byte[] xml) {
    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setExpandEntityReferences(false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse(new ByteArrayInputStream(xml));
    } catch (Exception e) {
      throw new KsefTransportException("Could not parse UPO document", e);
    }
    String ksefNumber = requiredText(doc, "NumerKSeFDokumentu");
    String issued = requiredText(doc, "DataNadaniaNumeruKSeF");
    String documentHash = optionalText(doc, "SkrotDokumentu");
    String invoiceNumber = optionalText(doc, "NumerFaktury");
    try {
      return new Parsed(ksefNumber, Instant.parse(issued), documentHash, invoiceNumber);
    } catch (DateTimeParseException e) {
      throw new KsefTransportException("Unparseable UPO issue date: " + issued, e);
    }
  }

  private static String requiredText(Document doc, String localName) {
    NodeList nodes = doc.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0) {
      throw new KsefTransportException("UPO document missing element: " + localName);
    }
    String text = nodes.item(0).getTextContent();
    if (text == null || text.isBlank()) {
      throw new KsefTransportException("UPO document has empty element: " + localName);
    }
    return text.trim();
  }

  private static String optionalText(Document doc, String localName) {
    NodeList nodes = doc.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0) {
      return null;
    }
    String text = nodes.item(0).getTextContent();
    if (text == null || text.isBlank()) {
      return null;
    }
    return text.trim();
  }
}
