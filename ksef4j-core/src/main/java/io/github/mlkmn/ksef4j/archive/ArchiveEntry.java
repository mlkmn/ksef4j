package io.github.mlkmn.ksef4j.archive;

import java.time.Instant;
import java.util.Map;

/**
 * One archived send: the FA(3) document, the UPO, and free-form metadata.
 *
 * @param key      identifies this archive entry
 * @param sentAt   instant the invoice was sent (UTC)
 * @param fa3Xml   the FA(3) XML document that was sent to KSeF
 * @param upoXml   the UPO XML returned by KSeF
 * @param metadata free-form, opaque to the library; persisted as-is. Defaults to empty.
 */
public record ArchiveEntry(
        ArchiveKey key,
        Instant sentAt,
        byte[] fa3Xml,
        byte[] upoXml,
        Map<String, String> metadata
) {

    public ArchiveEntry {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
