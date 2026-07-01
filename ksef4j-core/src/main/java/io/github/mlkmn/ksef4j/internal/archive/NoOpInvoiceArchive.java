package io.github.mlkmn.ksef4j.internal.archive;

import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import java.util.Optional;

/**
 * Internal: no-op {@link InvoiceArchive} used when no archive directory is configured. Stores
 * nothing; finds nothing. Stateless.
 */
public final class NoOpInvoiceArchive implements InvoiceArchive {

  @Override
  public void store(ArchiveEntry entry) {
    // intentionally does nothing
  }

  @Override
  public Optional<ArchiveEntry> find(ArchiveKey key) {
    return Optional.empty();
  }
}
