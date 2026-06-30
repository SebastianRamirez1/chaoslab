package com.chaoslab.domain.engine;

import com.chaoslab.domain.fault.Fault;

/** Se inyecta un fallo en la topología en este instante. */
public record FaultInjected(long timestampMillis, Fault fault) implements Event {
}
