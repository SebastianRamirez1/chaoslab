package com.chaoslab.domain.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resumen de latencias extremo a extremo (ms) de los requests completados.
 *
 * @param count cantidad de muestras
 * @param min   latencia mínima
 * @param p50   percentil 50 (mediana)
 * @param p95   percentil 95
 * @param p99   percentil 99
 * @param max   latencia máxima
 */
public record LatencyStats(long count, long min, long p50, long p95, long p99, long max) {

    private static final LatencyStats EMPTY = new LatencyStats(0, 0, 0, 0, 0, 0);

    /** Calcula los percentiles por el método de rango más cercano (determinista). */
    public static LatencyStats from(List<Long> samples) {
        if (samples == null || samples.isEmpty()) {
            return EMPTY;
        }
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int n = sorted.size();
        return new LatencyStats(
            n,
            sorted.get(0),
            percentile(sorted, 50),
            percentile(sorted, 95),
            percentile(sorted, 99),
            sorted.get(n - 1));
    }

    private static long percentile(List<Long> sorted, int p) {
        int n = sorted.size();
        int rank = (int) Math.ceil(p / 100.0 * n);
        int index = Math.min(Math.max(rank, 1), n) - 1;
        return sorted.get(index);
    }
}
