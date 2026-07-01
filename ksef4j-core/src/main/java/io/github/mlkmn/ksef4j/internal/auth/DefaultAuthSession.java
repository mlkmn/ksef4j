package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;
import io.github.mlkmn.ksef4j.error.KsefException;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Responses;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Internal: provides a valid KSeF access token, caching it and refreshing
 * (refresh-token then full re-auth) as needed. Thread-safe; concurrent callers
 * share one in-flight refresh.
 */
public final class DefaultAuthSession implements AuthSession {

    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(30);
    private static final int STATUS_SUCCESS = 200;

    private final HttpTransport transport;
    private final KeyResolver keyResolver;
    private final Environment environment;
    private final String ksefToken;
    private final String nip;
    private final Clock clock;
    private final Duration pollInterval;
    private final Duration authTimeout;
    private final Sleeper sleeper;

    private final ReentrantLock lock = new ReentrantLock();
    private CachedToken access;
    private CachedToken refresh;

    private record CachedToken(String value, Instant expiry) {
    }

    public DefaultAuthSession(HttpTransport transport, KeyResolver keyResolver, Environment environment,
                              String ksefToken, String nip, Clock clock,
                              Duration pollInterval, Duration authTimeout) {
        this(transport, keyResolver, environment, ksefToken, nip, clock, pollInterval, authTimeout,
                duration -> Thread.sleep(duration));
    }

    DefaultAuthSession(HttpTransport transport, KeyResolver keyResolver, Environment environment,
                       String ksefToken, String nip, Clock clock,
                       Duration pollInterval, Duration authTimeout, Sleeper sleeper) {
        this.transport = transport;
        this.keyResolver = keyResolver;
        this.environment = environment;
        this.ksefToken = ksefToken;
        this.nip = nip;
        this.clock = clock;
        this.pollInterval = pollInterval;
        this.authTimeout = authTimeout;
        this.sleeper = sleeper;
    }

    @Override
    public String accessToken() {
        lock.lock();
        try {
            if (access != null && clock.instant().isBefore(access.expiry().minus(EXPIRY_SKEW))) {
                return access.value();
            }
            if (refresh != null && clock.instant().isBefore(refresh.expiry().minus(EXPIRY_SKEW))) {
                try {
                    access = cached(transport.refreshToken(refresh.value()).accessToken());
                    return access.value();
                } catch (KsefException e) {
                    // refresh rejected/expired server-side -> fall back to a full handshake
                }
            }
            handshake();
            return access.value();
        } finally {
            lock.unlock();
        }
    }

    private void handshake() {
        Responses.Challenge challenge = transport.fetchChallenge();
        String encrypted = KsefCrypto.encryptToken(
                ksefToken, challenge.timestampMs(),
                keyResolver.publicKey(environment, KeyUsage.TOKEN_ENCRYPTION));
        Responses.AuthSubmit submit = transport.submitKsefTokenAuth(challenge.challenge(), nip, encrypted);
        String authToken = submit.authenticationToken().token();
        awaitAuthReady(submit.referenceNumber(), authToken);
        Responses.TokenPair pair = transport.redeemTokens(authToken);
        access = cached(pair.accessToken());
        refresh = cached(pair.refreshToken());
    }

    private void awaitAuthReady(String referenceNumber, String authToken) {
        Instant deadline = clock.instant().plus(authTimeout);
        while (true) {
            int code = transport.pollAuthStatus(referenceNumber, authToken).status().code();
            if (code == STATUS_SUCCESS) {
                return;
            }
            // KSeF status codes follow an HTTP-like family: 1xx = still processing, 200 = ready,
            // >= 300 = terminal failure. Keep polling on any 1xx (not just 100).
            boolean inProgress = code >= 100 && code < 200;
            if (!inProgress) {
                throw new KsefAuthenticationException("KSeF auth failed with status code " + code);
            }
            if (!clock.instant().isBefore(deadline)) {
                throw new KsefAuthenticationException("KSeF auth status not ready before timeout");
            }
            try {
                sleeper.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KsefAuthenticationException("Interrupted while awaiting KSeF auth", e);
            }
        }
    }

    private static CachedToken cached(Responses.Token token) {
        try {
            return new CachedToken(token.token(), Instant.parse(token.validUntil()));
        } catch (DateTimeParseException e) {
            throw new KsefAuthenticationException("Unparseable token validUntil: " + token.validUntil(), e);
        }
    }
}
