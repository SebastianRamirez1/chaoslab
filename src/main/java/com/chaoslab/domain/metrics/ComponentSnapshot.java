package com.chaoslab.domain.metrics;

import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.Health;

/**
 * Estado de un componente en un instante concreto de la simulación (para animar el dashboard).
 *
 * @param id       id del componente
 * @param type     tipo
 * @param health   salud en ese instante (UP/DEGRADED/DOWN)
 * @param inFlight requests en vuelo en ese instante (profundidad)
 */
public record ComponentSnapshot(String id, ComponentType type, Health health, int inFlight) {
}
