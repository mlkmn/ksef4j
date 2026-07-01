package io.github.mlkmn.ksef4j.internal.session;

import java.time.Duration;

/**
 * Internal: the wait between UPO-status polls. Injectable so tests advance a clock instead of
 * sleeping.
 */
@FunctionalInterface
interface Sleeper {
  void sleep(Duration duration) throws InterruptedException;
}
