package io.github.mlkmn.ksef4j.internal.session;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Internal: exponential backoff from {@code initial} to {@code max} with a +-20% jitter. Stateless
 * and thread-safe.
 */
public final class ExponentialBackoff implements BackoffStrategy {

  private final long initialNanos;
  private final long maxNanos;
  private final DoubleSupplier jitter;

  public ExponentialBackoff() {
    this(
        Duration.ofMillis(500),
        Duration.ofSeconds(4),
        () -> ThreadLocalRandom.current().nextDouble());
  }

  public ExponentialBackoff(Duration initial, Duration max, DoubleSupplier jitter) {
    this.initialNanos = initial.toNanos();
    this.maxNanos = max.toNanos();
    this.jitter = jitter;
  }

  @Override
  public Duration delayBeforeAttempt(int attemptNumber) {
    long base = baseNanos(attemptNumber);
    double factor = 0.8 + 0.4 * jitter.getAsDouble();
    return Duration.ofNanos((long) (base * factor));
  }

  private long baseNanos(int attemptNumber) {
    int shifts = attemptNumber - 1;
    // Overflow-safe: if doubling would reach max (or shift past long range), clamp to max.
    if (shifts >= 62 || initialNanos > (maxNanos >> shifts)) {
      return maxNanos;
    }
    return Math.min(initialNanos << shifts, maxNanos);
  }
}
