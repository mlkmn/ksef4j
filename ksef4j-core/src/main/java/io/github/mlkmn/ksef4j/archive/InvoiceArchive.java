package io.github.mlkmn.ksef4j.archive;

import io.github.mlkmn.ksef4j.error.ArchiveException;

import java.util.Optional;

/**
 * SPI for storing and retrieving sent-invoice records.
 *
 * <p><b>Experimental:</b> the v0.1 surface may evolve before v1.0. Callers and
 * implementations should expect at least source-compatible changes.
 *
 * <p>v0.1 ships {@code FilesystemInvoiceArchive} (per-NIP directory, files named
 * by KSeF reference) and {@code NoOpInvoiceArchive} (used when no directory is
 * configured). Both arrive in Wave A5.
 */
public interface InvoiceArchive {

    /**
     * Persist one entry. Implementations should be idempotent for repeated keys.
     *
     * @throws ArchiveException if the underlying store fails
     */
    void store(ArchiveEntry entry) throws ArchiveException;

    /**
     * Look up an entry by its key.
     *
     * @return the entry if present, else {@link Optional#empty()}
     * @throws ArchiveException if the underlying store fails
     */
    Optional<ArchiveEntry> find(ArchiveKey key) throws ArchiveException;
}
