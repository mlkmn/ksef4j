package io.github.mlkmn.ksef4j.error;

/**
 * The supplied invoice could not be loaded or validated. Covers YAML parse / load failures
 * (malformed YAML, unknown key, missing required field, type mismatch, unsupported VatRate, IO
 * error) and DTO validation failures raised by {@code InvoiceValidator}.
 */
public final class InvoiceValidationException extends KsefException {

  private static final long serialVersionUID = 1L;

  public InvoiceValidationException(String message) {
    super(message);
  }

  public InvoiceValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
