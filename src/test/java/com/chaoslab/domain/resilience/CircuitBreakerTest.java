package com.chaoslab.domain.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** El breaker abre tras N fallos, corta durante el enfriamiento y se cierra al recuperarse. */
class CircuitBreakerTest {

    @Test
    void opensAfterThresholdConsecutiveFailures() {
        CircuitBreaker breaker = new CircuitBreaker(3, 1_000L);
        assertThat(breaker.isCallPermitted(0L)).isTrue();

        breaker.recordFailure(0L);
        breaker.recordFailure(0L);
        assertThat(breaker.isCallPermitted(0L)).isTrue();
        breaker.recordFailure(0L);

        assertThat(breaker.state(0L)).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(breaker.isCallPermitted(500L)).isFalse();
    }

    @Test
    void allowsTrialAfterCooldownThenClosesOnSuccess() {
        CircuitBreaker breaker = new CircuitBreaker(2, 1_000L);
        breaker.recordFailure(0L);
        breaker.recordFailure(0L);

        // Pasado el enfriamiento: semiabierto, permite una prueba.
        assertThat(breaker.isCallPermitted(1_000L)).isTrue();
        assertThat(breaker.state(1_000L)).isEqualTo(CircuitBreakerState.HALF_OPEN);

        breaker.recordSuccess();
        assertThat(breaker.state(1_000L)).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(breaker.isCallPermitted(1_000L)).isTrue();
    }

    @Test
    void reopensIfTrialFails() {
        CircuitBreaker breaker = new CircuitBreaker(1, 500L);
        breaker.recordFailure(0L);
        assertThat(breaker.isCallPermitted(500L)).isTrue();

        breaker.recordFailure(500L);
        assertThat(breaker.isCallPermitted(600L)).isFalse();
        assertThat(breaker.state(600L)).isEqualTo(CircuitBreakerState.OPEN);
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new CircuitBreaker(0, 1_000L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CircuitBreaker(3, -1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
