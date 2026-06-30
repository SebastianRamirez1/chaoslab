package com.chaoslab.domain.engine;

/**
 * Evento de la simulación. Tipo sellado: el conjunto de eventos es cerrado y conocido,
 * lo que permite procesarlos con {@code switch} exhaustivo (Java 21) sin rama {@code default}.
 */
public sealed interface Event
    permits RequestArrived, RequestDeparted, RequestCompleted, RequestFailed, FaultInjected, FaultCleared {

    /** Instante simulado (ms) en que ocurre el evento. */
    long timestampMillis();
}
