package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.internal.session.UpoPoller;
import java.time.Duration;

/** Test double for {@link UpoPoller}: returns a fixed Upo (or throws), recording its arguments. */
final class FakeUpoPoller implements UpoPoller {

  int calls;
  String lastSessionRef;
  String lastInvoiceRef;
  String lastAccessToken;
  Duration lastTimeout;
  RuntimeException toThrow;
  private final Upo upo;

  FakeUpoPoller(Upo upo) {
    this.upo = upo;
  }

  @Override
  public Upo pollUntilSettled(
      String sessionReferenceNumber,
      String invoiceReferenceNumber,
      String accessToken,
      Duration timeout) {
    calls++;
    lastSessionRef = sessionReferenceNumber;
    lastInvoiceRef = invoiceReferenceNumber;
    lastAccessToken = accessToken;
    lastTimeout = timeout;
    if (toThrow != null) {
      throw toThrow;
    }
    return upo;
  }
}
