package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.Environment;
import java.security.PublicKey;

/**
 * Internal seam: resolves a KSeF public key for a given environment and usage. The default (Wave
 * A3) reads bundled certificates from the classpath; a future resolver may fetch them dynamically.
 * Test code substitutes a fixture-keypair resolver.
 */
public interface KeyResolver {

  /** Public key for the supplied environment and certificate usage. */
  PublicKey publicKey(Environment environment, KeyUsage usage);
}
