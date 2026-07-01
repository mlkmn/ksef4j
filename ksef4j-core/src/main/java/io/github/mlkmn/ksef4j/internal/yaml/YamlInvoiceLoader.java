package io.github.mlkmn.ksef4j.internal.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import io.github.mlkmn.ksef4j.invoice.Invoice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Private YAML loader for {@link Invoice}. Used by
 * {@link Invoice#fromYaml(java.nio.file.Path)} and its overloads. Configures
 * a single {@link ObjectMapper} once at class-load time; the configuration is
 * intentionally not exposed - v0.1 locks the YAML schema and the parser knobs.
 *
 * <p>All failures (parse, IO, unknown key, missing required, type mismatch,
 * unsupported VatRate) surface as {@link InvoiceValidationException}, with the
 * underlying Jackson or IO exception set as the cause.
 */
public final class YamlInvoiceLoader {

    private static final ObjectMapper MAPPER = YAMLMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private YamlInvoiceLoader() {}

    public static Invoice load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return MAPPER.readValue(in, Invoice.class);
        } catch (JsonProcessingException e) {
            throw wrap(e, "from " + path);
        } catch (IOException e) {
            throw new InvoiceValidationException(
                    "Unable to read invoice YAML from " + path + ": " + e.getMessage(), e);
        }
    }

    public static Invoice load(InputStream input) {
        try {
            return MAPPER.readValue(input, Invoice.class);
        } catch (JsonProcessingException e) {
            throw wrap(e, "from input stream");
        } catch (IOException e) {
            throw new InvoiceValidationException(
                    "Unable to read invoice YAML from input stream: " + e.getMessage(), e);
        }
    }

    public static Invoice load(String yaml) {
        try {
            return MAPPER.readValue(yaml, Invoice.class);
        } catch (JsonProcessingException e) {
            throw wrap(e, "from string");
        }
    }

    private static InvoiceValidationException wrap(JsonProcessingException e, String source) {
        var loc = e.getLocation();
        String where = loc == null
                ? source
                : source + " at line " + loc.getLineNr() + ", column " + loc.getColumnNr();
        return new InvoiceValidationException(
                "Invalid invoice YAML " + where + ": " + e.getOriginalMessage(), e);
    }
}
