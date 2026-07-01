package io.github.mlkmn.ksef4j.internal.session;

import java.time.Duration;

/**
 * Internal seam: provides delay durations for retrying or polling operations. The default
 * implementation is exponential with jitter.
 */
public interface BackoffStrategy {

  /**
   * @param attemptNumber 1-based attempt number (first attempt is 1)
   * @return suggested delay before the {@code attemptNumber}-th attempt
   */
  Duration delayBeforeAttempt(int attemptNumber);
}
