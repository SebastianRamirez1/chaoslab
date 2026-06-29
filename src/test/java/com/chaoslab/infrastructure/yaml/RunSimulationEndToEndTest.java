package com.chaoslab.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.FailureReason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** Integración: cargar YAML -> generar workload -> correr motor, de punta a punta y reproducible. */
class RunSimulationEndToEndTest {

    @TempDir
    private Path tempDir;

    private static final String YAML = """
        name: "API de pedidos"
        seed: 42
        components:
          - { id: gateway, type: LoadBalancer }
          - { id: api-1, type: Service, capacity: 100, base_latency_ms: 50 }
          - { id: api-2, type: Service, capacity: 100, base_latency_ms: 50 }
          - { id: orders-db, type: Database, max_connections: 50, read_latency_ms: 20 }
        connections:
          - { from: gateway, to: [api-1, api-2] }
          - { from: api-1, to: orders-db }
          - { from: api-2, to: orders-db }
        workload:
          requests_per_second: 200
          duration_seconds: 15
        """;

    private RunSimulationUseCase useCase() {
        SimulationLimits limits = SimulationLimits.defaults();
        return new RunSimulationUseCase(new YamlTopologyLoader(limits), limits);
    }

    private Path topologyFile() throws IOException {
        Path file = tempDir.resolve("order-api.yaml");
        Files.writeString(file, YAML);
        return file;
    }

    @Test
    void runsEndToEndAndIsReproducible() throws IOException {
        RunSimulationUseCase useCase = useCase();
        Path file = topologyFile();

        SimulationReport first = useCase.run(file, OptionalLong.empty(), List.of());
        SimulationReport second = useCase.run(file, OptionalLong.empty(), List.of());

        assertThat(first.generatedRequests()).isPositive();
        assertThat(first.completedRequests()).isPositive();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void seedOverrideChangesOutcome() throws IOException {
        RunSimulationUseCase useCase = useCase();
        Path file = topologyFile();

        SimulationReport withSeed1 = useCase.run(file, OptionalLong.of(1L), List.of());
        SimulationReport withSeed2 = useCase.run(file, OptionalLong.of(2L), List.of());

        assertThat(withSeed1).isNotEqualTo(withSeed2);
    }

    @Test
    void crashFaultOnOneReplicaProducesFailures() throws IOException {
        RunSimulationUseCase useCase = useCase();
        Path file = topologyFile();

        // Sin fallo: todo completa. Con CrashFault en una réplica: ~la mitad falla por CRASH.
        SimulationReport healthy = useCase.run(file, OptionalLong.empty(), List.of());
        List<Fault> crash = List.of(new CrashFault("crash-api1", "api-1", 0L, 0L));
        SimulationReport crashed = useCase.run(file, OptionalLong.empty(), crash);

        assertThat(healthy.failedRequests()).isZero();
        assertThat(crashed.failedRequests()).isPositive();
        assertThat(crashed.failuresByReason()).containsKey(FailureReason.CRASH);
        assertThat(crashed.successRate()).isLessThan(healthy.successRate());
    }
}
