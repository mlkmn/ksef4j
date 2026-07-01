package io.github.mlkmn.ksef4j.internal.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class SessionKeyTest {

    private static final byte[] FIXED_KEY = HexFormat.of().parseHex(
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
    private static final byte[] FIXED_IV = HexFormat.of().parseHex(
            "000102030405060708090a0b0c0d0e0f");

    @Test
    void encrypt_round_trips_with_same_key_and_iv() throws Exception {
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);
        byte[] plaintext = "<Faktura>...</Faktura>".getBytes(StandardCharsets.UTF_8);

        EncryptedInvoice enc = sk.encrypt(plaintext);

        Cipher decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(FIXED_KEY, "AES"), new IvParameterSpec(FIXED_IV));
        byte[] recovered = decrypt.doFinal(enc.ciphertext());

        assertThat(recovered).isEqualTo(plaintext);
        assertThat(enc.plaintextSize()).isEqualTo(plaintext.length);
        assertThat(enc.ciphertextSize()).isEqualTo(enc.ciphertext().length);
        assertThat(enc.ciphertext().length % 16).isZero();
        assertThat(enc.ciphertext().length).isGreaterThan(plaintext.length); // padding present
    }

    @Test
    void ciphertext_matches_independent_aes_cbc_pkcs7_encryption() throws Exception {
        // Pins the exact transformation: if SessionKey drifted to a different mode/padding,
        // this independently-configured AES/CBC/PKCS5Padding reference would diverge.
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);
        byte[] plaintext = "<Faktura/>".getBytes(StandardCharsets.UTF_8);

        EncryptedInvoice enc = sk.encrypt(plaintext);

        Cipher reference = Cipher.getInstance("AES/CBC/PKCS5Padding");
        reference.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(FIXED_KEY, "AES"), new IvParameterSpec(FIXED_IV));
        byte[] expected = reference.doFinal(plaintext);

        assertThat(enc.ciphertext()).isEqualTo(expected);
    }

    @Test
    void plaintext_hash_matches_known_sha256_vector() {
        // SHA-256("abc") - standard NIST test vector.
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);
        byte[] plaintext = "abc".getBytes(StandardCharsets.US_ASCII);

        EncryptedInvoice enc = sk.encrypt(plaintext);

        assertThat(HexFormat.of().formatHex(enc.plaintextSha256()))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void ciphertext_hash_is_sha256_of_ciphertext() throws Exception {
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);
        byte[] plaintext = "invoice-bytes".getBytes(StandardCharsets.UTF_8);

        EncryptedInvoice enc = sk.encrypt(plaintext);

        byte[] expected = MessageDigest.getInstance("SHA-256").digest(enc.ciphertext());
        assertThat(enc.ciphertextSha256()).isEqualTo(expected);
    }

    @Test
    void wrap_key_round_trips_with_matching_private_key() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);

        byte[] wrapped = sk.wrapKey(pair.getPublic());

        Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
        decrypt.init(Cipher.DECRYPT_MODE, pair.getPrivate(), new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        byte[] unwrapped = decrypt.doFinal(wrapped);

        assertThat(unwrapped).isEqualTo(FIXED_KEY);
    }

    @Test
    void generate_uses_fresh_random_material_each_time() {
        byte[] plaintext = "same-invoice".getBytes(StandardCharsets.UTF_8);

        byte[] c1 = SessionKey.generate(new SecureRandom()).encrypt(plaintext).ciphertext();
        byte[] c2 = SessionKey.generate(new SecureRandom()).encrypt(plaintext).ciphertext();

        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void iv_returns_a_defensive_copy() {
        SessionKey sk = new SessionKey(FIXED_KEY, FIXED_IV);

        assertThat(sk.iv()).isEqualTo(FIXED_IV);

        sk.iv()[0]++; // mutating the returned array must not change the stored IV
        assertThat(sk.iv()).isEqualTo(FIXED_IV);
    }
}
