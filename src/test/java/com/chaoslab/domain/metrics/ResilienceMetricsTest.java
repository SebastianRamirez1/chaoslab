package com.chaoslab.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.chaoslab.domain.resilience.CircuitBreakerState;
import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.Health;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las métricas de resiliencia se derivan de la timeline de forma pura y determinista. */
class ResilienceMetricsTest {

    private static ComponentSnapshot api(Health health) {
        return new ComponentSnapshot("api", ComponentType.SERVICE, health, 0);
    }

    private static SimulationSnapshot snap(long atMillis, long completed, long failed, Health health) {
        return new SimulationSnapshot(atMillis, completed, failed, 0L, List.of(api(health)), List.of());
    }

    @Test
    void availabilityCountsInstantsWithoutADownComponent() {
        // api: UP, DOWN, DOWN, UP, UP -> 3 de 5 instantes sin DOWN.
        List<SimulationSnapshot> timeline = List.of(
            snap(0, 0, 0, Health.UP),
            snap(1000, 10, 0, Health.DOWN),
            snap(2000, 10, 5, Health.DOWN),
            snap(3000, 20, 5, Health.UP),
            snap(4000, 30, 5, Health.UP));

        ResilienceMetrics metrics = ResilienceMetrics.from(timeline);

        assertThat(metrics.availability()).isEqualTo(0.6, within(1e-9));
    }

    @Test
    void mttrIsTheMeanDurationOfRecoveredImpairmentEpisodes() {
        // Deterioro de t=1000 a t=3000 (vuelve a UP) => episodio recuperado de 2000ms.
        List<SimulationSnapshot> timeline = List.of(
            snap(0, 0, 0, Health.UP),
            snap(1000, 0, 0, Health.DEGRADED),
            snap(2000, 0, 0, Health.DOWN),
            snap(3000, 0, 0, Health.UP));

        ResilienceMetrics metrics = ResilienceMetrics.from(timeline);

        assertThat(metrics.meanTimeToRecoveryMillis()).isEqualTo(2000L);
        assertThat(metrics.longestDowntimeMillis()).isEqualTo(2000L);
    }

    @Test
    void anUnrecoveredEpisodeStillCountsAsDowntime() {
        // Nunca vuelve a UP: no hay MTTR, pero sí downtime hasta el último instante.
        List<SimulationSnapshot> timeline = List.of(
            snap(0, 0, 0, Health.UP),
            snap(1000, 0, 0, Health.DOWN),
            snap(2000, 0, 0, Health.DOWN));

        ResilienceMetrics metrics = ResilienceMetrics.from(timeline);

        assertThat(metrics.meanTimeToRecoveryMillis()).isZero();
        assertThat(metrics.longestDowntimeMillis()).isEqualTo(1000L);
    }

    @Test
    void worstWindowSuccessRateFindsTheDeepestDip() {
        // Ventanas: [t1] 10/10=1.0, [t2] 2 ok / 8 fallos = 0.2, [t3] 10/10=1.0.
        List<SimulationSnapshot> timeline = List.of(
            snap(0, 0, 0, Health.UP),
            snap(1000, 10, 0, Health.UP),
            snap(2000, 12, 8, Health.UP),
            snap(3000, 22, 8, Health.UP));

        ResilienceMetrics metrics = ResilienceMetrics.from(timeline);

        assertThat(metrics.worstWindowSuccessRate()).isEqualTo(0.2, within(1e-9));
    }

    @Test
    void timeToFirstBreakerTripIsWhenABreakerFirstOpens() {
        SimulationSnapshot closed = new SimulationSnapshot(0, 0, 0, 0L, List.of(api(Health.UP)),
            List.of(new BreakerSnapshot("gateway", "api", CircuitBreakerState.CLOSED)));
        SimulationSnapshot open = new SimulationSnapshot(2000, 0, 0, 0L, List.of(api(Health.DOWN)),
            List.of(new BreakerSnapshot("gateway", "api", CircuitBreakerState.OPEN)));

        ResilienceMetrics metrics = ResilienceMetrics.from(List.of(closed, open));

        assertThat(metrics.timeToFirstBreakerTripMillis()).isEqualTo(2000L);
    }

    @Test
    void anEmptyTimelineIsNeutral() {
        ResilienceMetrics metrics = ResilienceMetrics.from(List.of());

        assertThat(metrics.availability()).isEqualTo(1.0);
        assertThat(metrics.timeToFirstBreakerTripMillis()).isEqualTo(-1L);
        assertThat(metrics.worstWindowSuccessRate()).isEqualTo(1.0);
    }
}
