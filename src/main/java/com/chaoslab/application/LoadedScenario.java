package com.chaoslab.application;

import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.Workload;
import java.util.List;

/**
 * Escenario listo para simular, ya parseado y validado desde una fuente externa (YAML).
 *
 * @param topology grafo de la topología
 * @param workload tráfico a generar
 * @param seed     semilla de reproducibilidad declarada en la fuente
 * @param faults   fallos predefinidos a inyectar durante la corrida
 */
public record LoadedScenario(TopologyGraph topology, Workload workload, long seed, List<Fault> faults) {

    public LoadedScenario {
        faults = faults == null ? List.of() : List.copyOf(faults);
    }
}
