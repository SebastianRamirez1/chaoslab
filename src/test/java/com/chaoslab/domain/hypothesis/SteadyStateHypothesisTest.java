package com.chaoslab.domain.hypothesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** El oráculo del caos: la hipótesis de estado estable evalúa invariantes contra una corrida. */
class SteadyStateHypothesisTest {

    /** Reporte de una corrida "sana": 99.5% de éxito, p95=120ms. */
    private static SimulationReport report(double successRate, long p95) {
        return new SimulationReport("demo", 42L, 30_000L, 1000L, 995L, 5L, successRate,
            new LatencyStats(995L, 10L, 70L, p95, 200L, 300L), List.of(), Map.of(), List.of());
    }

    @Test
    void aSatisfiedInvariantPasses() {
        Invariant invariant = new Invariant(Metric.SUCCESS_RATE, Comparison.GTE, 0.99);

        InvariantResult result = invariant.evaluate(report(0.995, 120));

        assertThat(result.satisfied()).isTrue();
        assertThat(result.actual()).isEqualTo(0.995);
    }

    @Test
    void aViolatedInvariantFails() {
        Invariant invariant = new Invariant(Metric.P95_LATENCY_MS, Comparison.LTE, 100);

        InvariantResult result = invariant.evaluate(report(0.995, 120));

        assertThat(result.satisfied()).isFalse();
        assertThat(result.actual()).isEqualTo(120.0);
    }

    @Test
    void hypothesisPassesOnlyWhenEveryInvariantHolds() {
        SteadyStateHypothesis hypothesis = new SteadyStateHypothesis(List.of(
            new Invariant(Metric.SUCCESS_RATE, Comparison.GTE, 0.99),
            new Invariant(Metric.P95_LATENCY_MS, Comparison.LTE, 150)));

        HypothesisReport passing = hypothesis.evaluate(report(0.995, 120));
        HypothesisReport failing = hypothesis.evaluate(report(0.995, 200)); // p95 se pasa del umbral

        assertThat(passing.declared()).isTrue();
        assertThat(passing.satisfied()).isTrue();
        assertThat(failing.satisfied()).isFalse();
        assertThat(failing.results()).hasSize(2);
    }

    @Test
    void anUndeclaredHypothesisIsVacuouslySatisfied() {
        HypothesisReport report = SteadyStateHypothesis.none().evaluate(report(0.5, 999));

        assertThat(report.declared()).isFalse();
        assertThat(report.satisfied()).isTrue();
        assertThat(report.results()).isEmpty();
    }

    @Test
    void sameReportYieldsAnEqualVerdict() {
        SteadyStateHypothesis hypothesis = new SteadyStateHypothesis(List.of(
            new Invariant(Metric.FAILED_REQUESTS, Comparison.LT, 10)));

        assertThat(hypothesis.evaluate(report(0.995, 120)))
            .isEqualTo(hypothesis.evaluate(report(0.995, 120)));
    }

    @Test
    void metricAndComparisonParseFromYamlTokens() {
        assertThat(Metric.fromKey("success_rate")).isEqualTo(Metric.SUCCESS_RATE);
        assertThat(Comparison.fromToken(">=")).isEqualTo(Comparison.GTE);
        assertThat(Comparison.fromToken("lte")).isEqualTo(Comparison.LTE);
    }

    @Test
    void unknownMetricOrComparisonIsRejected() {
        assertThatThrownBy(() -> Metric.fromKey("throughput"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comparison.fromToken("~="))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
