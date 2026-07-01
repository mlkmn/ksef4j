package io.github.mlkmn.ksef4j.internal.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** JDK HttpClient implementation of {@link HttpTransport} for KSeF v2. */
public final class JdkHttpTransport implements HttpTransport {

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

  private final EnvironmentEndpoints endpoints;
  private final Duration requestTimeout;
  private final HttpClient httpClient;
  private final ObjectMapper mapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  public JdkHttpTransport(
      EnvironmentEndpoints endpoints, Duration connectTimeout, Duration requestTimeout) {
    this.endpoints = endpoints;
    this.requestTimeout = requestTimeout;
    this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
  }

  public JdkHttpTransport(Environment environment) {
    this(EnvironmentEndpoints.of(environment), DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
  }

  @Override
  public Responses.Challenge fetchChallenge() {
    return sendForJson(
        request(endpoints.challenge(), null).POST(BodyPublishers.noBody()).build(),
        Responses.Challenge.class);
  }

  @Override
  public Responses.AuthSubmit submitKsefTokenAuth(
      String challenge, String nip, String encryptedTokenBase64) {
    Requests.KsefTokenAuth body =
        new Requests.KsefTokenAuth(
            challenge, new Requests.ContextIdentifier("nip", nip), encryptedTokenBase64);
    return sendForJson(jsonPost(endpoints.ksefTokenAuth(), body, null), Responses.AuthSubmit.class);
  }

  @Override
  public Responses.AuthStatus pollAuthStatus(String referenceNumber, String authToken) {
    return sendForJson(
        request(endpoints.authStatus(referenceNumber), authToken).GET().build(),
        Responses.AuthStatus.class);
  }

  @Override
  public Responses.TokenPair redeemTokens(String authToken) {
    return sendForJson(
        request(endpoints.tokenRedeem(), authToken).POST(BodyPublishers.noBody()).build(),
        Responses.TokenPair.class);
  }

  @Override
  public Responses.AccessToken refreshToken(String refreshToken) {
    return sendForJson(
        request(endpoints.tokenRefresh(), refreshToken).POST(BodyPublishers.noBody()).build(),
        Responses.AccessToken.class);
  }

  @Override
  public Responses.OpenSession openSession(Requests.OpenSession request, String accessToken) {
    return sendForJson(
        jsonPost(endpoints.openSession(), request, accessToken), Responses.OpenSession.class);
  }

  @Override
  public Responses.SendInvoice sendInvoice(
      String sessionRef, Requests.SendInvoice request, String accessToken) {
    return sendForJson(
        jsonPost(endpoints.sendInvoice(sessionRef), request, accessToken),
        Responses.SendInvoice.class);
  }

  @Override
  public Responses.SessionStatus fetchSessionStatus(String sessionRef, String accessToken) {
    return sendForJson(
        request(endpoints.sessionStatus(sessionRef), accessToken).GET().build(),
        Responses.SessionStatus.class);
  }

  @Override
  public Responses.InvoiceStatus fetchInvoiceStatus(
      String sessionReferenceNumber, String invoiceReferenceNumber, String accessToken) {
    return sendForJson(
        request(
                endpoints.invoiceStatus(sessionReferenceNumber, invoiceReferenceNumber),
                accessToken)
            .GET()
            .build(),
        Responses.InvoiceStatus.class);
  }

  @Override
  public void closeSession(String sessionRef, String accessToken) {
    // Response body shape is unconfirmed (200 vs 204); any 2xx is success.
    // Verify against the live KSeF test environment in the deferred smoke test.
    sendExpectingSuccess(
        request(endpoints.closeSession(sessionRef), accessToken)
            .POST(BodyPublishers.noBody())
            .build());
  }

  @Override
  public byte[] fetchUpo(URI downloadUrl) {
    // Pre-signed URL is self-authenticating; no Authorization header sent.
    // Verify in the deferred smoke test.
    return sendForBytes(request(downloadUrl, null).GET().build());
  }

  @Override
  public List<Responses.CertificateInfo> fetchCertificates() {
    Responses.CertificateInfo[] certs =
        sendForJson(
            request(endpoints.certificates(), null).GET().build(),
            Responses.CertificateInfo[].class);
    return List.of(certs);
  }

  @Override
  public Responses.QueryMetadata queryInvoiceMetadata(
      Requests.QueryMetadata filter, int pageOffset, int pageSize, String accessToken) {
    return sendForJson(
        jsonPost(endpoints.queryInvoiceMetadata(pageOffset, pageSize), filter, accessToken),
        Responses.QueryMetadata.class);
  }

  @Override
  public byte[] downloadInvoice(String ksefNumber, String accessToken) {
    return sendForBytes(
        request(endpoints.downloadInvoice(ksefNumber), accessToken)
            .header("Accept", "application/xml")
            .GET()
            .build());
  }

  // --- shared helpers (reused by the session methods) ---

  HttpRequest.Builder request(URI uri, String bearerToken) {
    HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(requestTimeout);
    if (bearerToken != null) {
      b.header("Authorization", "Bearer " + bearerToken);
    }
    return b;
  }

  HttpRequest jsonPost(URI uri, Object body, String bearerToken) {
    return request(uri, bearerToken)
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofByteArray(toJson(body)))
        .build();
  }

