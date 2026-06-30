package com.chaoslab.domain.metrics;

import com.chaoslab.domain.topology.FailureReason;
import java.util.List;
import java.util.Map;

/**
 * Resultado final y reproducible de una corrida. Es un value object inmutable: dos corridas con
 * la misma topología y la misma semilla producen reportes {@code equals} (test de determinismo).
 *
 * @param topologyName            nombre de la topología
 * @param seed                    semilla usada
 * @param simulatedDurationMillis tiempo simulado transcurrido (ms)
 * @param generatedRequests       requests generados por el workload
 * @param completedRequests       requests que completaron con éxito
 * @param failedRequests          requests que fallaron
 * @param successRate             tasa de éxito en [0,1]
 * @param latency                 percentiles de latencia extremo a extremo
 * @param components              estado por componente
 * @param failuresByReason        cantidad de fallos por causa
 */
public record SimulationReport(String topologyName, long seed, long simulatedDurationMillis,
                               long generatedRequests, long completedRequests, long failedRequests,
                               double successRate, LatencyStats latency, List<ComponentReport> components,
                               Map<FailureReason, Long> failuresByReason) {

    public SimulationReport {
        components = components == null ? List.of() : List.copyOf(components);
        failuresByReason = failuresByReason == null ? Map.of() : Map.copyOf(failuresByReason);
    }
}
