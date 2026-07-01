package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.error.KsefException;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Scriptable fake HttpTransport for AuthSession tests. Auth methods only; session methods are unused. */
final class FakeAuthTransport implements HttpTransport {

    Responses.Challenge challenge =
            new Responses.Challenge("CHAL", "2026-06-28T10:00:00Z", 1717000000000L, "1.2.3.4");
    Responses.AuthSubmit submit =
            new Responses.AuthSubmit(new Responses.Token("AUTHJWT", "2026-06-28T10:05:00Z"), "REF1");
    final Deque<Integer> statusCodes = new ArrayDeque<>(); // consumed in order; last value repeats; empty -> 200
    Responses.TokenPair tokenPair;
    Responses.AccessToken refreshResponse;
    KsefException refreshError;

    volatile String lastEncryptedToken;
    final AtomicInteger fetchChallengeCount = new AtomicInteger();
    final AtomicInteger submitCount = new AtomicInteger();
    final AtomicInteger pollCount = new AtomicInteger();
    final AtomicInteger redeemCount = new AtomicInteger();
    final AtomicInteger refreshCount = new AtomicInteger();

    @Override
    public Responses.Challenge fetchChallenge() {
        fetchChallengeCount.incrementAndGet();
        return challenge;
    }

    @Override
    public Responses.AuthSubmit submitKsefTokenAuth(String challengeValue, String nip, String encryptedTokenBase64) {
        submitCount.incrementAndGet();
        lastEncryptedToken = encryptedTokenBase64;
        return submit;
    }

    @Override
    public Responses.AuthStatus pollAuthStatus(String referenceNumber, String authToken) {
        pollCount.incrementAndGet();
        int code = statusCodes.size() > 1 ? statusCodes.poll()
                : (statusCodes.isEmpty() ? 200 : statusCodes.peek());
        return new Responses.AuthStatus(new Responses.Status(code, "", List.of()));
    }

    @Override
    public Responses.TokenPair redeemTokens(String authToken) {
        redeemCount.incrementAndGet();
        return tokenPair;
    }

    @Override
    public Responses.AccessToken refreshToken(String refreshToken) {
        refreshCount.incrementAndGet();
        if (refreshError != null) {
            throw refreshError;
        }
        return refreshResponse;
    }

    @Override
    public Responses.InvoiceStatus fetchInvoiceStatus(String sessionRef, String invoiceRef, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.OpenSession openSession(Requests.OpenSession request, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.SendInvoice sendInvoice(String sessionRef, Requests.SendInvoice request, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.SessionStatus fetchSessionStatus(String sessionRef, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeSession(String sessionRef, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] fetchUpo(URI downloadUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Responses.CertificateInfo> fetchCertificates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.QueryMetadata queryInvoiceMetadata(
            Requests.QueryMetadata filter, int pageOffset, int pageSize, String accessToken) {
        throw new UnsupportedOperationException();
    }
}
