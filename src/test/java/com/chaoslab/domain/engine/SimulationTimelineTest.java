package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.metrics.SimulationSnapshot;
import com.chaoslab.domain.topology.Health;
import com.chaoslab.domain.topology.Request;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El motor produce una línea de tiempo de snapshots que el dashboard podrá reproducir. */
class SimulationTimelineTest {

    private static SimulationEngine engineFor(TopologyGraph topology, MetricsCollector metrics) {
        return new SimulationEngine(topology, metrics, 1_000_000L);
    }

    @Test
    void producesSnapshotsOverTimeEndingWithFinalTotals() {
        TopologyGraph topology = TopologyGraph.of("t", List.of(new Service("svc", 1000, 10L)), List.of());
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = engineFor(topology, metrics);
        for (long second = 0; second < 3; second++) {
            engine.schedule(new RequestArrived(second * 1000 + 500, new Request(second, second * 1000 + 500), "svc"));
        }

        SimulationReport report = engine.run(1L, 3);
        List<SimulationSnapshot> timeline = report.timeline();

        assertThat(timeline).isNotEmpty();
        assertThat(timeline).allMatch(s -> s.components().size() == 1);
        SimulationSnapshot last = timeline.get(timeline.size() - 1);
        assertThat(last.completedSoFar()).isEqualTo(report.completedRequests());
    }

    @Test
    void snapshotsReflectComponentHealthDuringACrash() {
        TopologyGraph topology = TopologyGraph.of("t", List.of(new Service("svc", 1000, 10L)), List.of());
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = engineFor(topology, metrics);
        engine.schedule(new FaultInjected(1000L, new CrashFault("c", "svc", 1000L, 2000L)));
        engine.schedule(new RequestArrived(4000L, new Request(0, 4000L), "svc"));

        SimulationReport report = engine.run(1L, 1);

        boolean sawDown = report.timeline().stream()
            .flatMap(s -> s.components().stream())
            .anyMatch(c -> c.health() == Health.DOWN);
        assertThat(sawDown).isTrue();
    }
}
