package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.error.UpoTimeoutException;
import java.time.Duration;

/**
 * Internal seam: polls KSeF session status until the UPO is ready, then fetches and assembles it.
 * Implementation arrives in Wave B2.
 */
public interface UpoPoller {

  /**
   * Poll until the invoice is accepted and its UPO is ready, or a terminal failure. The per-invoice
   * status is the authoritative accept/reject signal; on acceptance the aggregate UPO is fetched
   * and assembled.
   *
   * @throws UpoTimeoutException if the timeout elapses first
   */
  Upo pollUntilSettled(
      String sessionReferenceNumber,
      String invoiceReferenceNumber,
      String accessToken,
      Duration timeout);
}
