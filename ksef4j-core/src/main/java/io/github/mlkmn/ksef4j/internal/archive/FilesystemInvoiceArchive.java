package io.github.mlkmn.ksef4j.internal.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.error.ArchiveException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Internal: filesystem {@link InvoiceArchive}. One directory per send, {@code
 * <root>/<issuerNip>/<ksefNumber>/}, holding {@code fa3.xml}, {@code upo.xml}, and {@code
 * metadata.json}. {@code metadata.json} is written last and is the commit marker. Holds only the
 * immutable root, so it is thread-safe to share across concurrent sends.
 */
public final class FilesystemInvoiceArchive implements InvoiceArchive {

  private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
  private static final String FA3 = "fa3.xml";
  private static final String UPO = "upo.xml";
  private static final String METADATA = "metadata.json";

  private final Path root;
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  public FilesystemInvoiceArchive(Path root) {
    this.root = root;
  }

  // Sidecar persisted as metadata.json. Package-private (not private) so Jackson's
  // record support can access the canonical constructor without reflection issues.
  record MetadataSidecar(String sentAt, Map<String, String> metadata) {}

  @Override
  public void store(ArchiveEntry entry) {
    Path dir = entryDir(entry.key());
    try {
      Files.createDirectories(dir);
      writeAtomic(dir.resolve(FA3), entry.fa3Xml());
      writeAtomic(dir.resolve(UPO), entry.upoXml());
      byte[] sidecar =
          MAPPER.writeValueAsBytes(
              new MetadataSidecar(entry.sentAt().toString(), entry.metadata()));
      writeAtomic(dir.resolve(METADATA), sidecar); // commit marker - written last
    } catch (IOException e) {
      throw new ArchiveException("Failed to write archive entry at " + dir, e);
    }
  }

  @Override
  public Optional<ArchiveEntry> find(ArchiveKey key) {
    Path dir = entryDir(key);
    Path metadataFile = dir.resolve(METADATA);
    if (!Files.exists(metadataFile)) {
      return Optional.empty();
    }
    try {
      MetadataSidecar sidecar =
          MAPPER.readValue(Files.readAllBytes(metadataFile), MetadataSidecar.class);
      byte[] fa3 = Files.readAllBytes(dir.resolve(FA3));
      byte[] upo = Files.readAllBytes(dir.resolve(UPO));
      return Optional.of(
          new ArchiveEntry(key, Instant.parse(sidecar.sentAt()), fa3, upo, sidecar.metadata()));
    } catch (IOException e) {
      throw new ArchiveException("Failed to read archive entry at " + dir, e);
    }
  }

  private Path entryDir(ArchiveKey key) {
    return root.resolve(safe(key.issuerNip(), "issuerNip"))
        .resolve(safe(key.ksefNumber(), "ksefNumber"));
  }

  private static String safe(String segment, String field) {
    if (segment == null
        || !SAFE_SEGMENT.matcher(segment).matches()
        || segment.equals(".")
        || segment.equals("..")) {
      throw new ArchiveException("Unsafe " + field + " for filesystem archive: " + segment);
    }
    return segment;
  }

  private static void writeAtomic(Path target, byte[] bytes) throws IOException {
    Path tmp = Files.createTempFile(target.getParent(), ".tmp-", null);
    try {
      Files.write(tmp, bytes);
      try {
        Files.move(
            tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      Files.deleteIfExists(tmp);
      throw e;
    }
  }
}
