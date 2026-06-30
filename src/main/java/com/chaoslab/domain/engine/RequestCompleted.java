package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.Request;

/** Un request alcanzó un componente terminal: completó su recorrido con éxito. */
public record RequestCompleted(long timestampMillis, Request request) implements Event {
}
