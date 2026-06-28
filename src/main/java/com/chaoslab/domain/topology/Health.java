package com.chaoslab.domain.topology;

/** Estado de salud de un componente, tal como lo pinta el dashboard. */
public enum Health {
    /** Operativo y con capacidad libre. */
    UP,
    /** Operativo pero saturado (sin capacidad para nuevos requests). */
    DEGRADED,
    /** Caído: no responde (reservado para los fallos de la Fase 2). */
    DOWN
}
