package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import java.io.ByteArrayInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link KeyResolver}: reads bundled certificates first (no network) and only fetches fresh
 * certificates from KSeF to refresh a cert that is missing or within {@code refreshSkew} of expiry.
 * Selects by usage, preferring the latest {@code validFrom}; caches per (env, usage). On fetch
 * failure it falls back to the bundled cert as a last resort. Thread-safe.
 */
public final class HttpCertificateResolver implements KeyResolver {

  private static final Logger LOG = System.getLogger(HttpCertificateResolver.class.getName());
  private static final Duration DEFAULT_REFRESH_SKEW = Duration.ofMinutes(2);

  private record CacheKey(Environment environment, KeyUsage usage) {}

  private record CachedCert(PublicKey key, Instant notAfter) {}

  private final HttpTransport transport;
  private final Clock clock;
  private final Duration refreshSkew;
  private final ConcurrentHashMap<CacheKey, CachedCert> cache = new ConcurrentHashMap<>();

  public HttpCertificateResolver(HttpTransport transport, Clock clock) {
    this(transport, clock, DEFAULT_REFRESH_SKEW);
  }

  HttpCertificateResolver(HttpTransport transport, Clock clock, Duration refreshSkew) {
    this.transport = transport;
    this.clock = clock;
    this.refreshSkew = refreshSkew;
  }

  @Override
  public PublicKey publicKey(Environment environment, KeyUsage usage) {
    CacheKey cacheKey = new CacheKey(environment, usage);
    CachedCert cached = cache.get(cacheKey);
    if (cached != null && fresh(cached.notAfter())) {
      return cached.key();
    }

    X509Certificate bundled = tryLoadBundled(environment, usage);
    if (bundled != null && fresh(bundled.getNotAfter().toInstant())) {
      cache.put(
          cacheKey, new CachedCert(bundled.getPublicKey(), bundled.getNotAfter().toInstant()));
      return bundled.getPublicKey();
    }

    LOG.log(
        Level.WARNING,
        "Bundled KSeF {0} {1} certificate is missing or near/past expiry; "
            + "fetching a fresh certificate from KSeF. Update the bundled certificate.",
        environment,
        usage);
    try {
      X509Certificate fetched = selectFetched(environment, usage);
      cache.put(
          cacheKey, new CachedCert(fetched.getPublicKey(), fetched.getNotAfter().toInstant()));
      return fetched.getPublicKey();
    } catch (RuntimeException fetchFailure) {
      if (bundled != null) {
        LOG.log(
            Level.WARNING,
            "KSeF certificate fetch failed for {0} {1}; falling back to an "
                + "expired/near-expiry bundled certificate. Operations may fail until refreshed.",
            environment,
            usage);
        return bundled.getPublicKey();
      }
      throw new KsefAuthenticationException(
          "No KSeF certificate available for "
              + environment
              + " "
              + usage
              + " (no bundle, fetch failed)",
          fetchFailure);
    }
  }

  private boolean fresh(Instant notAfter) {
    return notAfter.isAfter(clock.instant().plus(refreshSkew));
  }

  private X509Certificate tryLoadBundled(Environment environment, KeyUsage usage) {
    try {
      return BundledCertificates.load(environment, usage);
    } catch (KsefAuthenticationException missing) {
      return null;
    }
  }

  private X509Certificate selectFetched(Environment environment, KeyUsage usage) {
    String wanted = ksefUsage(usage);
    Instant now = clock.instant();
    Optional<X509Certificate> best =
        transport.fetchCertificates().stream()
            .filter(info -> info.usage() != null && info.usage().contains(wanted))
            .map(info -> parse(info.certificate()))
            .filter(
                cert ->
                    !now.isBefore(cert.getNotBefore().toInstant())
                        && now.isBefore(cert.getNotAfter().toInstant()))
            .max(Comparator.comparing(cert -> cert.getNotBefore().toInstant()));
    return best.orElseThrow(
        () ->
            new KsefAuthenticationException(
                "KSeF published no valid " + wanted + " certificate for " + environment));
  }

  private static String ksefUsage(KeyUsage usage) {
    return switch (usage) {
      case TOKEN_ENCRYPTION -> "KsefTokenEncryption";
      case SYMMETRIC_KEY_ENCRYPTION -> "SymmetricKeyEncryption";
    };
  }

  private static X509Certificate parse(String base64) {
    try {
      byte[] der = Base64.getDecoder().decode(base64);
      return (X509Certificate)
          CertificateFactory.getInstance("X.509")
              .generateCertificate(new ByteArrayInputStream(der));
    } catch (Exception e) {
      throw new KsefAuthenticationException("Failed to parse a fetched KSeF certificate", e);
    }
  }
}
