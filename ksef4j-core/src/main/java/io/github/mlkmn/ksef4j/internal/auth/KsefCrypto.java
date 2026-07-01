package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.internal.crypto.KsefRsa;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Internal: encrypts the KSeF challenge token under the environment public
 * key, per the locked RSA-OAEP-SHA-256 recipe (delegated to {@link KsefRsa}).
 * Stateless.
 */
final class KsefCrypto {

    private KsefCrypto() {
    }

    /**
     * Encrypts {@code token|timestampMs} with RSA/ECB/OAEPPadding (SHA-256,
     * MGF1-SHA-256) and Base64-encodes the ciphertext.
     */
    static String encryptToken(String token, long timestampMs, PublicKey ksefPublicKey) {
        try {
            byte[] plaintext = "%s|%d".formatted(token, timestampMs).getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = KsefRsa.encrypt(plaintext, ksefPublicKey);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new KsefAuthenticationException("Failed to encrypt KSeF challenge token", e);
        }
    }
}
