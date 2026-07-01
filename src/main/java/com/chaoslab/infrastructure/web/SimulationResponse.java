package com.chaoslab.infrastructure.web;

import com.chaoslab.domain.metrics.SimulationReport;

/**
 * Respuesta del endpoint de simulación: la estructura de la topología (para el grafo) y el
 * reporte completo con su línea de tiempo (para reproducir la animación en el cliente).
 *
 * @param topology estructura del grafo
 * @param report   reporte y timeline de la corrida
 */
public record SimulationResponse(TopologyView topology, SimulationReport report) {
}
