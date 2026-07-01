package io.github.mlkmn.ksef4j.internal.http;

import java.net.URI;
import java.util.List;

/**
 * Internal seam: low-level HTTP calls to KSeF v2. One request / one response per method; no
 * polling, backoff, or orchestration (those live in the session layer). Returns wire DTOs from
 * {@link Responses}.
 */
public interface HttpTransport {

  Responses.Challenge fetchChallenge();

  Responses.AuthSubmit submitKsefTokenAuth(
      String challenge, String nip, String encryptedTokenBase64);

  Responses.AuthStatus pollAuthStatus(String referenceNumber, String authToken);

  Responses.TokenPair redeemTokens(String authToken);

  Responses.AccessToken refreshToken(String refreshToken);

  Responses.OpenSession openSession(Requests.OpenSession request, String accessToken);

  Responses.SendInvoice sendInvoice(
      String sessionRef, Requests.SendInvoice request, String accessToken);

  Responses.SessionStatus fetchSessionStatus(String sessionRef, String accessToken);

  /** Fetches the processing status of a single invoice within a session. */
  Responses.InvoiceStatus fetchInvoiceStatus(
      String sessionReferenceNumber, String invoiceReferenceNumber, String accessToken);

  void closeSession(String sessionRef, String accessToken);

  byte[] fetchUpo(URI downloadUrl);

  /** Fetches the environment's published encryption certificates (public endpoint, no auth). */
  List<Responses.CertificateInfo> fetchCertificates();

  /** Queries invoice metadata; the filter is the JSON body, paging goes in the query string. */
  Responses.QueryMetadata queryInvoiceMetadata(
      Requests.QueryMetadata filter, int pageOffset, int pageSize, String accessToken);

  /**
   * Downloads one invoice's XML by its KSeF reference number. Returns the raw plaintext XML bytes.
   */
  byte[] downloadInvoice(String ksefNumber, String accessToken);
}
