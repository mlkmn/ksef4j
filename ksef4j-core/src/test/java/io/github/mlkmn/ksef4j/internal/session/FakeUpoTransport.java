package io.github.mlkmn.ksef4j.internal.session;

import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Scriptable fake HttpTransport for UpoPoller tests (session-status + UPO fetch only). */
final class FakeUpoTransport implements HttpTransport {

    final Deque<Responses.SessionStatus> statuses = new ArrayDeque<>(); // consumed in order; last repeats
    byte[] upoXml;
    final AtomicInteger sessionStatusCount = new AtomicInteger();
    volatile URI lastUpoUrl;
    final Deque<Responses.InvoiceStatus> invoiceStatuses = new ArrayDeque<>(); // consumed in order; last repeats
    final AtomicInteger invoiceStatusCount = new AtomicInteger();

    static Responses.SessionStatus inProgress() {
        return status(100, null);
    }

    static Responses.SessionStatus successNoUpo() {
        return status(200, null);
    }

    static Responses.SessionStatus processing(int code) {
        return status(code, null);
    }

    static Responses.SessionStatus failure(int code) {
        return status(code, null);
    }

    static Responses.SessionStatus ready(String pageRef, String downloadUrl) {
        return status(200, new Responses.Upo(List.of(new Responses.UpoPage(pageRef, downloadUrl, null))));
    }

    private static Responses.SessionStatus status(int code, Responses.Upo upo) {
        return new Responses.SessionStatus(
                new Responses.Status(code, "", List.of()), 1, 1, 0, null, null, null, upo);
    }

    static Responses.InvoiceStatus invoiceStatus(int code) {
        return invoiceStatus(code, "");
    }

    static Responses.InvoiceStatus invoiceStatus(int code, String description) {
        return new Responses.InvoiceStatus(new Responses.Status(code, description, List.of()));
    }

    @Override
    public Responses.SessionStatus fetchSessionStatus(String sessionRef, String accessToken) {
        sessionStatusCount.incrementAndGet();
        return statuses.size() > 1 ? statuses.poll() : statuses.peek();
    }

    @Override
    public Responses.InvoiceStatus fetchInvoiceStatus(String sessionRef, String invoiceRef, String accessToken) {
        invoiceStatusCount.incrementAndGet();
        return invoiceStatuses.size() > 1 ? invoiceStatuses.poll() : invoiceStatuses.peek();
    }

    @Override
    public byte[] fetchUpo(URI downloadUrl) {
        lastUpoUrl = downloadUrl;
        return upoXml;
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
    public Responses.OpenSession openSession(Requests.OpenSession request, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Responses.SendInvoice sendInvoice(String sessionRef, Requests.SendInvoice request, String accessToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeSession(String sessionRef, String accessToken) {
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
