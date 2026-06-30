package com.chaoslab.domain.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Validación estructural e invariantes del grafo de topología. */
class TopologyGraphTest {

    private static Service service(String id) {
        return new Service(id, 10, 5L);
    }

    @Test
    void detectsSingleEntryPoint() {
        TopologyGraph graph = TopologyGraph.of(
            "t",
            List.of(new LoadBalancer("lb"), service("a"), service("b")),
            List.of(new Connection("lb", "a"), new Connection("lb", "b")));

        assertThat(graph.entryPointId()).isEqualTo("lb");
    }

    @Test
    void loadBalancerRoutesRoundRobinOthersFanOut() {
        TopologyGraph balanced = TopologyGraph.of(
            "t",
            List.of(new LoadBalancer("lb"), service("a"), service("b")),
            List.of(new Connection("lb", "a"), new Connection("lb", "b")));
        assertThat(balanced.route("lb")).containsExactly("a");
        assertThat(balanced.route("lb")).containsExactly("b");

        TopologyGraph fanOut = TopologyGraph.of(
            "t",
            List.of(service("s"), new MessageQueue("q1", 10), new MessageQueue("q2", 10)),
            List.of(new Connection("s", "q1"), new Connection("s", "q2")));
        assertThat(fanOut.route("s")).containsExactly("q1", "q2");
    }

    @Test
    void terminalComponentRoutesNowhere() {
        TopologyGraph graph = TopologyGraph.of(
            "t", List.of(service("only")), List.of());

        assertThat(graph.route("only")).isEmpty();
    }

    @Test
    void rejectsConnectionToUnknownComponent() {
        assertThatThrownBy(() -> TopologyGraph.of(
            "t", List.of(service("a")), List.of(new Connection("a", "ghost"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> TopologyGraph.of(
            "t", List.of(service("a"), service("a")), List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicado");
    }

    @Test
    void rejectsMultipleEntryPoints() {
        assertThatThrownBy(() -> TopologyGraph.of(
            "t", List.of(service("a"), service("b")), List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("punto de entrada");
    }

    @Test
    void rejectsCycleWithoutEntryPoint() {
        assertThatThrownBy(() -> TopologyGraph.of(
            "t", List.of(service("a"), service("b")),
            List.of(new Connection("a", "b"), new Connection("b", "a"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("punto de entrada");
    }
}
