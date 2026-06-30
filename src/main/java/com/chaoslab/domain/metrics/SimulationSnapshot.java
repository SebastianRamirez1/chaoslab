package com.chaoslab.domain.metrics;

import java.util.List;

/**
 * Foto de la simulación en un instante: métricas acumuladas hasta ese momento y el estado de
 * cada componente. La secuencia de snapshots es la "línea de tiempo" que el dashboard reproduce.
 *
 * @param atMillis        instante simulado (ms)
 * @param completedSoFar  requests completados hasta el instante
 * @param failedSoFar     requests fallidos hasta el instante
 * @param latencyP95Millis percentil 95 de latencia hasta el instante
 * @param components      estado de cada componente en el instante
 */
public record SimulationSnapshot(long atMillis, long completedSoFar, long failedSoFar,
                                 long latencyP95Millis, List<ComponentSnapshot> components) {

    public SimulationSnapshot {
        components = components == null ? List.of() : List.copyOf(components);
    }
}
