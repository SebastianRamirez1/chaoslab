package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.NetworkPartition;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Database;
import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.Request;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** El motor inyecta y revierte fallos por evento, afectando el resultado de forma determinista. */
class FaultInjectionTest {

    private static final long MAX_EVENTS = 1_000_000L;

    private static void arriveAt(SimulationEngine engine, long time, long id, String componentId) {
        engine.schedule(new RequestArrived(time, new Request(id, time), componentId));
    }

    @Test
    void crashFaultFailsRequestsOnlyDuringItsWindow() {
        TopologyGraph topology = TopologyGraph.of(
            "t", List.of(new Service("svc", 100, 10L)), List.of());
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);

        // svc cae en [100, 150).
        engine.schedule(new FaultInjected(100L, new CrashFault("c", "svc", 100L, 50L)));
        arriveAt(engine, 50L, 0, "svc");    // antes -> completa
        arriveAt(engine, 120L, 1, "svc");   // durante -> falla por CRASH
        arriveAt(engine, 200L, 2, "svc");   // después de recuperar -> completa

        SimulationReport report = engine.run(1L, 3);

        assertThat(report.completedRequests()).isEqualTo(2L);
        assertThat(report.failedRequests()).isEqualTo(1L);
        assertThat(report.failuresByReason()).containsEntry(FailureReason.CRASH, 1L);
    }

    @Test
    void networkPartitionFailsRequestsThatMustCross() {
        TopologyGraph topology = TopologyGraph.of(
            "t",
            List.of(new Service("api", 100, 10L), new Database("db", 100, 5L)),
            List.of(new Connection("api", "db")));
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);

        engine.schedule(new FaultInjected(0L, new NetworkPartition("p", Set.of("api"), Set.of("db"), 0L, 0L)));
        arriveAt(engine, 10L, 0, "api");

        SimulationReport report = engine.run(1L, 1);

        assertThat(report.completedRequests()).isZero();
        assertThat(report.failuresByReason()).containsEntry(FailureReason.NETWORK_PARTITION, 1L);
    }
}
