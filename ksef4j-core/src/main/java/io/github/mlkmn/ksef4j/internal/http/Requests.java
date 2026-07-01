package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;

import java.util.Base64;

/**
 * Internal: KSeF v2 request wire DTOs. The static factories perform the
 * Base64 mapping from the crypto layer's neutral byte outputs. Not supported API.
 */
public final class Requests {

    private Requests() {
    }

    private static final Base64.Encoder B64 = Base64.getEncoder();

    public record ContextIdentifier(String type, String value) {
    }

    public record KsefTokenAuth(String challenge, ContextIdentifier contextIdentifier, String encryptedToken) {
    }

    public record FormCode(String systemCode, String schemaVersion, String value) {
    }

    public record Encryption(String encryptedSymmetricKey, String initializationVector) {
    }

    public record OpenSession(FormCode formCode, Encryption encryption) {
        public static OpenSession from(byte[] wrappedKey, byte[] iv, FormCode formCode) {
            return new OpenSession(formCode,
                    new Encryption(B64.encodeToString(wrappedKey), B64.encodeToString(iv)));
        }
    }

    public record SendInvoice(
            String invoiceHash,
            long invoiceSize,
            String encryptedInvoiceHash,
            long encryptedInvoiceSize,
            String encryptedInvoiceContent,
            boolean offlineMode) {

        public static SendInvoice from(EncryptedInvoice invoice) {
            return new SendInvoice(
                    B64.encodeToString(invoice.plaintextSha256()),
                    invoice.plaintextSize(),
                    B64.encodeToString(invoice.ciphertextSha256()),
                    invoice.ciphertextSize(),
                    B64.encodeToString(invoice.ciphertext()),
                    false);
        }
    }
}
