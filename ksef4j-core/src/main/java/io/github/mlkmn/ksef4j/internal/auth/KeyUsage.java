package io.github.mlkmn.ksef4j.internal.auth;

/**
 * The two KSeF v2 public-key certificate purposes, distinguished by the {@code usage} field of
 * {@code GET /security/public-key-certificates}. Same OAEP recipe, different keys.
 */
public enum KeyUsage {
  /** Encrypts the authentication token ({@code encryptedToken}). */
  TOKEN_ENCRYPTION,
  /** Encrypts the AES session key ({@code encryptedSymmetricKey}). */
  SYMMETRIC_KEY_ENCRYPTION
}
