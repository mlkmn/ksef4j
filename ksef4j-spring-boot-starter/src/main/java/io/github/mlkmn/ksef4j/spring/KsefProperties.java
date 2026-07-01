package io.github.mlkmn.ksef4j.spring;

import io.github.mlkmn.ksef4j.Environment;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/** Binds {@code ksef.*} configuration for the Spring Boot starter. */
@ConfigurationProperties("ksef")
public record KsefProperties(
        @DefaultValue("test") Environment environment,
        URI baseUrl,
        @DefaultValue Auth auth,
        @DefaultValue Context context,
        @DefaultValue Archive archive,
        @DefaultValue Upo upo) {

    /** KSeF API authentication. */
    public record Auth(String token) {
    }

    /** The taxpayer context the token acts for. */
    public record Context(String nip) {
    }

    /** Local archive settings. */
    public record Archive(Path directory) {
    }

    /** UPO polling settings. */
    public record Upo(@DefaultValue("180s") Duration pollTimeout) {
    }
}
