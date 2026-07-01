package io.github.mlkmn.ksef4j.internal.crypto;

import io.github.mlkmn.ksef4j.error.KsefTransportException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Internal: per-session symmetric material (AES-256 key + 128-bit IV). Wraps the key with RSA-OAEP
 * for the open-session call and AES-256-CBC encrypts each invoice. Create via {@link
 * #generate(SecureRandom)}.
 */
public final class SessionKey {

  private static final int AES_KEY_BYTES = 32; // 256-bit
  private static final int IV_BYTES = 16; // 128-bit

  private final byte[] key;
  private final byte[] iv;

  SessionKey(byte[] key, byte[] iv) {
    this.key = key;
    this.iv = iv;
  }

  /** Generates a fresh random AES-256 key and 128-bit IV. */
  public static SessionKey generate(SecureRandom random) {
    byte[] key = new byte[AES_KEY_BYTES];
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(key);
    random.nextBytes(iv);
    return new SessionKey(key, iv);
  }

  /** RSA-OAEP-wraps the AES key for the open-session call. */
  public byte[] wrapKey(PublicKey ksefPublicKey) {
    try {
      return KsefRsa.encrypt(key, ksefPublicKey);
    } catch (GeneralSecurityException e) {
      throw new KsefTransportException("Failed to wrap KSeF session key", e);
    }
  }

  /** AES-256-CBC encrypts one invoice and computes the plaintext/ciphertext hashes and sizes. */
  public EncryptedInvoice encrypt(byte[] invoiceXml) {
    try {
      // PKCS5Padding is byte-identical to PKCS#7 for AES's 16-byte block; do not change.
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
      byte[] ciphertext = cipher.doFinal(invoiceXml);
      return new EncryptedInvoice(
          ciphertext, sha256(invoiceXml), invoiceXml.length, sha256(ciphertext), ciphertext.length);
    } catch (GeneralSecurityException e) {
      throw new KsefTransportException("Failed to encrypt invoice for KSeF", e);
    }
  }

  /** The raw 128-bit initialization vector (defensive copy). */
  public byte[] iv() {
    return iv.clone();
  }

  private static byte[] sha256(byte[] data) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(data);
    } catch (GeneralSecurityException e) {
      throw new KsefTransportException("SHA-256 unavailable", e);
    }
  }
}
