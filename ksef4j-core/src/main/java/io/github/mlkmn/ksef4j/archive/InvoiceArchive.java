package io.github.mlkmn.ksef4j.archive;

import io.github.mlkmn.ksef4j.error.ArchiveException;
import java.util.Optional;

/**
 * SPI for storing and retrieving sent-invoice records.
 *
 * <p><b>Experimental:</b> this SPI may still evolve; callers and implementations should expect at
 * least source-compatible changes.
 *
 * <p>ksef4j ships {@code FilesystemInvoiceArchive} (per-NIP directory, files named by KSeF number)
 * and {@code NoOpInvoiceArchive} (used when no directory is configured).
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
