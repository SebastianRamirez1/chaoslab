package com.chaoslab.domain.engine;

import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.Request;

/** Un request falló en un componente, con su causa categorizada. */
public record RequestFailed(long timestampMillis, Request request, String componentId, FailureReason reason)
    implements Event {
}
