package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.Request;

/** Un request llega a un componente y pide ser admitido. */
public record RequestArrived(long timestampMillis, Request request, String componentId) implements Event {
}
