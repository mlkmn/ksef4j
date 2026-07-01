package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.Environment;
import java.net.URI;
import java.util.Objects;

/**
 * Internal: resolves KSeF v2 base URLs per environment and builds the full request URI for each
 * endpoint. {@link #ofBaseUri(URI)} is the testability seam (point at an in-process server).
 */
public final class EnvironmentEndpoints {

  private final String base; // never ends with '/'

  private EnvironmentEndpoints(String base) {
    this.base = base.replaceAll("/+$", "");
  }

  public static EnvironmentEndpoints of(Environment environment) {
    return new EnvironmentEndpoints(
        switch (environment) {
          case TEST -> "https://api-test.ksef.mf.gov.pl/v2";
          case DEMO -> "https://api-demo.ksef.mf.gov.pl/v2";
          case PROD -> "https://api.ksef.mf.gov.pl/v2";
        });
  }

  public static EnvironmentEndpoints ofBaseUri(URI baseUri) {
    Objects.requireNonNull(baseUri, "baseUri");
    return new EnvironmentEndpoints(baseUri.toString());
  }

  public URI challenge() {
    return URI.create(base + "/auth/challenge");
  }

  public URI ksefTokenAuth() {
    return URI.create(base + "/auth/ksef-token");
  }

  public URI authStatus(String ref) {
    return URI.create(base + "/auth/" + ref);
  }

  public URI tokenRedeem() {
    return URI.create(base + "/auth/token/redeem");
  }

  public URI tokenRefresh() {
    return URI.create(base + "/auth/token/refresh");
  }

  public URI openSession() {
    return URI.create(base + "/sessions/online");
  }

  public URI sendInvoice(String sessionRef) {
    return URI.create(base + "/sessions/online/" + sessionRef + "/invoices");
  }

  public URI sessionStatus(String sessionRef) {
    return URI.create(base + "/sessions/" + sessionRef);
  }

  public URI invoiceStatus(String sessionRef, String invoiceRef) {
    return URI.create(base + "/sessions/" + sessionRef + "/invoices/" + invoiceRef);
  }

  public URI closeSession(String sessionRef) {
    return URI.create(base + "/sessions/online/" + sessionRef + "/close");
  }

  public URI certificates() {
    return URI.create(base + "/security/public-key-certificates");
  }

  public URI queryInvoiceMetadata(int pageOffset, int pageSize) {
    return URI.create(
        base + "/invoices/query/metadata?pageOffset=" + pageOffset + "&pageSize=" + pageSize);
  }

  public URI downloadInvoice(String ksefNumber) {
    return URI.create(base + "/invoices/ksef/" + ksefNumber);
  }
}
