package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.Request;

/** Un request terminó de procesarse en un componente; se libera capacidad y se enruta. */
public record RequestDeparted(long timestampMillis, Request request, String componentId) implements Event {
}
