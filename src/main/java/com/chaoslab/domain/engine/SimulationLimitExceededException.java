package com.chaoslab.domain.engine;

/** Se lanza cuando la simulación supera un límite duro (eventos o requests), evitando consumo ilimitado. */
public class SimulationLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SimulationLimitExceededException(String message) {
        super(message);
    }
}
