package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.internal.auth.KeyResolver;
import io.github.mlkmn.ksef4j.internal.auth.KeyUsage;
import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;
import io.github.mlkmn.ksef4j.internal.crypto.SessionKey;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;

import java.security.SecureRandom;

/**
 * Internal: opens a KSeF interactive session, sends one encrypted FA(3) invoice,
 * and closes the session (close is the UPO prerequisite). Stateless; safe to share.
 */
public final class DefaultInteractiveSession implements InteractiveSession {

    private static final Requests.FormCode FA3_FORM_CODE =
            new Requests.FormCode("FA (3)", "1-0E", "FA");

    private final HttpTransport transport;
    private final KeyResolver keyResolver;
    private final Environment environment;
    private final SecureRandom random;

    public DefaultInteractiveSession(HttpTransport transport, KeyResolver keyResolver,
                                     Environment environment, SecureRandom random) {
        this.transport = transport;
        this.keyResolver = keyResolver;
        this.environment = environment;
        this.random = random;
    }

    @Override
    public SendReceipt send(byte[] fa3Xml, String accessToken) {
        SessionKey key = SessionKey.generate(random);
        byte[] wrappedKey = key.wrapKey(
                keyResolver.publicKey(environment, KeyUsage.SYMMETRIC_KEY_ENCRYPTION));
        Requests.OpenSession openRequest = Requests.OpenSession.from(wrappedKey, key.iv(), FA3_FORM_CODE);
        Responses.OpenSession opened = transport.openSession(openRequest, accessToken);
        String sessionRef = opened.referenceNumber();

        String invoiceRef;
        try {
            EncryptedInvoice encrypted = key.encrypt(fa3Xml);
            invoiceRef = transport.sendInvoice(sessionRef, Requests.SendInvoice.from(encrypted), accessToken)
                    .referenceNumber();
        } catch (RuntimeException e) {
            closeQuietly(sessionRef, accessToken);
            throw e;
        }

        transport.closeSession(sessionRef, accessToken);
        return new SendReceipt(sessionRef, invoiceRef);
    }

    private void closeQuietly(String sessionRef, String accessToken) {
        try {
            transport.closeSession(sessionRef, accessToken);
        } catch (RuntimeException ignored) {
            // Best-effort cleanup of the opened session; surface the original send failure instead.
        }
    }
}
