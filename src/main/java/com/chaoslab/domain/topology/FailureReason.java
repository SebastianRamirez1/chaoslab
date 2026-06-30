package com.chaoslab.domain.topology;

/** Causa por la que un request falla. Permite medir el efecto de cada fallo y defensa. */
public enum FailureReason {
    /** El componente rechazó por capacidad agotada. */
    CAPACITY,
    /** El componente está caído (CrashFault). */
    CRASH,
    /** La comunicación quedó cortada por una partición de red. */
    NETWORK_PARTITION,
    /** La llamada superó el timeout configurado por el llamador. */
    TIMEOUT,
    /** El circuit breaker del llamador está abierto: se cortó la llamada (fail-fast). */
    CIRCUIT_OPEN
}
