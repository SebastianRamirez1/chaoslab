package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.Request;

/** Un request fue rechazado en un componente (p. ej. por capacidad). */
public record RequestFailed(long timestampMillis, Request request, String componentId, String reason)
    implements Event {
}
