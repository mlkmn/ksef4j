package io.github.mlkmn.ksef4j;

/**
 * KSeF deployment environment. Selects the base URL set and the bundled public-key certificate used
 * for token encryption. Endpoint URLs are resolved by {@code EnvironmentEndpoints} (internal,
 * defined by Wave A4).
 */
public enum Environment {
  TEST,
  DEMO,
  PROD
}
