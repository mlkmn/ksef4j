package io.github.mlkmn.ksef4j.error;

/** I/O failure or a non-2xx HTTP response without a recognisable KSeF business code. */
public final class KsefTransportException extends KsefException {

  private static final long serialVersionUID = 1L;

  public KsefTransportException(String message) {
    super(message);
  }

  public KsefTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
