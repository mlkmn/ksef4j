package io.github.mlkmn.ksef4j.internal.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.jupiter.api.Test;

class KsefRsaTest {

  @Test
  void encrypt_round_trips_with_matching_private_key() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    KeyPair pair = gen.generateKeyPair();
    byte[] plaintext = "session-key-material".getBytes(StandardCharsets.UTF_8);

    byte[] ciphertext = KsefRsa.encrypt(plaintext, pair.getPublic());

    Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
    decrypt.init(
        Cipher.DECRYPT_MODE,
        pair.getPrivate(),
        new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
    byte[] recovered = decrypt.doFinal(ciphertext);

    assertThat(recovered).isEqualTo(plaintext);
  }
}
