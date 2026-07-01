package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.internal.auth.ClasspathKeyResolver;
import io.github.mlkmn.ksef4j.internal.auth.DefaultAuthSession;
import io.github.mlkmn.ksef4j.internal.http.EnvironmentEndpoints;
import io.github.mlkmn.ksef4j.internal.http.JdkHttpTransport;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MockKsefAuthTest {

  @Test
  void real_auth_session_obtains_an_access_token_against_the_mock() {
    try (MockKsef ksef = MockKsef.create()) {
      JdkHttpTransport transport =
          new JdkHttpTransport(
              EnvironmentEndpoints.ofBaseUri(ksef.baseUrl()),
              Duration.ofSeconds(5),
              Duration.ofSeconds(5));
      DefaultAuthSession auth =
          new DefaultAuthSession(
              transport,
              new ClasspathKeyResolver(),
              Environment.TEST,
              "any-token",
              "5260250274",
              Clock.systemUTC(),
              Duration.ofMillis(50),
              Duration.ofSeconds(5));
      assertThat(auth.accessToken()).isEqualTo("ACC");
    }
  }
}
