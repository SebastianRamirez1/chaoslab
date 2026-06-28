package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests del reloj virtual: unit puro del dominio, sin levantar Spring (rápido, §4.3). */
class ClockTest {

    @Test
    void startsAtZeroByDefault() {
        assertThat(new Clock().now()).isZero();
    }

    @Test
    void startsAtGivenInstant() {
        assertThat(new Clock(1_000L).now()).isEqualTo(1_000L);
    }

    @Test
    void advancesForward() {
        Clock clock = new Clock();
        clock.advanceTo(100L);
        assertThat(clock.now()).isEqualTo(100L);
    }

    @Test
    void allowsAdvancingToSameInstant() {
        Clock clock = new Clock(50L);
        clock.advanceTo(50L);
        assertThat(clock.now()).isEqualTo(50L);
    }

    @Test
    void rejectsNegativeStart() {
        assertThatThrownBy(() -> new Clock(-1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMovingBackwards() {
        Clock clock = new Clock(50L);
        assertThatThrownBy(() -> clock.advanceTo(10L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
