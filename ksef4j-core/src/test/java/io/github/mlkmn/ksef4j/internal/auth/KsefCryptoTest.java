package io.github.mlkmn.ksef4j.internal.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import static org.assertj.core.api.Assertions.assertThat;

class KsefCryptoTest {

    @Test
    void encrypt_token_round_trips_with_matching_private_key() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String b64 = KsefCrypto.encryptToken("token-abc", 1717000000000L, pair.getPublic());

        Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec params = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        decrypt.init(Cipher.DECRYPT_MODE, pair.getPrivate(), params);
        byte[] plaintext = decrypt.doFinal(Base64.getDecoder().decode(b64));

        assertThat(new String(plaintext, StandardCharsets.UTF_8)).isEqualTo("token-abc|1717000000000");
    }

    @Test
    void plaintext_layout_is_token_pipe_timestamp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String b64 = KsefCrypto.encryptToken("XYZ", 42L, pair.getPublic());

        Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
        decrypt.init(Cipher.DECRYPT_MODE, pair.getPrivate(), new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        byte[] plaintext = decrypt.doFinal(Base64.getDecoder().decode(b64));

        assertThat(new String(plaintext, StandardCharsets.UTF_8)).isEqualTo("XYZ|42");
    }
}
