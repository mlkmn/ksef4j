package io.github.mlkmn.ksef4j.internal.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExponentialBackoffTest {

    @Test
    void schedule_doubles_and_caps_without_jitter() {
        ExponentialBackoff b = new ExponentialBackoff(Duration.ofMillis(500), Duration.ofSeconds(4), () -> 0.5);
        assertThat(b.delayBeforeAttempt(1)).isEqualTo(Duration.ofMillis(500));
        assertThat(b.delayBeforeAttempt(2)).isEqualTo(Duration.ofSeconds(1));
        assertThat(b.delayBeforeAttempt(3)).isEqualTo(Duration.ofSeconds(2));
        assertThat(b.delayBeforeAttempt(4)).isEqualTo(Duration.ofSeconds(4));
        assertThat(b.delayBeforeAttempt(5)).isEqualTo(Duration.ofSeconds(4));
        assertThat(b.delayBeforeAttempt(40)).isEqualTo(Duration.ofSeconds(4)); // overflow-safe
    }

    @Test
    void jitter_stays_within_band() {
        ExponentialBackoff low = new ExponentialBackoff(Duration.ofMillis(500), Duration.ofSeconds(4), () -> 0.0);
        ExponentialBackoff high = new ExponentialBackoff(Duration.ofMillis(500), Duration.ofSeconds(4), () -> 0.999999);
        assertThat(low.delayBeforeAttempt(1)).isEqualTo(Duration.ofMillis(400));   // 0.8 * 500ms
        assertThat(high.delayBeforeAttempt(1)).isBetween(Duration.ofMillis(599), Duration.ofMillis(600)); // ~1.2 * 500ms
    }
}
