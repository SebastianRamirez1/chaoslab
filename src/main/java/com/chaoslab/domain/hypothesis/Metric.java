package com.chaoslab.domain.hypothesis;

import com.chaoslab.domain.metrics.SimulationReport;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/**
 * Indicador medible de una corrida contra el que se declara un invariante de estado estable
 * (steady-state hypothesis, principio formal de Chaos Engineering). Cada métrica sabe extraer
 * su valor de un {@link SimulationReport} ya calculado, de forma determinista.
 */
public enum Metric {

    SUCCESS_RATE("success_rate", SimulationReport::successRate),
    P50_LATENCY_MS("p50_latency_ms", r -> r.latency().p50()),
    P95_LATENCY_MS("p95_latency_ms", r -> r.latency().p95()),
    P99_LATENCY_MS("p99_latency_ms", r -> r.latency().p99()),
    MAX_LATENCY_MS("max_latency_ms", r -> r.latency().max()),
    COMPLETED_REQUESTS("completed_requests", SimulationReport::completedRequests),
    FAILED_REQUESTS("failed_requests", SimulationReport::failedRequests),
    GENERATED_REQUESTS("generated_requests", SimulationReport::generatedRequests);

    private final String key;
    private final ToDoubleFunction<SimulationReport> extractor;

    Metric(String key, ToDoubleFunction<SimulationReport> extractor) {
        this.key = key;
        this.extractor = extractor;
    }

    /** Clave estable usada en el YAML (entrada no confiable) y en la salida JSON. */
    public String key() {
        return key;
    }

    /** Valor de la métrica para un reporte dado. */
    public double extract(SimulationReport report) {
        return extractor.applyAsDouble(report);
    }

    /** Resuelve una métrica por su clave de YAML; lanza {@link IllegalArgumentException} si no existe. */
    public static Metric fromKey(String rawKey) {
        String normalized = rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
        for (Metric metric : values()) {
            if (metric.key.equals(normalized)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("métrica desconocida: '" + rawKey + "' (válidas: "
            + "success_rate, p50_latency_ms, p95_latency_ms, p99_latency_ms, max_latency_ms, "
            + "completed_requests, failed_requests, generated_requests)");
    }
}
