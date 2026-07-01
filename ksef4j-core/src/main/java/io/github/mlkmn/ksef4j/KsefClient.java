package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.internal.archive.FilesystemInvoiceArchive;
import io.github.mlkmn.ksef4j.internal.archive.NoOpInvoiceArchive;
import io.github.mlkmn.ksef4j.internal.auth.AuthSession;
import io.github.mlkmn.ksef4j.internal.auth.DefaultAuthSession;
import io.github.mlkmn.ksef4j.internal.auth.HttpCertificateResolver;
import io.github.mlkmn.ksef4j.internal.auth.KeyResolver;
import io.github.mlkmn.ksef4j.internal.client.DefaultKsefClient;
import io.github.mlkmn.ksef4j.internal.client.UpoSignatureCheck;
import io.github.mlkmn.ksef4j.internal.http.EnvironmentEndpoints;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.JdkHttpTransport;
import io.github.mlkmn.ksef4j.internal.session.DefaultInteractiveSession;
import io.github.mlkmn.ksef4j.internal.session.DefaultUpoPoller;
import io.github.mlkmn.ksef4j.internal.session.ExponentialBackoff;
import io.github.mlkmn.ksef4j.internal.session.InteractiveSession;
import io.github.mlkmn.ksef4j.internal.session.UpoPoller;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * High-level KSeF client. Construct via {@link #builder()}. Thread-safe; intended to be used as a
 * singleton (Spring bean, manual cache, etc.).
 */
public interface KsefClient {

  /**
   * Send a single invoice via the interactive session API.
   *
   * @return a {@link SendResult} handle; the caller should use try-with-resources
   */
  SendResult send(Invoice invoice);

  /**
   * Fetch one page of invoice metadata matching {@code query}.
   *
   * @return the page (its {@code invoices()} may be empty; check {@code hasMore()} for more pages)
   */
  InvoiceMetadataPage queryInvoices(InvoiceQuery query);

  /**
   * Lazily stream all invoice metadata matching {@code query}, fetching successive pages on demand.
   * Each page is one network call; a remote failure surfaces during terminal iteration.
   */
  Stream<InvoiceMetadata> streamInvoices(InvoiceQuery query);

  /**
   * Download the FA(3) XML of an invoice by its KSeF reference number (e.g. from {@link
   * InvoiceMetadata#ksefNumber()}). Returns the raw plaintext XML bytes. Requires the token's
   * {@code InvoiceRead} permission.
   */
  byte[] downloadInvoice(String ksefNumber);

  /** Entry point for configuring a new client. */
  static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link KsefClient}. */
  final class Builder {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_UPO_POLL_TIMEOUT = Duration.ofSeconds(180);
    private static final Duration AUTH_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(30);

    private Environment environment;
    private URI baseUrl;
    private String token;
    private String nip;
    private Duration httpConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration httpRequestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private Duration upoPollTimeout = DEFAULT_UPO_POLL_TIMEOUT;
    private Path archiveDirectory;
    private InvoiceArchive archive;
    private boolean verifyUpoSignature;

    private Builder() {
      // Use KsefClient.builder()
    }

    public Builder environment(Environment environment) {
      this.environment = environment;
      return this;
    }

    public Builder tokenAuth(String token, String nip) {
      this.token = token;
      this.nip = nip;
      return this;
    }

    /**
     * Advanced: send all KSeF traffic to {@code baseUrl} instead of the {@link Environment}'s
     * default host -- e.g. a reverse proxy or API gateway that fronts the selected environment, or
     * an in-process simulator. The environment still selects the encryption certificate, so this is
     * not a way to reach a different KSeF instance. Credentials and encrypted invoices flow over
     * this URL; point it only at a trusted endpoint. Defaults to the environment host.
     */
    public Builder baseUrl(URI baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder httpConnectTimeout(Duration timeout) {
      this.httpConnectTimeout = timeout;
      return this;
    }

    public Builder httpRequestTimeout(Duration timeout) {
      this.httpRequestTimeout = timeout;
      return this;
    }

    /** Maximum time {@link SendResult#awaitUpo()} waits for the UPO. Default 180s. */
    public Builder upoPollTimeout(Duration timeout) {
      this.upoPollTimeout = timeout;
      return this;
    }

    /**
     * Archive sent invoices to a directory tree (convenience for a {@link
     * FilesystemInvoiceArchive}).
     */
    public Builder archiveDirectory(Path directory) {
      this.archiveDirectory = directory;
      return this;
    }

    /**
     * Archive sent invoices through a custom {@link InvoiceArchive}. Mutually exclusive with {@link
     * #archiveDirectory(Path)}.
     */
    public Builder archive(InvoiceArchive archive) {
      this.archive = archive;
      return this;
    }

    /**
     * Also verify each UPO's Ministry signature during {@link SendResult#awaitUpo()}. Default off.
     * Requires a bundled signing certificate for the environment (TEST, DEMO and PROD are all
     * bundled). Throws {@link io.github.mlkmn.ksef4j.error.UpoVerificationException} on a bad
     * signature. The standalone {@link UpoSignatureVerifier} can verify a UPO independently.
     */
    public Builder verifyUpoSignature(boolean verify) {
      this.verifyUpoSignature = verify;
      return this;
    }

    /**
     * Build the client from the configured options.
     *
     * @throws IllegalStateException if {@code environment} was not set, if {@code tokenAuth(token,
     *     nip)} was not set (or token/nip is blank), or if both {@code archive(...)} and {@code
     *     archiveDirectory(...)} were set
     */
    public KsefClient build() {
      if (environment == null) {
        throw new IllegalStateException("environment is required");
      }
      if (token == null || token.isBlank() || nip == null || nip.isBlank()) {
        throw new IllegalStateException("tokenAuth(token, nip) is required");
      }
      if (archive != null && archiveDirectory != null) {
        throw new IllegalStateException(
            "Set either archive(...) or archiveDirectory(...), not both");
      }

      EnvironmentEndpoints endpoints =
          (baseUrl != null)
              ? EnvironmentEndpoints.ofBaseUri(baseUrl)
              : EnvironmentEndpoints.of(environment);
      HttpTransport transport =
          new JdkHttpTransport(endpoints, httpConnectTimeout, httpRequestTimeout);
      Clock clock = Clock.systemUTC();
      KeyResolver keyResolver = new HttpCertificateResolver(transport, clock);

      AuthSession auth =
          new DefaultAuthSession(
              transport,
              keyResolver,
              environment,
              token,
              nip,
              clock,
              AUTH_POLL_INTERVAL,
              AUTH_TIMEOUT);
      InteractiveSession session =
          new DefaultInteractiveSession(transport, keyResolver, environment, new SecureRandom());
      UpoPoller poller = new DefaultUpoPoller(transport, new ExponentialBackoff(), clock);

      UpoSignatureCheck signatureCheck =
          verifyUpoSignature ? xml -> new UpoSignatureVerifier().verify(xml, environment) : null;

      return new DefaultKsefClient(
          auth,
          session,
          poller,
          resolveArchive(),
          clock,
          upoPollTimeout,
          signatureCheck,
          transport);
    }

    private InvoiceArchive resolveArchive() {
      if (archive != null) {
        return archive;
      }
      if (archiveDirectory != null) {
        return new FilesystemInvoiceArchive(archiveDirectory);
      }
      return new NoOpInvoiceArchive();
    }
  }
}
