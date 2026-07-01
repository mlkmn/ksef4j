package io.github.mlkmn.ksef4j.error;

/** Token rejected, or one of the auth-flow steps (challenge / submit / redeem) failed. */
public final class KsefAuthenticationException extends KsefException {

    private static final long serialVersionUID = 1L;

    public KsefAuthenticationException(String message) {
        super(message);
    }

    public KsefAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
