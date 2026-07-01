package io.github.mlkmn.ksef4j.internal.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.Environment;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class ClasspathKeyResolverTest {

  private final ClasspathKeyResolver resolver = new ClasspathKeyResolver();

  @Test
  void resolves_test_environment_to_rsa_public_key() {
    PublicKey key = resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);

    assertThat(key).isInstanceOf(RSAPublicKey.class);
    assertThat(key.getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  void caches_resolved_key_per_environment_and_usage() {
    PublicKey first = resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);
    PublicKey second = resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);

    assertThat(second).isSameAs(first);
  }

  @Test
  void resolves_demo_and_prod_for_both_usages() {
    for (Environment env : new Environment[] {Environment.DEMO, Environment.PROD}) {
      PublicKey token = resolver.publicKey(env, KeyUsage.TOKEN_ENCRYPTION);
      PublicKey symmetric = resolver.publicKey(env, KeyUsage.SYMMETRIC_KEY_ENCRYPTION);
      assertThat(token.getAlgorithm()).isEqualTo("RSA");
      assertThat(symmetric.getAlgorithm()).isEqualTo("RSA");
      assertThat(symmetric).isNotEqualTo(token);
    }
  }

  @Test
  void loads_distinct_keys_for_token_and_symmetric_usage() {
    PublicKey token = resolver.publicKey(Environment.TEST, KeyUsage.TOKEN_ENCRYPTION);
    PublicKey symmetric = resolver.publicKey(Environment.TEST, KeyUsage.SYMMETRIC_KEY_ENCRYPTION);

    assertThat(token.getAlgorithm()).isEqualTo("RSA");
    assertThat(symmetric.getAlgorithm()).isEqualTo("RSA");
    assertThat(symmetric).isNotEqualTo(token); // different KSeF certs per usage
  }
}
