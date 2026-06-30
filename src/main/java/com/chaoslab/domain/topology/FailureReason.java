package com.chaoslab.domain.topology;

/** Causa por la que un request falla. Permite medir el efecto de cada fallo inyectado. */
public enum FailureReason {
    /** El componente rechazó por capacidad agotada. */
    CAPACITY,
    /** El componente está caído (CrashFault). */
    CRASH,
    /** La comunicación quedó cortada por una partición de red. */
    NETWORK_PARTITION
}
