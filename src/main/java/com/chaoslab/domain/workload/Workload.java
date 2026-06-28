package com.chaoslab.domain.workload;

/**
 * Definición del tráfico a generar contra la topología.
 *
 * @param requestsPerSecond tasa de llegada de requests (&gt; 0)
 * @param durationSeconds   duración de la generación, en segundos (&gt; 0)
 */
public record Workload(int requestsPerSecond, int durationSeconds) {

    public Workload {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("requests_per_second debe ser > 0, fue: " + requestsPerSecond);
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("duration_seconds debe ser > 0, fue: " + durationSeconds);
        }
    }

    /** Ventana de generación en milisegundos. */
    public long windowMillis() {
        return durationSeconds * 1000L;
    }
}
