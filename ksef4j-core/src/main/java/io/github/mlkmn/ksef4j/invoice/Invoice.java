package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mlkmn.ksef4j.internal.yaml.YamlInvoiceLoader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * English-named, YAML-friendly invoice DTO. Loaded via {@link #fromYaml(Path)}
 * (or the {@code InputStream} / {@code String} overloads) and translated to
 * FA(3) by {@code InvoiceMapper} (internal). v0.1 schema lives in v0.1.md.
 *
 * <p>The compact constructor applies two schema defaults: {@code saleDate}
 * defaults to {@code issueDate}, and {@code currency} defaults to {@code "PLN"}.
 * The {@code items} list is defensively copied.
 *
 * <p>A non-null {@code exchangeRate} is required for non-PLN invoices and is
 * emitted as the FA(3) {@code KursWalutyZ}.
 */
public record Invoice(
        @JsonProperty(required = true) String invoiceNumber,
        @JsonProperty(required = true) LocalDate issueDate,
        LocalDate saleDate,
        String currency,
        BigDecimal exchangeRate,
        @JsonProperty(required = true) Seller seller,
        @JsonProperty(required = true) Buyer buyer,
        @JsonProperty(required = true) List<Item> items
) {

    public Invoice {
        if (saleDate == null) {
            saleDate = issueDate;
        }
        if (currency == null) {
            currency = "PLN";
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Invoice fromYaml(Path path) {
        return YamlInvoiceLoader.load(path);
    }

    public static Invoice fromYaml(InputStream input) {
        return YamlInvoiceLoader.load(input);
    }

    public static Invoice fromYaml(String yaml) {
        return YamlInvoiceLoader.load(yaml);
    }
}
