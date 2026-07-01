package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.Environment;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default offline {@link KeyResolver}: returns the public key of a bundled per-environment,
 * per-usage X.509 certificate ({@code /keys/<env>-<usage>.pem}), cached. v0.1 bundles TEST, DEMO
 * and PROD. Thread-safe; shared across concurrent sends.
 */
public final class ClasspathKeyResolver implements KeyResolver {

  private record CacheKey(Environment environment, KeyUsage usage) {}

  private final ConcurrentHashMap<CacheKey, PublicKey> cache = new ConcurrentHashMap<>();

  @Override
  public PublicKey publicKey(Environment environment, KeyUsage usage) {
    return cache.computeIfAbsent(
        new CacheKey(environment, usage),
        k -> BundledCertificates.load(k.environment(), k.usage()).getPublicKey());
  }
}
