package io.github.mlkmn.ksef4j.internal.crypto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class UpoXmlSignatureTest {

  private static final String NS = "http://upo.schematy.mf.gov.pl/KSeF/v4-3";
  private static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

  private static KeyPair keyPair;
  private static KeyPair otherKeyPair;

  @BeforeAll
  static void keys() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    keyPair = gen.generateKeyPair();
    otherKeyPair = gen.generateKeyPair();
  }

  @Test
  void valid_signature_with_pinned_key_passes() throws Exception {
    byte[] signed = sign(keyPair, "SYNTHETIC-KSEF-1");
    assertThatCode(() -> UpoXmlSignature.verify(signed, keyPair.getPublic()))
        .doesNotThrowAnyException();
  }

  @Test
  void tampered_document_body_fails() throws Exception {
    byte[] signed = sign(keyPair, "SYNTHETIC-KSEF-1");
    byte[] tampered =
        new String(signed, StandardCharsets.UTF_8)
            .replace("SYNTHETIC-KSEF-1", "SYNTHETIC-KSEF-9")
            .getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> UpoXmlSignature.verify(tampered, keyPair.getPublic()))
        .isInstanceOf(UpoVerificationException.class);
  }

  @Test
  void verification_with_wrong_key_fails() throws Exception {
    byte[] signed = sign(keyPair, "SYNTHETIC-KSEF-1");
    assertThatThrownBy(() -> UpoXmlSignature.verify(signed, otherKeyPair.getPublic()))
        .isInstanceOf(UpoVerificationException.class);
  }

  @Test
  void document_without_signature_fails() throws Exception {
    byte[] unsigned =
        ("<Potwierdzenie xmlns=\""
                + NS
                + "\"><Dokument>"
                + "<NumerKSeFDokumentu>X</NumerKSeFDokumentu></Dokument></Potwierdzenie>")
            .getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> UpoXmlSignature.verify(unsigned, keyPair.getPublic()))
        .isInstanceOf(UpoVerificationException.class)
        .hasMessageContaining("no XML signature");
  }

  /**
   * Build a synthetic UPO and enveloped-sign it with two references: URI="" (the whole document)
   * and URI="#SignedProperties" (a same-document element carrying Id="SignedProperties"). This
   * exercises the verifier's Id-resolution path without a full XAdES Object.
   *
   * <p>The base XML must be serialized and re-parsed before signing so that sign and verify both
   * operate on a parsed DOM. A programmatically-built DOM (createElementNS) can produce a different
   * canonical form than a parsed DOM for the URI="" reference, causing the digest to differ on
   * round-trip even though the document bytes are identical.
   */
  private static byte[] sign(KeyPair kp, String ksefNumber) throws Exception {
    String baseXml =
        "<Potwierdzenie xmlns=\""
            + NS
            + "\"><Dokument Id=\"SignedProperties\">"
            + "<NumerKSeFDokumentu>"
            + ksefNumber
            + "</NumerKSeFDokumentu></Dokument></Potwierdzenie>";

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc =
        dbf.newDocumentBuilder()
            .parse(new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)));
    Element root = doc.getDocumentElement();
    Element dok = (Element) doc.getElementsByTagNameNS(NS, "Dokument").item(0);
    dok.setIdAttribute("Id", true);

    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
    DigestMethod sha256 = fac.newDigestMethod(DigestMethod.SHA256, null);
    Reference enveloped =
        fac.newReference(
            "",
            sha256,
            List.of(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
            null,
            null);
    Reference toProps =
        fac.newReference(
            "#SignedProperties",
            sha256,
            List.of(
                fac.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null)),
            "http://uri.etsi.org/01903#SignedProperties",
            null);
    SignedInfo si =
        fac.newSignedInfo(
            fac.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
            fac.newSignatureMethod(RSA_SHA256, null),
            List.of(enveloped, toProps));
    XMLSignature signature = fac.newXMLSignature(si, null);
    signature.sign(new DOMSignContext(kp.getPrivate(), root));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(doc), new StreamResult(out));
    return out.toByteArray();
  }
}
