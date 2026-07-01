package io.github.mlkmn.ksef4j.internal.crypto;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.security.PublicKey;

/**
 * Verifies the enveloped XML-DSig signature on a KSeF UPO against a pinned public key. The
 * document's own {@code KeyInfo} is ignored - trust comes solely from the pinned key. JDK-only.
 */
public final class UpoXmlSignature {

    private static final String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#";

    private UpoXmlSignature() {
    }

    public static void verify(byte[] upoXml, PublicKey pinnedKey) {
        Document doc = parse(upoXml);
        registerIdAttributes(doc);

        NodeList signatures = doc.getElementsByTagNameNS(DSIG_NS, "Signature");
        if (signatures.getLength() == 0) {
            throw new UpoVerificationException("UPO has no XML signature");
        }
        try {
            DOMValidateContext ctx = new DOMValidateContext(new FixedKey(pinnedKey), signatures.item(0));
            ctx.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx);
            if (!signature.validate(ctx)) {
                throw new UpoVerificationException(
                        "UPO signature is invalid (signature value or a reference digest did not verify)");
            }
        } catch (UpoVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new UpoVerificationException("Failed to verify UPO signature", e);
        }
    }

    private static Document parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new UpoVerificationException("Could not parse UPO document for signature verification", e);
        }
    }

    // A non-schema-validating DOM does not treat "Id" attributes as XML IDs, so same-document
    // references like URI="#SignedProperties" would not resolve. Register every "Id" as an ID.
    private static void registerIdAttributes(Document doc) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (element.hasAttribute("Id")) {
                element.setIdAttribute("Id", true);
            }
        }
    }

    private static final class FixedKey extends KeySelector {

        private final PublicKey key;

        FixedKey(PublicKey key) {
            this.key = key;
        }

        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                                        XMLCryptoContext context) {
            return () -> key;
        }
    }
}
