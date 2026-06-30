package com.chaoslab.domain.engine;

import com.chaoslab.domain.fault.Fault;

/** Se revierte un fallo previamente inyectado (al cumplirse su duración). */
public record FaultCleared(long timestampMillis, Fault fault) implements Event {
}
