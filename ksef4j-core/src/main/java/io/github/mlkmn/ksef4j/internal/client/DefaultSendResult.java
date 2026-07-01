package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.error.ArchiveException;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import io.github.mlkmn.ksef4j.internal.session.UpoPoller;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal: one in-flight send. Polls for the UPO on first {@link #awaitUpo()} (caching it),
 * archives the completed record best-effort, and guards against use after {@link #close()}.
 * Not thread-safe; single-caller use within try-with-resources.
 */
final class DefaultSendResult implements SendResult {

    private static final Logger LOG = System.getLogger(DefaultSendResult.class.getName());

    private final UpoPoller poller;
    private final InvoiceArchive archive;
    private final String sessionReferenceNumber;
    private final String invoiceReferenceNumber;
    private final String accessToken;
    private final String issuerNip;
    private final byte[] fa3Xml;
    private final Instant sentAt;
    private final Duration upoPollTimeout;
    private final UpoSignatureCheck signatureCheck;

    private Upo cachedUpo;
    private boolean closed;

    DefaultSendResult(UpoPoller poller, InvoiceArchive archive,
                      String sessionReferenceNumber, String invoiceReferenceNumber,
                      String accessToken, String issuerNip, byte[] fa3Xml, Instant sentAt,
                      Duration upoPollTimeout, UpoSignatureCheck signatureCheck) {
        this.poller = poller;
        this.archive = archive;
        this.sessionReferenceNumber = sessionReferenceNumber;
        this.invoiceReferenceNumber = invoiceReferenceNumber;
        this.accessToken = accessToken;
        this.issuerNip = issuerNip;
        this.fa3Xml = fa3Xml;
        this.sentAt = sentAt;
        this.upoPollTimeout = upoPollTimeout;
        this.signatureCheck = signatureCheck;
    }

    @Override
    public String invoiceReferenceNumber() {
        return invoiceReferenceNumber;
    }

    @Override
    public Upo awaitUpo() {
        if (closed) {
            throw new IllegalStateException("SendResult is closed");
        }
        if (cachedUpo == null) {
            Upo upo = poller.pollUntilSettled(sessionReferenceNumber, invoiceReferenceNumber, accessToken, upoPollTimeout);
            verifyDocumentHash(upo);
            if (signatureCheck != null) {
                signatureCheck.check(upo.xml());
            }
            cachedUpo = upo;
            archive(upo);
        }
        return cachedUpo;
    }

    private void verifyDocumentHash(Upo upo) {
        String declared = upo.documentHash();
        if (declared == null) {
            LOG.log(Level.DEBUG, "UPO {0} carried no document hash; skipping integrity check",
                    upo.ksefReferenceNumber());
            return;
        }
        String expected = sha256Base64(fa3Xml);
        if (!expected.equals(declared)) {
            throw new UpoVerificationException("UPO document hash mismatch for invoice "
                    + invoiceReferenceNumber + ": expected " + truncate(expected)
                    + " but UPO carried " + truncate(declared));
        }
    }

    private static String sha256Base64(byte[] data) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String truncate(String hash) {
        return hash.length() <= 12 ? hash : hash.substring(0, 12) + "...";
    }

    private void archive(Upo upo) {
        try {
            archive.store(new ArchiveEntry(
                    new ArchiveKey(upo.ksefReferenceNumber(), issuerNip),
                    sentAt, fa3Xml, upo.xml(), metadataFor(upo)));
        } catch (ArchiveException e) {
            // KSeF already accepted the invoice (we hold the UPO); a local archive failure is
            // non-fatal per the InvoiceArchive contract. Log and still return the UPO.
            LOG.log(Level.ERROR, "Failed to archive invoice " + upo.ksefReferenceNumber(), e);
        }
    }

    private Map<String, String> metadataFor(Upo upo) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("ksefReferenceNumber", upo.ksefReferenceNumber());
        metadata.put("invoiceReferenceNumber", invoiceReferenceNumber);
        if (upo.invoiceNumber() != null) {
            metadata.put("invoiceNumber", upo.invoiceNumber());
        }
        if (upo.documentHash() != null) {
            metadata.put("documentHash", upo.documentHash());
        }
        metadata.put("issuedAt", upo.issuedAt().toString());
        metadata.put("issuerNip", issuerNip);
        return metadata;
    }

    @Override
    public void close() {
        closed = true;
    }
}
