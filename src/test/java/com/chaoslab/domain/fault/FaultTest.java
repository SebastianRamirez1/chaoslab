package com.chaoslab.domain.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Database;
import com.chaoslab.domain.topology.Health;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Cada fallo aplica y revierte su efecto sobre la topología. */
class FaultTest {

    private static TopologyGraph topology() {
        return TopologyGraph.of(
            "t",
            List.of(new Service("api", 10, 5L), new Database("db", 5, 2L)),
            List.of(new Connection("api", "db")));
    }

    @Test
    void crashFaultTakesComponentDownAndBack() {
        TopologyGraph topology = topology();
        CrashFault fault = new CrashFault("c", "db", 0L, 0L);

        fault.apply(topology);
        assertThat(topology.component("db").health()).isEqualTo(Health.DOWN);

        fault.clear(topology);
        assertThat(topology.component("db").health()).isEqualTo(Health.UP);
    }

    @Test
    void latencyFaultAddsAndRemovesLatency() {
        TopologyGraph topology = topology();
        LatencyFault fault = new LatencyFault("l", "api", 0L, 0L, 100L);

        fault.apply(topology);
        assertThat(topology.component("api").receive(new com.chaoslab.domain.topology.Request(0, 0L)).latencyMillis())
            .isEqualTo(105L);

        fault.clear(topology);
        assertThat(topology.component("api").receive(new com.chaoslab.domain.topology.Request(1, 0L)).latencyMillis())
            .isEqualTo(5L);
    }

    @Test
    void networkPartitionCutsReachability() {
        TopologyGraph topology = topology();
        NetworkPartition fault = new NetworkPartition("p", Set.of("api"), Set.of("db"), 0L, 0L);

        fault.apply(topology);
        assertThat(topology.isReachable("api", "db")).isFalse();

        fault.clear(topology);
        assertThat(topology.isReachable("api", "db")).isTrue();
    }

    @Test
    void exposesTargetsForValidation() {
        assertThat(new CrashFault("c", "db", 0L, 0L).targets()).containsExactly("db");
        assertThat(new NetworkPartition("p", Set.of("api"), Set.of("db"), 0L, 0L).targets())
            .containsExactlyInAnyOrder("api", "db");
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new CrashFault("", "db", 0L, 0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrashFault("c", "db", -1L, 0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LatencyFault("l", "api", 0L, 0L, -5L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NetworkPartition("p", Set.of(), Set.of("db"), 0L, 0L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
