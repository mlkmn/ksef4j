package io.github.mlkmn.ksef4j.error;

import java.time.Duration;

/**
 * Polling for the UPO exceeded the client-configured timeout (see {@code
 * KsefClient.Builder.upoPollTimeout}).
 */
public final class UpoTimeoutException extends KsefException {

  private static final long serialVersionUID = 1L;

  private final Duration timeout;

  public UpoTimeoutException(Duration timeout, String message) {
    super(message);
    this.timeout = timeout;
  }

  public Duration timeout() {
    return timeout;
  }
}
