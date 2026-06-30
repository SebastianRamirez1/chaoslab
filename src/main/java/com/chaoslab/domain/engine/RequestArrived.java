package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.Request;

/**
 * Un request llega a un componente y pide ser admitido.
 *
 * @param timestampMillis instante del evento
 * @param request         el request
 * @param componentId     componente destino de esta llamada
 * @param fromId          componente llamador (null si es la entrada del workload, sin llamador)
 * @param attempt         número de intento (0 = primero); lo incrementan los reintentos
 */
public record RequestArrived(long timestampMillis, Request request, String componentId, String fromId, int attempt)
    implements Event {

    /** Llegada a la entrada del sistema (sin llamador, primer intento). */
    public RequestArrived(long timestampMillis, Request request, String componentId) {
        this(timestampMillis, request, componentId, null, 0);
    }
}
