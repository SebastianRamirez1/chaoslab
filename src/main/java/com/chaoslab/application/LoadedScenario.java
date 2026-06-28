package com.chaoslab.application;

import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.Workload;

/**
 * Escenario listo para simular, ya parseado y validado desde una fuente externa (YAML).
 *
 * @param topology grafo de la topología
 * @param workload tráfico a generar
 * @param seed     semilla de reproducibilidad declarada en la fuente
 */
public record LoadedScenario(TopologyGraph topology, Workload workload, long seed) {
}
