package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Scriptable fake HttpTransport for InteractiveSession tests (open/send/close only). */
final class FakeInteractiveTransport implements HttpTransport {

    final List<String> calls = new ArrayList<>();
    final List<Requests.OpenSession> openRequests = new ArrayList<>();
    Requests.OpenSession lastOpenRequest;
    Requests.SendInvoice lastSendRequest;
    String closedSessionRef;
    int closeCount = 0;

    String openSessionRef = "SESS1";
    String invoiceRef = "INV1";
    RuntimeException sendError;
    RuntimeException closeError;
    RuntimeException openError;
    String sentSessionRef;

    @Override
    public Responses.OpenSession openSession(Requests.OpenSession request, String accessToken) {
        calls.add("open");
        lastOpenRequest = request;
        openRequests.add(request);
        if (openError != null) {
            throw openError;
        }
        return new Responses.OpenSession(openSessionRef, "2026-06-28T22:00:00Z");
    }

    @Override
    public Responses.SendInvoice sendInvoice(String sessionRef, Requests.SendInvoice request, String accessToken) {
        calls.add("send");
        sentSessionRef = sessionRef;
        lastSendRequest = request;
        if (sendError != null) {
            throw sendError;
        }
        return new Responses.SendInvoice(invoiceRef);
    }

    @Override
    public void closeSession(String sessionRef, String accessToken) {
        calls.add("close");
        closeCount++;
        closedSessionRef = sessionRef;
        if (closeError != null) {
            throw closeError;
        }
    }

    @Override
    public Responses.Challenge fetchChallenge() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.AuthSubmit submitKsefTokenAuth(String challenge, String nip, String encryptedTokenBase64) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.AuthStatus pollAuthStatus(String referenceNumber, String authToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.TokenPair redeemTokens(String authToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.AccessToken refreshToken(String refreshToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.SessionStatus fetchSessionStatus(String sessionRef, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.InvoiceStatus fetchInvoiceStatus(String sessionRef, String invoiceRef, String accessToken) {
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
}
