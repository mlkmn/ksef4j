package io.github.mlkmn.ksef4j.internal.crypto;

/**
 * Internal: neutral result of encrypting one invoice. Raw bytes and sizes;
 * the transport layer Base64-encodes the byte arrays for the JSON DTO.
 */
public record EncryptedInvoice(
        byte[] ciphertext,
        byte[] plaintextSha256,
        long plaintextSize,
        byte[] ciphertextSha256,
        long ciphertextSize) {
}
