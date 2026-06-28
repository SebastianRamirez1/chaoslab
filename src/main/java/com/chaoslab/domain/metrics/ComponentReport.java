package com.chaoslab.domain.metrics;

import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.Health;

/**
 * Estado de un componente al final de la corrida.
 *
 * @param id          id del componente
 * @param type        tipo
 * @param arrived     requests que llegaron al componente
 * @param rejected    requests que rechazó
 * @param maxInFlight pico de concurrencia observado (proxy de profundidad de cola)
 * @param health      salud final
 */
public record ComponentReport(String id, ComponentType type, long arrived, long rejected,
                              int maxInFlight, Health health) {
}
