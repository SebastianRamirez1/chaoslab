package com.chaoslab.domain.resilience;

/** Estado de un circuit breaker. */
public enum CircuitBreakerState {
    /** Cerrado: las llamadas pasan normalmente. */
    CLOSED,
    /** Abierto: las llamadas se cortan (fail-fast) durante el enfriamiento. */
    OPEN,
    /** Semiabierto: pasó el enfriamiento; se permite una llamada de prueba. */
    HALF_OPEN
}
