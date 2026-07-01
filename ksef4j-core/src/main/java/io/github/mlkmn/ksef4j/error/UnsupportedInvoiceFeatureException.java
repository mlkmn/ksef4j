package io.github.mlkmn.ksef4j.error;

/**
 * The Invoice DTO uses an FA(3) shape that the v0.1 mapper does not model. See the FA(3) coverage
 * section of v0.1.md for the supported allowlist.
 */
public final class UnsupportedInvoiceFeatureException extends KsefException {

  private static final long serialVersionUID = 1L;

  public UnsupportedInvoiceFeatureException(String message) {
    super(message);
  }
}
