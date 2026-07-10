package com.chaoslab.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.infrastructure.yaml.YamlTopologyLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** El gate de resiliencia distingue escenarios que sostienen su hipótesis de los que no. */
class ResilienceCheckUseCaseTest {

    @TempDir
    private Path tempDir;

    private ResilienceCheckUseCase useCase() {
        SimulationLimits limits = SimulationLimits.defaults();
        return new ResilienceCheckUseCase(new RunSimulationUseCase(new YamlTopologyLoader(limits), limits));
    }

    private static final String BASE = """
        name: t
        seed: 1
        components:
          - { id: gateway, type: LoadBalancer }
          - { id: api-1, type: Service, capacity: 100, base_latency_ms: 10 }
          - { id: api-2, type: Service, capacity: 100, base_latency_ms: 10 }
        connections:
          - { from: gateway, to: [api-1, api-2] }
        workload: { requests_per_second: 100, duration_seconds: 5 }
        """;

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void passesWhenTheHypothesisHolds() throws IOException {
        Path healthy = write("healthy.yaml", BASE + """
            steady_state:
              - { metric: success_rate, comparison: ">=", threshold: 0.99 }
            """);

        List<CheckOutcome> outcomes = useCase().check(List.of(healthy));

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.scenario()).isEqualTo("healthy.yaml");
            assertThat(o.passed()).isTrue();
        });
    }

    @Test
    void failsWhenTheHypothesisIsRefuted() throws IOException {
        // Un crash permanente en una réplica sin resiliencia tira el éxito por debajo del SLO.
        Path broken = write("broken.yaml", BASE + """
            faults:
              - { type: crash, target: api-1, at_second: 0 }
            steady_state:
              - { metric: success_rate, comparison: ">=", threshold: 0.99 }
            """);

        List<CheckOutcome> outcomes = useCase().check(List.of(broken));

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.declared()).isTrue();
            assertThat(o.satisfied()).isFalse();
            assertThat(o.passed()).isFalse();
        });
    }

    @Test
    void flagsScenariosWithoutADeclaredHypothesis() throws IOException {
        Path noHypothesis = write("no-hyp.yaml", BASE);

        List<CheckOutcome> outcomes = useCase().check(List.of(noHypothesis));

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.declared()).isFalse();
            assertThat(o.passed()).isFalse();
        });
    }
}
