package com.chaoslab.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Database;
import com.chaoslab.domain.topology.LoadBalancer;
import com.chaoslab.domain.topology.MessageQueue;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.PoissonWorkloadGenerator;
import com.chaoslab.domain.workload.Workload;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * La invariante de Fiabilidad (#1): mismo seed -> mismo resultado. Construye una topología fresca
 * en cada corrida (los componentes son estado mutable) y compara los reportes completos.
 */
class DeterminismTest {

    private static TopologyGraph freshTopology() {
        List<Component> components = List.of(
            new LoadBalancer("gateway"),
            new Service("api-1", 100, 50L),
            new Service("api-2", 100, 50L),
            new MessageQueue("orders-queue", 1000),
            new Database("orders-db", 50, 20L));
        List<Connection> connections = List.of(
            new Connection("gateway", "api-1"),
            new Connection("gateway", "api-2"),
            new Connection("api-1", "orders-queue"),
            new Connection("api-2", "orders-queue"),
            new Connection("orders-queue", "orders-db"));
        return TopologyGraph.of("API de pedidos", components, connections);
    }

    private static SimulationReport runWithSeed(long seed) {
        TopologyGraph topology = freshTopology();
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, 20_000_000L);
        PoissonWorkloadGenerator generator = new PoissonWorkloadGenerator(new Random(seed));
        long generated = generator.scheduleArrivals(engine, new Workload(200, 30), topology.entryPointId(), 5_000_000L);
        return engine.run(seed, generated);
    }

    @Test
    void sameSeedProducesIdenticalReport() {
        assertThat(runWithSeed(42L)).isEqualTo(runWithSeed(42L));
    }

    @Test
    void differentSeedProducesDifferentReport() {
        assertThat(runWithSeed(42L)).isNotEqualTo(runWithSeed(7L));
    }

    @Test
    void simulationProducesMeaningfulMetrics() {
        SimulationReport report = runWithSeed(42L);

        assertThat(report.generatedRequests()).isPositive();
        assertThat(report.completedRequests()).isPositive();
        assertThat(report.successRate()).isBetween(0.0, 1.0);
        assertThat(report.components()).hasSize(5);
    }
}
