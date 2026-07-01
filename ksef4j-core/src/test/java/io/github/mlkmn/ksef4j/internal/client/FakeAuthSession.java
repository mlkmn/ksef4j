package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.internal.auth.AuthSession;

/** Test double for {@link AuthSession}: returns a fixed token, counting calls. */
final class FakeAuthSession implements AuthSession {

  int calls;
  private final String token;

  FakeAuthSession(String token) {
    this.token = token;
  }

  @Override
  public String accessToken() {
    calls++;
    return token;
  }
}
