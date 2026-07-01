package io.github.mlkmn.ksef4j.internal.archive;

import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.error.ArchiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemInvoiceArchiveTest {

    @TempDir
    Path root;

    private FilesystemInvoiceArchive archive;

    @BeforeEach
    void setUp() {
        archive = new FilesystemInvoiceArchive(root);
    }

    private static ArchiveEntry sampleEntry() {
        return new ArchiveEntry(
                new ArchiveKey("20260628-ABC123", "5260250274"),
                Instant.parse("2026-06-28T10:15:30Z"),
                "<Faktura/>".getBytes(StandardCharsets.UTF_8),
                "<UPO/>".getBytes(StandardCharsets.UTF_8),
                Map.of("source", "test", "channel", "interactive"));
    }

    @Test
    void store_then_find_round_trips_all_fields() {
        ArchiveEntry entry = sampleEntry();
        archive.store(entry);

        Optional<ArchiveEntry> found = archive.find(entry.key());

        assertThat(found).isPresent();
        ArchiveEntry got = found.get();
        assertThat(got.key()).isEqualTo(entry.key());
        assertThat(got.sentAt()).isEqualTo(entry.sentAt());
        assertThat(got.fa3Xml()).isEqualTo(entry.fa3Xml());
        assertThat(got.upoXml()).isEqualTo(entry.upoXml());
        assertThat(got.metadata()).isEqualTo(entry.metadata());
    }

    @Test
    void store_writes_expected_on_disk_layout() throws IOException {
        ArchiveEntry entry = sampleEntry();
        archive.store(entry);

        Path dir = root.resolve("5260250274").resolve("20260628-ABC123");
        assertThat(dir.resolve("fa3.xml")).exists();
        assertThat(dir.resolve("upo.xml")).exists();
        assertThat(dir.resolve("metadata.json")).exists();
        assertThat(Files.readAllBytes(dir.resolve("fa3.xml"))).isEqualTo(entry.fa3Xml());
        assertThat(Files.readAllBytes(dir.resolve("upo.xml"))).isEqualTo(entry.upoXml());
        String json = Files.readString(dir.resolve("metadata.json"));
        assertThat(json).contains("2026-06-28T10:15:30Z").contains("source").contains("interactive");
    }

    @Test
    void find_absent_returns_empty() {
        assertThat(archive.find(new ArchiveKey("NOPE", "5260250274"))).isEmpty();
    }

    @Test
    void find_returns_empty_when_metadata_marker_missing() throws IOException {
        // partial write: fa3/upo present, metadata.json (commit marker) absent
        Path dir = root.resolve("5260250274").resolve("PARTIAL");
        Files.createDirectories(dir);
        Files.write(dir.resolve("fa3.xml"), "<Faktura/>".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("upo.xml"), "<UPO/>".getBytes(StandardCharsets.UTF_8));

        assertThat(archive.find(new ArchiveKey("PARTIAL", "5260250274"))).isEmpty();
    }

    @Test
    void re_store_same_key_overwrites_idempotently() {
        ArchiveKey key = new ArchiveKey("20260628-ABC123", "5260250274");
        archive.store(new ArchiveEntry(key, Instant.parse("2026-06-28T10:00:00Z"),
                "v1".getBytes(StandardCharsets.UTF_8), "u1".getBytes(StandardCharsets.UTF_8), Map.of()));
        archive.store(new ArchiveEntry(key, Instant.parse("2026-06-28T11:00:00Z"),
                "v2".getBytes(StandardCharsets.UTF_8), "u2".getBytes(StandardCharsets.UTF_8), Map.of("x", "y")));

        ArchiveEntry got = archive.find(key).orElseThrow();
        assertThat(got.fa3Xml()).isEqualTo("v2".getBytes(StandardCharsets.UTF_8));
        assertThat(got.sentAt()).isEqualTo(Instant.parse("2026-06-28T11:00:00Z"));
        assertThat(got.metadata()).isEqualTo(Map.of("x", "y"));
        assertThat(got.upoXml()).isEqualTo("u2".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void empty_metadata_round_trips() {
        ArchiveKey key = new ArchiveKey("REF-EMPTY", "5260250274");
        archive.store(new ArchiveEntry(key, Instant.parse("2026-06-28T10:00:00Z"),
                "fa3".getBytes(StandardCharsets.UTF_8), "upo".getBytes(StandardCharsets.UTF_8), Map.of()));

        assertThat(archive.find(key).orElseThrow().metadata()).isEmpty();
    }

    @Test
    void unsafe_key_segments_are_rejected_and_nothing_escapes_root() throws IOException {
        assertThatThrownBy(() -> archive.store(new ArchiveEntry(
                new ArchiveKey("..", "5260250274"),
                Instant.parse("2026-06-28T10:00:00Z"),
                "x".getBytes(StandardCharsets.UTF_8), "y".getBytes(StandardCharsets.UTF_8), Map.of())))
                .isInstanceOf(ArchiveException.class);

        assertThatThrownBy(() -> archive.find(new ArchiveKey("a/b", "5260250274")))
                .isInstanceOf(ArchiveException.class);

        assertThatThrownBy(() -> archive.find(new ArchiveKey("ok", "../escape")))
                .isInstanceOf(ArchiveException.class);

        try (var entries = Files.list(root)) {
            assertThat(entries.toList()).isEmpty();
        }
    }
}
