package io.github.mlkmn.ksef4j.internal.fa3;

import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Schema-conformance smoke test for FA(3) marshalling. Loads the bundled XSDs
 * with all {@code maxOccurs} values >= 1000 clamped to 100 in memory (the
 * clamp dodges Xerces' DFA cost on huge cardinality bounds; required-field
 * enforcement lives on minOccurs / required="true" and is untouched). Then
 * runs each fixture invoice through {@link InvoiceMapper} +
 * {@link InvoiceMarshaller} and validates the output bytes against the
 * clamped schema.
 *
 * <p>Tagged {@code schema-conformance} so the default {@code gradle test}
 * task excludes it; invoke via {@code gradle validateFixtures}.
 */
@Tag("schema-conformance")
class Fa3SchemaConformanceTest {

    private static final Pattern HIGH_MAX_OCCURS = Pattern.compile("maxOccurs=\"\\d{4,}\"");
    private static final String[] XSD_RESOURCES = {
            "fa3.xsd",
            "StrukturyDanych_v10-0E.xsd",
            "ElementarneTypyDanych_v10-0E.xsd",
            "KodyKrajow_v10-0E.xsd"
    };

    private static Schema clampedSchema;
    private static final InvoiceMarshaller MARSHALLER = new InvoiceMarshaller();

    @BeforeAll
    static void loadClampedSchema() throws Exception {
        Path tempDir = Files.createTempDirectory("ksef4j-fa3-clamped-");
        tempDir.toFile().deleteOnExit();

        Path mainXsd = null;
        for (String name : XSD_RESOURCES) {
            String original;
            try (var in = Fa3SchemaConformanceTest.class.getResourceAsStream("/fa3/" + name)) {
                if (in == null) {
                    throw new IllegalStateException("Bundled XSD not found on classpath: /fa3/" + name);
                }
                original = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String clamped = HIGH_MAX_OCCURS.matcher(original).replaceAll(Matcher.quoteReplacement("maxOccurs=\"100\""));
            Path out = tempDir.resolve(name);
            Files.writeString(out, clamped, StandardCharsets.UTF_8);
            out.toFile().deleteOnExit();
            if (name.equals("fa3.xsd")) {
                mainXsd = out;
            }
        }

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
        clampedSchema = factory.newSchema(mainXsd.toFile());
    }

    @Test
    void single_line_fixture_validates_against_clamped_xsd() {
        byte[] xml = marshal(InvoiceFixtures.singleLineVat23());
        assertThatCode(() -> validate(xml)).doesNotThrowAnyException();
    }

    @Test
    void multi_line_fixture_validates_against_clamped_xsd() {
        byte[] xml = marshal(InvoiceFixtures.multiLineMixedRates());
        assertThatCode(() -> validate(xml)).doesNotThrowAnyException();
    }

    private static byte[] marshal(Invoice invoice) {
        return MARSHALLER.marshal(InvoiceMapper.toFaktura(invoice));
    }

    private static void validate(byte[] xml) throws SAXException, IOException {
        var validator = clampedSchema.newValidator();
        List<SAXException> errors = new ArrayList<>();
        validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override public void error(org.xml.sax.SAXParseException e) { errors.add(e); }
            @Override public void fatalError(org.xml.sax.SAXParseException e) throws SAXException { throw e; }
        });
        validator.validate(new StreamSource(new ByteArrayInputStream(xml)));
        if (!errors.isEmpty()) {
            throw new SAXException("Validation produced " + errors.size() + " error(s); first: " + errors.get(0).getMessage(), errors.get(0));
        }
    }
}
