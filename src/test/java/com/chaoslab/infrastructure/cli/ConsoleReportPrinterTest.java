package com.chaoslab.infrastructure.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.metrics.ComponentReport;
import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.Health;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleReportPrinterTest {

    @Test
    void formatIncludesHeadlineMetrics() {
        SimulationReport report = new SimulationReport(
            "demo", 42L, 30_000L, 100L, 95L, 5L, 0.95,
            new LatencyStats(95L, 50L, 70L, 120L, 200L, 250L),
            List.of(new ComponentReport("api", ComponentType.SERVICE, 100L, 5L, 12, Health.UP)));

        String text = new ConsoleReportPrinter().format(report);

        assertThat(text).contains("demo");
        assertThat(text).contains("seed=42");
        assertThat(text).contains("completados=95");
        assertThat(text).contains("p95=120");
        assertThat(text).contains("api");
        assertThat(text).contains("SERVICE");
    }
}
