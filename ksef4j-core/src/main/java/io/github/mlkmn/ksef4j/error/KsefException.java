package io.github.mlkmn.ksef4j.error;

/**
 * Base type for all errors raised by ksef4j. All subclasses are unchecked (extend {@link
 * RuntimeException}); this base is sealed so the set of failure modes is closed and adding a new
 * one is a deliberate API change.
 */
public abstract sealed class KsefException extends RuntimeException
    permits KsefAuthenticationException,
        KsefTransportException,
        KsefBusinessException,
        InvoiceValidationException,
        UnsupportedInvoiceFeatureException,
        UpoTimeoutException,
        UpoVerificationException,
        ResultTruncatedException,
        ArchiveException {

  private static final long serialVersionUID = 1L;

  protected KsefException(String message) {
    super(message);
  }

  protected KsefException(String message, Throwable cause) {
    super(message, cause);
  }
}
