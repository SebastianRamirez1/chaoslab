package com.chaoslab.infrastructure.web;

import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.metrics.ResilienceMetrics;
import com.chaoslab.domain.metrics.SimulationReport;

/**
 * Respuesta del endpoint de simulación: la estructura de la topología (para el grafo), el reporte
 * completo con su línea de tiempo (para reproducir la animación en el cliente), el veredicto de la
 * hipótesis de estado estable y las métricas de resiliencia derivadas.
 *
 * @param topology   estructura del grafo
 * @param report     reporte y timeline de la corrida
 * @param hypothesis veredicto PASA/FALLA de la hipótesis (o "no declarada")
 * @param resilience métricas de resiliencia (disponibilidad, MTTR, degradación, detección)
 */
public record SimulationResponse(TopologyView topology, SimulationReport report,
                                 HypothesisReport hypothesis, ResilienceMetrics resilience) {
}
