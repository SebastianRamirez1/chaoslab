package com.chaoslab.infrastructure.yaml;

/** Error de validación de la topología de entrada, con un mensaje claro de qué está mal. */
public class TopologyValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TopologyValidationException(String message) {
        super(message);
    }

    public TopologyValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
