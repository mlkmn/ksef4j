package io.github.mlkmn.ksef4j.error;

/**
 * KSeF returned a business error. Exposes the KSeF-defined error code and the underlying HTTP
 * status for callers that need to branch on either.
 */
public final class KsefBusinessException extends KsefException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final int httpStatus;

  public KsefBusinessException(String code, int httpStatus, String message) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public String code() {
    return code;
  }

  public int httpStatus() {
    return httpStatus;
  }
}
