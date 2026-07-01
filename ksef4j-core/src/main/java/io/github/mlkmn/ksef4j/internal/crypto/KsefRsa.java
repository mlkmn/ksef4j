package io.github.mlkmn.ksef4j.internal.crypto;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Internal: RSA-OAEP-SHA-256 encryption of raw bytes - the single home of the KSeF RSA recipe. Used
 * to encrypt the auth challenge token (internal.auth) and to wrap the per-session AES key
 * (internal.crypto). Stateless.
 */
public final class KsefRsa {

  private KsefRsa() {}

  /**
   * RSA/ECB/OAEPPadding (SHA-256, MGF1-SHA-256) encryption of {@code plaintext}. Propagates {@link
   * GeneralSecurityException}; callers wrap it in the KsefException subtype appropriate to their
   * context.
   */
  public static byte[] encrypt(byte[] plaintext, PublicKey ksefPublicKey)
      throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
    OAEPParameterSpec params =
        new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    cipher.init(Cipher.ENCRYPT_MODE, ksefPublicKey, params);
    return cipher.doFinal(plaintext);
  }
}
