package com.chaoslab.infrastructure.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.hypothesis.Comparison;
import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.hypothesis.InvariantResult;
import com.chaoslab.domain.hypothesis.Metric;
import com.chaoslab.domain.metrics.ComponentReport;
import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.Health;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConsoleReportPrinterTest {

    @Test
    void formatIncludesHeadlineMetrics() {
        SimulationReport report = new SimulationReport(
            "demo", 42L, 30_000L, 100L, 95L, 5L, 0.95,
            new LatencyStats(95L, 50L, 70L, 120L, 200L, 250L),
            List.of(new ComponentReport("api", ComponentType.SERVICE, 100L, 5L, 12, Health.UP)),
            Map.of(FailureReason.CAPACITY, 5L), List.of());

        String text = new ConsoleReportPrinter().format(report);

        assertThat(text).contains("demo");
        assertThat(text).contains("seed=42");
        assertThat(text).contains("completados=95");
        assertThat(text).contains("p95=120");
        assertThat(text).contains("api");
        assertThat(text).contains("SERVICE");
        assertThat(text).contains("fallos por causa");
        assertThat(text).contains("CAPACITY=5");
    }

    @Test
    void formatsAViolatedHypothesisAsFalla() {
        HypothesisReport hypothesis = new HypothesisReport(true, false, List.of(
            new InvariantResult(Metric.SUCCESS_RATE, Comparison.GTE, 0.99, 0.831, false)));

        String text = new ConsoleReportPrinter().formatHypothesis(hypothesis);

        assertThat(text).contains("FALLA");
        assertThat(text).contains("success_rate >= 0.990");
        assertThat(text).contains("0.831");
    }

    @Test
    void anUndeclaredHypothesisPrintsNothing() {
        assertThat(new ConsoleReportPrinter().formatHypothesis(HypothesisReport.notDeclared())).isEmpty();
    }
}
