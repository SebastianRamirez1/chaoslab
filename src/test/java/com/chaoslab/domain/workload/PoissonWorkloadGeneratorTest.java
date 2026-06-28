package com.chaoslab.domain.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.engine.SimulationLimitExceededException;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** El generador de workload debe ser reproducible y respetar el límite duro de requests. */
class PoissonWorkloadGeneratorTest {

    private static SimulationEngine newEngine() {
        TopologyGraph topology = TopologyGraph.of(
            "t", List.of(new Service("svc", 1_000_000, 1L)), List.of());
        return new SimulationEngine(topology, new MetricsCollector(), 50_000_000L);
    }

    @Test
    void sameSeedGeneratesSameCount() {
        PoissonWorkloadGenerator gen1 = new PoissonWorkloadGenerator(new Random(42));
        PoissonWorkloadGenerator gen2 = new PoissonWorkloadGenerator(new Random(42));

        long count1 = gen1.scheduleArrivals(newEngine(), new Workload(200, 10), "svc", 5_000_000L);
        long count2 = gen2.scheduleArrivals(newEngine(), new Workload(200, 10), "svc", 5_000_000L);

        assertThat(count1).isPositive();
        assertThat(count1).isEqualTo(count2);
    }

    @Test
    void enforcesMaxRequestsLimit() {
        PoissonWorkloadGenerator generator = new PoissonWorkloadGenerator(new Random(1));

        assertThatThrownBy(() ->
            generator.scheduleArrivals(newEngine(), new Workload(1000, 60), "svc", 5L))
            .isInstanceOf(SimulationLimitExceededException.class);
    }
}
