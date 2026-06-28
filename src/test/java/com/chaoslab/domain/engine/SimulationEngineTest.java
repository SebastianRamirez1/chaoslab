package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Comportamiento del motor de eventos discretos sobre topologías pequeñas y verificables. */
class SimulationEngineTest {

    private static final long MAX_EVENTS = 1_000_000L;

    @Test
    void processesRequestsThroughTerminalComponent() {
        TopologyGraph topology = TopologyGraph.of(
            "t", List.of(new Service("svc", 100, 50L)), List.of());
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);

        for (long id = 0; id < 3; id++) {
            engine.schedule(new RequestArrived(id * 10, new com.chaoslab.domain.topology.Request(id, id * 10), "svc"));
        }
        SimulationReport report = engine.run(7L, 3);

        assertThat(report.completedRequests()).isEqualTo(3L);
        assertThat(report.failedRequests()).isZero();
        assertThat(report.latency().max()).isEqualTo(50L);
        assertThat(report.successRate()).isEqualTo(1.0);
    }

    @Test
    void rejectsRequestsOverCapacity() {
        TopologyGraph topology = TopologyGraph.of(
            "t", List.of(new Service("svc", 1, 50L)), List.of());
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);

        for (long id = 0; id < 3; id++) {
            engine.schedule(new RequestArrived(0L, new com.chaoslab.domain.topology.Request(id, 0L), "svc"));
        }
        SimulationReport report = engine.run(7L, 3);

        assertThat(report.completedRequests()).isEqualTo(1L);
        assertThat(report.failedRequests()).isEqualTo(2L);
    }

    @Test
    void routesThroughChainToDatabase() {
        TopologyGraph topology = TopologyGraph.of(
            "chain",
            List.of(new Service("api", 100, 30L), new Service("db", 100, 20L)),
            List.of(new Connection("api", "db")));
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);

        engine.schedule(new RequestArrived(0L, new com.chaoslab.domain.topology.Request(0, 0L), "api"));
        SimulationReport report = engine.run(7L, 1);

        // latencia extremo a extremo = 30 (api) + 20 (db)
        assertThat(report.completedRequests()).isEqualTo(1L);
        assertThat(report.latency().max()).isEqualTo(50L);
    }
}
