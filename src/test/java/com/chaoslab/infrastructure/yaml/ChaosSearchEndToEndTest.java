package com.chaoslab.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.application.ChaosSearchUseCase;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.search.ChaosSearchResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** Integración: la búsqueda de caos halla el punto débil real de una topología resiliente. */
class ChaosSearchEndToEndTest {

    @TempDir
    private Path tempDir;

    // Gateway con breaker protege las réplicas api, pero la DB es un punto único de fallo (SPOF).
    private static final String YAML = """
        name: "resiliente con SPOF"
        seed: 1
        components:
          - id: gateway
            type: LoadBalancer
            resilience:
              circuit_breaker: { threshold: 5, cooldown_ms: 2000 }
          - { id: api-1, type: Service, capacity: 100, base_latency_ms: 30 }
          - { id: api-2, type: Service, capacity: 100, base_latency_ms: 30 }
          - { id: db, type: Database, max_connections: 50, read_latency_ms: 20 }
        connections:
          - { from: gateway, to: [api-1, api-2] }
          - { from: api-1, to: db }
          - { from: api-2, to: db }
        workload: { requests_per_second: 100, duration_seconds: 10 }
        steady_state:
          - { metric: success_rate, comparison: ">=", threshold: 0.99 }
        """;

    private ChaosSearchUseCase useCase() {
        SimulationLimits limits = SimulationLimits.defaults();
        return new ChaosSearchUseCase(new YamlTopologyLoader(limits), limits);
    }

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("topology.yaml");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void findsTheDatabaseAsTheSinglePointOfFailure() throws IOException {
        ChaosSearchResult result = useCase().search(write(YAML), 3, 80);

        // El breaker cubre las réplicas api; el escenario mínimo que rompe el SLO es tumbar la DB.
        assertThat(result.refuted()).isTrue();
        assertThat(result.minimal().faultCount()).isEqualTo(1);
        Fault fault = result.minimal().faults().get(0);
        assertThat(fault).isInstanceOf(CrashFault.class);
        assertThat(((CrashFault) fault).targetId()).isEqualTo("db");
    }

    @Test
    void requiresADeclaredHypothesis() throws IOException {
        String withoutHypothesis = """
            name: t
            components:
              - { id: api, type: Service, capacity: 10, base_latency_ms: 5 }
            workload: { requests_per_second: 10, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> useCase().search(write(withoutHypothesis), 3, 80))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("steady_state");
    }
}
