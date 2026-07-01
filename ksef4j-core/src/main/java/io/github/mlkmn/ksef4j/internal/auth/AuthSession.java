package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;

/**
 * Internal seam: provides a valid access token, refreshing as needed.
 * Concurrent callers share one in-flight refresh (locked).
 *
 * <p>Implementation arrives in Wave B1.
 */
public interface AuthSession {

    /**
     * Return a valid access token, refreshing if absent or expired.
     *
     * @throws KsefAuthenticationException if the token cannot be obtained
     */
    String accessToken();
}
