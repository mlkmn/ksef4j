package io.github.mlkmn.ksef4j.error;

/**
 * Failure reading from or writing to an {@code InvoiceArchive}. Non-fatal during
 * {@code awaitUpo()} (logged at ERROR; the {@code Upo} is still returned).
 */
public final class ArchiveException extends KsefException {

    private static final long serialVersionUID = 1L;

    public ArchiveException(String message) {
        super(message);
    }

    public ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
