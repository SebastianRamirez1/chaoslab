package com.chaoslab.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.hypothesis.Comparison;
import com.chaoslab.domain.hypothesis.Invariant;
import com.chaoslab.domain.hypothesis.Metric;
import com.chaoslab.domain.hypothesis.SteadyStateHypothesis;
import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La búsqueda de caos halla y minimiza el escenario que refuta la hipótesis (mini-DST). */
class ChaosSearchTest {

    private static final SteadyStateHypothesis SLO = new SteadyStateHypothesis(List.of(
        new Invariant(Metric.SUCCESS_RATE, Comparison.GTE, 0.99)));
    private static final List<Long> SEEDS = List.of(0L, 1L, 2L);

    /** Reporte con la tasa de éxito dada (timeline vacía: worstWindow queda neutro). */
    private static SimulationReport report(double successRate) {
        long completed = Math.round(successRate * 100);
        return new SimulationReport("t", 0L, 1000L, 100L, completed, 100 - completed, successRate,
            new LatencyStats(0, 0, 0, 0, 0, 0), List.of(), Map.of(), List.of());
    }

    private static boolean crashes(List<Fault> faults, String target) {
        return faults.stream().anyMatch(f -> f instanceof CrashFault c && c.targetId().equals(target));
    }

    @Test
    void findsTheSingleFaultThatRefutesTheHypothesis() {
        List<Fault> candidates = List.of(
            new CrashFault("c-db", "db", 0L, 0L),
            new CrashFault("c-api", "api", 0L, 0L));
        // Solo la caída de la DB (SPOF) rompe el SLO.
        ScenarioRunner runner = (seed, faults) -> report(crashes(faults, "db") ? 0.5 : 1.0);

        ChaosSearchResult result = new ChaosSearch(runner, SLO).search(SEEDS, candidates, 80);

        assertThat(result.refuted()).isTrue();
        assertThat(result.minimal().faultCount()).isEqualTo(1);
        assertThat(result.minimal().faults().get(0)).isInstanceOf(CrashFault.class);
        assertThat(((CrashFault) result.minimal().faults().get(0)).targetId()).isEqualTo("db");
    }

    @Test
    void reportsRobustWhenNothingRefutes() {
        List<Fault> candidates = List.of(
            new CrashFault("c-db", "db", 0L, 0L),
            new CrashFault("c-api", "api", 0L, 0L));
        ScenarioRunner runner = (seed, faults) -> report(1.0); // nada rompe el SLO

        ChaosSearchResult result = new ChaosSearch(runner, SLO).search(SEEDS, candidates, 80);

        assertThat(result.refuted()).isFalse();
        assertThat(result.counterexample()).isEmpty();
        assertThat(result.scenariosEvaluated()).isPositive();
    }

    @Test
    void shrinksTheLatencyIntensityToTheMinimumThatStillBreaksIt() {
        // La latencia rompe el SLO solo si extra >= 100ms; el candidato arranca en 256ms.
        List<Fault> candidates = List.of(new LatencyFault("l-db", "db", 0L, 1000L, 256L));
        ScenarioRunner runner = (seed, faults) -> {
            boolean heavy = faults.stream()
                .anyMatch(f -> f instanceof LatencyFault l && l.extraMillis() >= 100);
            return report(heavy ? 0.5 : 1.0);
        };

        ChaosSearchResult result = new ChaosSearch(runner, SLO).search(SEEDS, candidates, 80);

        assertThat(result.refuted()).isTrue();
        LatencyFault minimal = (LatencyFault) result.minimal().faults().get(0);
        // 256 -> 128 (aún refuta) -> 64 (ya no): el mínimo por halving es 128ms.
        assertThat(minimal.extraMillis()).isEqualTo(128L);
    }

    @Test
    void isDeterministic() {
        List<Fault> candidates = List.of(new CrashFault("c-db", "db", 0L, 0L));
        ScenarioRunner runner = (seed, faults) -> report(crashes(faults, "db") ? 0.5 : 1.0);

        ChaosSearchResult first = new ChaosSearch(runner, SLO).search(SEEDS, candidates, 80);
        ChaosSearchResult second = new ChaosSearch(runner, SLO).search(SEEDS, candidates, 80);

        assertThat(first).isEqualTo(second);
    }
}
