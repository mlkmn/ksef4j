package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.internal.archive.NoOpInvoiceArchive;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KsefClientBuilderTest {

    @Test
    void build_requires_environment() {
        assertThatThrownBy(() -> KsefClient.builder()
                .tokenAuth("token", "5260250274")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("environment");
    }

    @Test
    void build_requires_token_and_nip() {
        assertThatThrownBy(() -> KsefClient.builder()
                .environment(Environment.TEST)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tokenAuth");
    }

    @Test
    void build_rejects_both_archive_options() {
        assertThatThrownBy(() -> KsefClient.builder()
                .environment(Environment.TEST)
                .tokenAuth("token", "5260250274")
                .archive(new NoOpInvoiceArchive())
                .archiveDirectory(Path.of("build/archive-builder-test"))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not both");
    }

    @Test
    void build_returns_client_for_valid_config() {
        KsefClient client = KsefClient.builder()
                .environment(Environment.TEST)
                .tokenAuth("token", "5260250274")
                .httpConnectTimeout(Duration.ofSeconds(5))
                .httpRequestTimeout(Duration.ofSeconds(15))
                .build();
        assertThat(client).isNotNull();
    }
}
