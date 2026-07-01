package io.github.mlkmn.ksef4j.internal.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NoOpInvoiceArchiveTest {

  private final NoOpInvoiceArchive archive = new NoOpInvoiceArchive();

  @Test
  void store_does_nothing_and_does_not_throw() {
    ArchiveEntry entry =
        new ArchiveEntry(
            new ArchiveKey("REF1", "5260250274"),
            Instant.parse("2026-06-28T10:00:00Z"),
            "fa3".getBytes(StandardCharsets.UTF_8),
            "upo".getBytes(StandardCharsets.UTF_8),
            Map.of("k", "v"));
    assertThatCode(() -> archive.store(entry)).doesNotThrowAnyException();
  }

  @Test
  void find_returns_empty() {
    assertThat(archive.find(new ArchiveKey("REF1", "5260250274"))).isEmpty();
  }
}