  <T> T sendForJson(HttpRequest request, Class<T> type) {
    HttpResponse<byte[]> response = execute(request);
    if (isSuccess(response)) {
      try {
        return mapper.readValue(response.body(), type);
      } catch (IOException e) {
        throw new KsefTransportException("Failed to parse KSeF response from " + request.uri(), e);
      }
    }
    throw errorFor(response, request.uri());
  }

  void sendExpectingSuccess(HttpRequest request) {
    HttpResponse<byte[]> response = execute(request);
    if (!isSuccess(response)) {
      throw errorFor(response, request.uri());
    }
  }

  byte[] sendForBytes(HttpRequest request) {
    HttpResponse<byte[]> response = execute(request);
    if (isSuccess(response)) {
      return response.body();
    }
    throw errorFor(response, request.uri());
  }

  private HttpResponse<byte[]> execute(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new KsefTransportException("HTTP call to KSeF failed: " + request.uri(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new KsefTransportException("HTTP call to KSeF interrupted: " + request.uri(), e);
    }
  }

  private byte[] toJson(Object body) {
    try {
      return mapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      throw new KsefTransportException("Failed to serialize KSeF request body", e);
    }
  }

  private static boolean isSuccess(HttpResponse<?> response) {
    int s = response.statusCode();
    return s >= 200 && s < 300;
  }

  KsefException errorFor(HttpResponse<byte[]> response, URI uri) {
    int status = response.statusCode();
    ErrorDetail detail = extractErrorDetail(response.body());
    if (status == 401 || status == 403) {
      return new KsefAuthenticationException(
          "KSeF authentication failed (HTTP " + status + "): " + detail.summary());
    }
    if (status >= 400 && status < 500) {
      String retry =
          response.headers().firstValue("Retry-After").map(r -> "; Retry-After=" + r).orElse("");
      String message =
          "KSeF rejected the request (HTTP " + status + "): " + detail.summary() + retry;
      return new KsefBusinessException(detail.exceptionCode(), status, message);
    }
    return new KsefTransportException(
        "KSeF transport error (HTTP " + status + "): " + detail.summary());
  }

  private record ErrorDetail(String exceptionCode, String serviceCode, String summary) {}

  private ErrorDetail extractErrorDetail(byte[] body) {
    try {
      JsonNode exception = mapper.readTree(body).path("exception");
      JsonNode first = exception.path("exceptionDetailList").path(0);
      String code = first.path("exceptionCode").asText("");
      String description = first.path("exceptionDescription").asText("");
      String serviceCode = exception.path("serviceCode").asText("");
      if (!description.isEmpty() || !code.isEmpty()) {
        String summary =
            "code="
                + code
                + " "
                + description
                + (serviceCode.isEmpty() ? "" : " [serviceCode=" + serviceCode + "]");
        return new ErrorDetail(code, serviceCode, summary);
      }
    } catch (IOException | RuntimeException ignored) {
      // best-effort: fall through to raw snippet
    }
    String raw = new String(body, StandardCharsets.UTF_8);
    String snippet = raw.length() > 200 ? raw.substring(0, 200) : raw;
    return new ErrorDetail("", "", snippet);
  }
}
