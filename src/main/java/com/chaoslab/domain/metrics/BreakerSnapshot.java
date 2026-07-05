package com.chaoslab.domain.metrics;

import com.chaoslab.domain.resilience.CircuitBreakerState;

/**
 * Estado de un circuit breaker (del llamador hacia un destino) en un instante de la simulación.
 * Permite visualizar en el dashboard cuándo un breaker se abre y el sistema deja de enrutar ahí.
 *
 * @param fromId componente llamador (dueño del breaker)
 * @param toId   destino protegido por el breaker
 * @param state  estado en ese instante (CLOSED / OPEN / HALF_OPEN)
 */
public record BreakerSnapshot(String fromId, String toId, CircuitBreakerState state) {
}
