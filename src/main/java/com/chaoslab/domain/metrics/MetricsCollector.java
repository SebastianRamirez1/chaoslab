package com.chaoslab.domain.metrics;

import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recolecta el estado interno de la simulación a medida que ocurren los eventos y produce el
 * {@link SimulationReport} final. Es la observabilidad del dominio (directrices §1.3).
 */
public final class MetricsCollector {

    private static final int ARRIVED = 0;
    private static final int REJECTED = 1;

    private long completed;
    private long failed;
    private final List<Long> latencies = new ArrayList<>();
    private final Map<String, long[]> perComponent = new LinkedHashMap<>();
    private final Map<FailureReason, Long> failuresByReason = new EnumMap<>(FailureReason.class);

    /** Registra que un request llegó a un componente. */
    public void recordArrival(String componentId) {
        counters(componentId)[ARRIVED]++;
    }

    /** Registra que un request falló en un componente, con su causa. */
    public void recordFailure(String componentId, FailureReason reason) {
        failed++;
        counters(componentId)[REJECTED]++;
        failuresByReason.merge(reason, 1L, Long::sum);
    }

    /** Registra que un request completó su recorrido con la latencia extremo a extremo dada. */
    public void recordCompletion(long latencyMillis) {
        completed++;
        latencies.add(latencyMillis);
    }

    private long[] counters(String componentId) {
        return perComponent.computeIfAbsent(componentId, k -> new long[2]);
    }

    /** Construye el reporte final leyendo también el estado de cada componente de la topología. */
    public SimulationReport report(TopologyGraph topology, long simEndMillis, long generatedRequests, long seed) {
        List<ComponentReport> componentReports = new ArrayList<>();
        for (Component component : topology.components()) {
            long[] ctr = perComponent.getOrDefault(component.id(), new long[2]);
            componentReports.add(new ComponentReport(
                component.id(), component.type(), ctr[ARRIVED], ctr[REJECTED],
                component.maxInFlight(), component.health()));
        }
        long total = completed + failed;
        double successRate = total == 0 ? 0.0 : (double) completed / total;
        return new SimulationReport(topology.name(), seed, simEndMillis, generatedRequests,
            completed, failed, successRate, LatencyStats.from(latencies), componentReports, failuresByReason);
    }
}
