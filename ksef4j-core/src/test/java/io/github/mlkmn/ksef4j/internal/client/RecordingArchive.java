package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.error.ArchiveException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Test double for {@link InvoiceArchive}: records stored entries; can be made to fail N times. */
final class RecordingArchive implements InvoiceArchive {

  final List<ArchiveEntry> stored = new ArrayList<>();
  int calls;
  int failTimes;

  @Override
  public void store(ArchiveEntry entry) {
    calls++;
    if (calls <= failTimes) {
      throw new ArchiveException("simulated archive failure");
    }
    stored.add(entry);
  }

  @Override
  public Optional<ArchiveEntry> find(ArchiveKey key) {
    return Optional.empty();
  }
}
