package io.github.mlkmn.ksef4j.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.error.UpoTimeoutException;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import io.github.mlkmn.ksef4j.internal.archive.NoOpInvoiceArchive;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSendResultTest {

  private static final byte[] FA3 = "<xml/>".getBytes(StandardCharsets.UTF_8);
  private static final Instant SENT_AT = Instant.parse("2026-06-28T10:15:30Z");
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(30);
  private static final Upo UPO =
      new Upo(
          "ksef-ref-9",
          "upo-ref-9",
          Instant.parse("2026-06-28T10:16:00Z"),
          null,
          null,
          "<upo/>".getBytes(StandardCharsets.UTF_8));

  private DefaultSendResult newResult(FakeUpoPoller poller, RecordingArchive archive) {
    return new DefaultSendResult(
        poller,
        archive,
        "session-1",
        "invoice-1",
        "access-token",
        "5260250274",
        FA3,
        SENT_AT,
        POLL_TIMEOUT,
        null);
  }

  @Test
  void invoiceReferenceNumber_available_immediately() {
    DefaultSendResult result = newResult(new FakeUpoPoller(UPO), new RecordingArchive());
    assertThat(result.invoiceReferenceNumber()).isEqualTo("invoice-1");
  }

  @Test
  void awaitUpo_polls_with_configured_timeout_and_archives_once() {
    FakeUpoPoller poller = new FakeUpoPoller(UPO);
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(poller, archive);

    Upo upo = result.awaitUpo();

    assertThat(upo).isSameAs(UPO);
    assertThat(poller.lastSessionRef).isEqualTo("session-1");
    assertThat(poller.lastAccessToken).isEqualTo("access-token");
    assertThat(poller.lastTimeout).isEqualTo(POLL_TIMEOUT);
    assertThat(archive.stored).hasSize(1);
    ArchiveEntry entry = archive.stored.get(0);
    assertThat(entry.key().ksefNumber()).isEqualTo("ksef-ref-9");
    assertThat(entry.key().issuerNip()).isEqualTo("5260250274");
    assertThat(entry.sentAt()).isEqualTo(SENT_AT);
    assertThat(entry.fa3Xml()).isEqualTo(FA3);
    assertThat(entry.upoXml()).isEqualTo(UPO.xml());
  }

  @Test
  void awaitUpo_caches_result_without_repolling_or_rearchiving() {
    FakeUpoPoller poller = new FakeUpoPoller(UPO);
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(poller, archive);

    Upo first = result.awaitUpo();
    Upo second = result.awaitUpo();

    assertThat(second).isSameAs(first);
    assertThat(poller.calls).isEqualTo(1);
    assertThat(archive.calls).isEqualTo(1);
  }

  @Test
  void awaitUpo_swallows_archive_failure_and_returns_upo() {
    FakeUpoPoller poller = new FakeUpoPoller(UPO);
    RecordingArchive archive = new RecordingArchive();
    archive.failTimes = 1;
    DefaultSendResult result = newResult(poller, archive);

    Upo upo = result.awaitUpo();

    assertThat(upo).isSameAs(UPO);
    assertThat(poller.calls).isEqualTo(1);

    result.awaitUpo();
    assertThat(archive.calls).isEqualTo(1);
    assertThat(archive.stored).isEmpty();
  }

  @Test
  void awaitUpo_propagates_timeout_and_stays_repollable() {
    FakeUpoPoller poller = new FakeUpoPoller(UPO);
    poller.toThrow = new UpoTimeoutException(Duration.ofSeconds(5), "timed out");
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(poller, archive);

    assertThatThrownBy(result::awaitUpo).isInstanceOf(UpoTimeoutException.class);
    assertThat(archive.calls).isZero();

    poller.toThrow = null;
    Upo upo = result.awaitUpo();
    assertThat(upo).isSameAs(UPO);
    assertThat(poller.calls).isEqualTo(2);
    assertThat(archive.stored).hasSize(1);
  }

  @Test
  void awaitUpo_with_noop_archive_succeeds() {
    DefaultSendResult result =
        new DefaultSendResult(
            new FakeUpoPoller(UPO),
            new NoOpInvoiceArchive(),
            "session-1",
            "invoice-1",
            "access-token",
            "5260250274",
            FA3,
            SENT_AT,
            POLL_TIMEOUT,
            null);
    assertThat(result.awaitUpo()).isSameAs(UPO);
  }

  @Test
  void awaitUpo_after_close_throws() {
    DefaultSendResult result = newResult(new FakeUpoPoller(UPO), new RecordingArchive());
    result.close();
    assertThatThrownBy(result::awaitUpo).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void close_is_idempotent() {
    DefaultSendResult result = newResult(new FakeUpoPoller(UPO), new RecordingArchive());
    result.close();
    result.close();
  }

  private static String sha256Base64(byte[] data) throws Exception {
    return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
  }

  @Test
  void awaitUpo_passes_when_document_hash_matches() throws Exception {
    Upo matching =
        new Upo(
            "ksef-ref-9",
            "upo-ref-9",
            Instant.parse("2026-06-28T10:16:00Z"),
            sha256Base64(FA3),
            "FV/1",
            "<upo/>".getBytes(StandardCharsets.UTF_8));
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(new FakeUpoPoller(matching), archive);

    assertThat(result.awaitUpo()).isSameAs(matching);
    assertThat(archive.stored).hasSize(1);
  }

  @Test
  void awaitUpo_throws_when_document_hash_mismatches_and_does_not_archive() {
    Upo wrong =
        new Upo(
            "ksef-ref-9",
            "upo-ref-9",
            Instant.parse("2026-06-28T10:16:00Z"),
            "TUlTTUFUQ0gtSEFTSA==",
            "FV/1",
            "<upo/>".getBytes(StandardCharsets.UTF_8));
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(new FakeUpoPoller(wrong), archive);

    assertThatThrownBy(result::awaitUpo)
        .isInstanceOf(UpoVerificationException.class)
        .hasMessageContaining("invoice-1");
    assertThat(archive.stored).isEmpty();
  }

  @Test
  void archive_metadata_is_populated_from_the_upo() throws Exception {
    Upo upo =
        new Upo(
            "ksef-ref-9",
            "upo-ref-9",
            Instant.parse("2026-06-28T10:16:00Z"),
            sha256Base64(FA3),
            "FV/1",
            "<upo/>".getBytes(StandardCharsets.UTF_8));
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result = newResult(new FakeUpoPoller(upo), archive);

    result.awaitUpo();

    Map<String, String> meta = archive.stored.get(0).metadata();
    assertThat(meta)
        .hasSize(6)
        .containsEntry("ksefNumber", "ksef-ref-9")
        .containsEntry("invoiceReferenceNumber", "invoice-1")
        .containsEntry("invoiceNumber", "FV/1")
        .containsEntry("documentHash", sha256Base64(FA3))
        .containsEntry("issuedAt", "2026-06-28T10:16:00Z")
        .containsEntry("issuerNip", "5260250274");
  }

  @Test
  void awaitUpo_runs_signature_check_and_archives_when_it_passes() {
    Upo upo =
        new Upo(
            "ksef-ref-9",
            "upo-ref-9",
            Instant.parse("2026-06-28T10:16:00Z"),
            null,
            null,
            "<upo/>".getBytes(StandardCharsets.UTF_8));
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result =
        new DefaultSendResult(
            new FakeUpoPoller(upo),
            archive,
            "session-1",
            "invoice-1",
            "access-token",
            "5260250274",
            FA3,
            SENT_AT,
            POLL_TIMEOUT,
            xml -> {});

    assertThat(result.awaitUpo()).isSameAs(upo);
    assertThat(archive.stored).hasSize(1);
  }

  @Test
  void awaitUpo_throws_and_does_not_archive_when_signature_check_fails() {
    RecordingArchive archive = new RecordingArchive();
    DefaultSendResult result =
        new DefaultSendResult(
            new FakeUpoPoller(UPO),
            archive,
            "session-1",
            "invoice-1",
            "access-token",
            "5260250274",
            FA3,
            SENT_AT,
            POLL_TIMEOUT,
            xml -> {
              throw new UpoVerificationException("bad signature");
            });

    assertThatThrownBy(result::awaitUpo).isInstanceOf(UpoVerificationException.class);
    assertThat(archive.stored).isEmpty();
  }
}
