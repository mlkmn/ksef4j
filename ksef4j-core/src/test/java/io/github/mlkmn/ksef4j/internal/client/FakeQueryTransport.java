package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Scriptable fake HttpTransport for client query tests; serves a queue of pages and counts calls.
 */
final class FakeQueryTransport implements HttpTransport {

  final List<Responses.QueryMetadata> pages = new ArrayList<>();
  int queryCalls = 0;
  Integer lastPageOffset;
  Integer lastPageSize;

  @Override
  public Responses.QueryMetadata queryInvoiceMetadata(
      Requests.QueryMetadata filter, int pageOffset, int pageSize, String accessToken) {
    lastPageOffset = pageOffset;
    lastPageSize = pageSize;
    return pages.get(queryCalls++);
  }

  @Override
  public Responses.Challenge fetchChallenge() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.AuthSubmit submitKsefTokenAuth(String c, String n, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.AuthStatus pollAuthStatus(String r, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.TokenPair redeemTokens(String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.AccessToken refreshToken(String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.OpenSession openSession(Requests.OpenSession r, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.SendInvoice sendInvoice(String s, Requests.SendInvoice r, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.SessionStatus fetchSessionStatus(String s, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Responses.InvoiceStatus fetchInvoiceStatus(String s, String i, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeSession(String s, String t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] fetchUpo(URI u) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Responses.CertificateInfo> fetchCertificates() {
    throw new UnsupportedOperationException();
  }
}
