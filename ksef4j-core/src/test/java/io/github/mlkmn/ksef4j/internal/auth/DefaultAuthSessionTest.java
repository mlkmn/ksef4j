package io.github.mlkmn.ksef4j.internal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.KsefTransportException;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultAuthSessionTest {

  private static final Instant START = Instant.parse("2026-06-28T10:00:00Z");
  private static final String NIP = "5260250274";
  private static final String KSEF_TOKEN = "secret-token";

  private MutableClock clock;
  private FakeAuthTransport transport;
  private PublicKey publicKey;
  private PrivateKey privateKey;
  private KeyResolver keyResolver;

  @BeforeEach
  void setUp() throws Exception {
    clock = new MutableClock(START);
    transport = new FakeAuthTransport();
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    KeyPair kp = gen.generateKeyPair();
    publicKey = kp.getPublic();
    privateKey = kp.getPrivate();
    keyResolver = (env, usage) -> publicKey;
    transport.tokenPair = pair(Duration.ofMinutes(15), Duration.ofDays(7));
  }

  private Responses.TokenPair pair(Duration accessTtl, Duration refreshTtl) {
    return new Responses.TokenPair(
        new Responses.Token("ACCESS", START.plus(accessTtl).toString()),
        new Responses.Token("REFRESH", START.plus(refreshTtl).toString()));
  }

  private DefaultAuthSession session() {
    return new DefaultAuthSession(
        transport,
        keyResolver,
        Environment.TEST,
        KSEF_TOKEN,
        NIP,
        clock,
        Duration.ofSeconds(10),
        Duration.ofSeconds(30),
        d -> clock.advance(d));
  }

  @Test
  void full_handshake_returns_access_token_and_encrypts_token() throws Exception {
    String token = session().accessToken();

    assertThat(token).isEqualTo("ACCESS");
    assertThat(transport.fetchChallengeCount.get()).isEqualTo(1);
    assertThat(transport.submitCount.get()).isEqualTo(1);
    assertThat(transport.redeemCount.get()).isEqualTo(1);

    Cipher c = Cipher.getInstance("RSA/ECB/OAEPPadding");
    c.init(
        Cipher.DECRYPT_MODE,
        privateKey,
        new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
    byte[] plain = c.doFinal(Base64.getDecoder().decode(transport.lastEncryptedToken));
    assertThat(new String(plain, StandardCharsets.UTF_8))
        .isEqualTo(KSEF_TOKEN + "|" + transport.challenge.timestampMs());
  }

  @Test
  void cached_token_reused_without_new_calls() {
    DefaultAuthSession session = session();
    session.accessToken();
    session.accessToken();
    assertThat(transport.submitCount.get()).isEqualTo(1);
    assertThat(transport.fetchChallengeCount.get()).isEqualTo(1);
  }

  @Test
  void expired_access_uses_refresh_token() {
    DefaultAuthSession session = session();
    session.accessToken();
    transport.refreshResponse =
        new Responses.AccessToken(
            new Responses.Token("ACCESS2", START.plus(Duration.ofMinutes(31)).toString()));
    clock.advance(Duration.ofMinutes(16));

    String token = session.accessToken();

    assertThat(token).isEqualTo("ACCESS2");
    assertThat(transport.refreshCount.get()).isEqualTo(1);
    assertThat(transport.submitCount.get()).isEqualTo(1);
  }

  @Test
  void expired_refresh_triggers_full_reauth() {
    DefaultAuthSession session = session();
    session.accessToken();
    clock.advance(Duration.ofDays(8));

    session.accessToken();

    assertThat(transport.submitCount.get()).isEqualTo(2);
    assertThat(transport.refreshCount.get()).isEqualTo(0);
  }

  @Test
  void failed_refresh_falls_back_to_handshake() {
    DefaultAuthSession session = session();
    session.accessToken();
    transport.refreshError = new KsefAuthenticationException("refresh rejected");
    clock.advance(Duration.ofMinutes(16));

    String token = session.accessToken();

    assertThat(transport.refreshCount.get()).isEqualTo(1);
    assertThat(transport.submitCount.get()).isEqualTo(2);
    assertThat(token).isEqualTo("ACCESS");
  }

  @Test
  void polls_until_ready() {
    transport.statusCodes.add(100);
    transport.statusCodes.add(100);
    transport.statusCodes.add(200);

    String token = session().accessToken();

    assertThat(token).isEqualTo("ACCESS");
    assertThat(transport.pollCount.get()).isEqualTo(3);
  }

  @Test
  void polls_through_non_100_1xx_status_before_ready() {
    transport.statusCodes.add(150);
    transport.statusCodes.add(200);

    String token = session().accessToken();

    assertThat(token).isEqualTo("ACCESS");
    assertThat(transport.pollCount.get()).isEqualTo(2);
  }

  @Test
  void poll_timeout_throws() {
    transport.statusCodes.add(100);

    DefaultAuthSession session = session();
    assertThatThrownBy(session::accessToken)
        .isInstanceOf(KsefAuthenticationException.class)
        .hasMessageContaining("timeout");
  }

  @Test
  void token_within_skew_window_is_treated_as_expired() {
    transport.tokenPair = pair(Duration.ofSeconds(20), Duration.ofDays(7));
    transport.refreshResponse =
        new Responses.AccessToken(
            new Responses.Token("ACCESS2", START.plus(Duration.ofMinutes(31)).toString()));
    DefaultAuthSession session = session();

    session.accessToken();
    String token = session.accessToken();

    assertThat(transport.refreshCount.get()).isEqualTo(1);
    assertThat(token).isEqualTo("ACCESS2");
  }

  @Test
  void concurrent_callers_do_one_handshake() throws Exception {
    transport.statusCodes.add(200);
    DefaultAuthSession session = session();
    int n = 8;
    ExecutorService pool = Executors.newFixedThreadPool(n);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return session.accessToken();
              }));
    }
    start.countDown();
    for (Future<String> f : futures) {
      assertThat(f.get()).isEqualTo("ACCESS");
    }
    pool.shutdown();
    assertThat(transport.submitCount.get()).isEqualTo(1);
  }

  @Test
  void transport_failure_during_handshake_propagates_as_transport_exception() {
    transport.challengeError = new KsefTransportException("network timeout");

    assertThatThrownBy(session()::accessToken)
        .isInstanceOf(KsefTransportException.class)
        .isNotInstanceOf(KsefAuthenticationException.class);
  }

  @Test
  void business_rejection_during_handshake_surfaces_as_authentication_exception() {
    transport.submitError = new KsefBusinessException("AUTH_001", 400, "invalid token");

    assertThatThrownBy(session()::accessToken)
        .isInstanceOf(KsefAuthenticationException.class)
        .hasMessageContaining("handshake failed");
  }

  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    synchronized void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public synchronized Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}
