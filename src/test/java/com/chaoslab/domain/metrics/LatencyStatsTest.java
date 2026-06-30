package com.chaoslab.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Cálculo determinista de percentiles por rango más cercano. */
class LatencyStatsTest {

    @Test
    void computesPercentiles() {
        LatencyStats stats = LatencyStats.from(List.of(10L, 20L, 30L, 40L, 50L));

        assertThat(stats.count()).isEqualTo(5L);
        assertThat(stats.min()).isEqualTo(10L);
        assertThat(stats.max()).isEqualTo(50L);
        assertThat(stats.p50()).isEqualTo(30L);
        assertThat(stats.p95()).isEqualTo(50L);
        assertThat(stats.p99()).isEqualTo(50L);
    }

    @Test
    void emptySamplesYieldZeros() {
        LatencyStats stats = LatencyStats.from(List.of());

        assertThat(stats.count()).isZero();
        assertThat(stats.p99()).isZero();
    }
}
