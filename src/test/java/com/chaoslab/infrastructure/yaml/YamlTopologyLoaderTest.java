package com.chaoslab.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.application.LoadedScenario;
import com.chaoslab.domain.engine.SimulationLimits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** El loader trata el YAML como entrada no confiable: valida estructura, rangos y referencias. */
class YamlTopologyLoaderTest {

    private final YamlTopologyLoader loader = new YamlTopologyLoader(SimulationLimits.defaults());

    @TempDir
    private Path tempDir;

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("topology.yaml");
        Files.writeString(file, content);
        return file;
    }

    private static final String VALID = """
        name: "demo"
        seed: 42
        components:
          - { id: gateway, type: LoadBalancer }
          - { id: api, type: Service, capacity: 100, base_latency_ms: 50 }
          - { id: db, type: Database, max_connections: 50, read_latency_ms: 20 }
        connections:
          - { from: gateway, to: api }
          - { from: api, to: db }
        workload:
          requests_per_second: 100
          duration_seconds: 10
        """;

    @Test
    void loadsValidTopology() throws IOException {
        LoadedScenario scenario = loader.load(write(VALID));

        assertThat(scenario.seed()).isEqualTo(42L);
        assertThat(scenario.topology().name()).isEqualTo("demo");
        assertThat(scenario.topology().entryPointId()).isEqualTo("gateway");
        assertThat(scenario.workload().requestsPerSecond()).isEqualTo(100);
    }

    @Test
    void rejectsUnknownComponentType() throws IOException {
        String yaml = """
            name: t
            components:
              - { id: x, type: Wormhole }
            workload: { requests_per_second: 10, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> loader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class)
            .hasMessageContaining("Wormhole");
    }

    @Test
    void rejectsMissingRequiredField() throws IOException {
        String yaml = """
            name: t
            components:
              - { id: api, type: Service, capacity: 10 }
            workload: { requests_per_second: 10, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> loader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class)
            .hasMessageContaining("base_latency_ms");
    }

    @Test
    void rejectsDanglingConnectionReference() throws IOException {
        String yaml = """
            name: t
            components:
              - { id: api, type: Service, capacity: 10, base_latency_ms: 5 }
            connections:
              - { from: api, to: ghost }
            workload: { requests_per_second: 10, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> loader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void rejectsNegativeRange() throws IOException {
        String yaml = """
            name: t
            components:
              - { id: api, type: Service, capacity: 10, base_latency_ms: -5 }
            workload: { requests_per_second: 10, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> loader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class);
    }

    @Test
    void enforcesWorkloadLimits() throws IOException {
        SimulationLimits tight = new SimulationLimits(100, 5, 10, 1000, 1000);
        YamlTopologyLoader tightLoader = new YamlTopologyLoader(tight);
        String yaml = """
            name: t
            components:
              - { id: api, type: Service, capacity: 10, base_latency_ms: 5 }
            workload: { requests_per_second: 999, duration_seconds: 1 }
            """;

        assertThatThrownBy(() -> tightLoader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class)
            .hasMessageContaining("requests_per_second");
    }

    @Test
    void rejectsUnsafeYamlTags() throws IOException {
        String yaml = "name: !!java.net.URL [\"http://evil\"]\ncomponents: []\n";

        assertThatThrownBy(() -> loader.load(write(yaml)))
            .isInstanceOf(TopologyValidationException.class);
    }

    @Test
    void rejectsMissingFile() {
        assertThatThrownBy(() -> loader.load(tempDir.resolve("nope.yaml")))
            .isInstanceOf(TopologyValidationException.class)
            .hasMessageContaining("no existe");
    }
}
