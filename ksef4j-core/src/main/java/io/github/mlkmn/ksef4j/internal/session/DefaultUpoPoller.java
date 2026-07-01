package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import io.github.mlkmn.ksef4j.error.UpoTimeoutException;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Internal: with exponential backoff, first polls the per-invoice status as the authoritative
 * accept/reject signal (rejection fails fast with the KSeF reason), then on acceptance polls the
 * session status until the UPO is ready and assembles the {@link Upo}. Stateless/shareable.
 */
public final class DefaultUpoPoller implements UpoPoller {

  private static final int STATUS_SUCCESS = 200;

  private final HttpTransport transport;
  private final BackoffStrategy backoff;
  private final Clock clock;
  private final Sleeper sleeper;

  public DefaultUpoPoller(HttpTransport transport, BackoffStrategy backoff, Clock clock) {
    this(transport, backoff, clock, duration -> Thread.sleep(duration));
  }

  DefaultUpoPoller(HttpTransport transport, BackoffStrategy backoff, Clock clock, Sleeper sleeper) {
    this.transport = transport;
    this.backoff = backoff;
    this.clock = clock;
    this.sleeper = sleeper;
  }

  @Override
  public Upo pollUntilSettled(
      String sessionReferenceNumber,
      String invoiceReferenceNumber,
      String accessToken,
      Duration timeout) {
    Instant deadline = clock.instant().plus(timeout);
    int attempt = 1;

    // Phase 1: the per-invoice status is the authoritative accept/reject signal for a single
    // invoice (a session can report 200 "processed" with no UPO even when its invoice was
    // rejected).
    while (true) {
      Responses.InvoiceStatus invoice =
          transport.fetchInvoiceStatus(sessionReferenceNumber, invoiceReferenceNumber, accessToken);
      int code = invoice.status().code();
      if (code == STATUS_SUCCESS) {
        break; // accepted -> retrieve the UPO
      }
      if (!isProcessing(code)) {
        throw new KsefBusinessException(
            String.valueOf(code),
            STATUS_SUCCESS,
            "KSeF rejected the invoice (status " + code + "): " + invoice.status().description());
      }
      attempt = backoffOrTimeout(deadline, attempt, timeout, sessionReferenceNumber);
    }

    // Phase 2: the invoice is accepted; await the aggregate UPO. "200 + empty pages" now
    // unambiguously means the UPO is still being generated (never a rejected invoice).
    while (true) {
      Responses.SessionStatus status =
          transport.fetchSessionStatus(sessionReferenceNumber, accessToken);
      int code = status.status().code();
      if (code == STATUS_SUCCESS && status.upo() != null && !status.upo().pages().isEmpty()) {
        return assemble(status.upo().pages().get(0));
      }
      if (!isProcessing(code) && code != STATUS_SUCCESS) {
        throw new KsefBusinessException(
            String.valueOf(code),
            STATUS_SUCCESS,
            "KSeF session failed during UPO generation (session status "
                + code
                + "): "
                + status.status().description());
      }
      attempt = backoffOrTimeout(deadline, attempt, timeout, sessionReferenceNumber);
    }
  }

  private static boolean isProcessing(int code) {
    return code >= 100 && code < 200;
  }

  private int backoffOrTimeout(Instant deadline, int attempt, Duration timeout, String sessionRef) {
    if (!clock.instant().isBefore(deadline)) {
      throw new UpoTimeoutException(
          timeout, "UPO not ready within " + timeout + " for session " + sessionRef);
    }
    try {
      sleeper.sleep(backoff.delayBeforeAttempt(attempt));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new KsefTransportException("Interrupted while polling for UPO", e);
    }
    return attempt + 1;
  }

  private Upo assemble(Responses.UpoPage page) {
    byte[] xml = transport.fetchUpo(URI.create(page.downloadUrl()));
    UpoXml.Parsed parsed = UpoXml.parse(xml);
    return new Upo(
        parsed.ksefReferenceNumber(),
        page.referenceNumber(),
        parsed.issuedAt(),
        parsed.documentHash(),
        parsed.invoiceNumber(),
        xml);
  }
}
