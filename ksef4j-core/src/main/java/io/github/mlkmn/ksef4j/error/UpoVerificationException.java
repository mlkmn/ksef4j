package io.github.mlkmn.ksef4j.error;

/**
 * Raised when a received UPO fails verification - e.g. its document hash does not match the
 * invoice that was sent. Signals a receipt that must not be trusted as-is.
 */
public final class UpoVerificationException extends KsefException {

    private static final long serialVersionUID = 1L;

    public UpoVerificationException(String message) {
        super(message);
    }

    public UpoVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
