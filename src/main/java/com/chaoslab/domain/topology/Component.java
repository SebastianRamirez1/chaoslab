package com.chaoslab.domain.topology;

/**
 * Contrato de un componente de la topología (directrices §1.2: frontera con contrato,
 * no "una clase suelta"). Añadir un tipo nuevo = implementar esta interfaz, sin tocar el motor.
 */
public interface Component {

    /** Identificador único dentro de la topología. */
    String id();

    /** Tipo de componente. */
    ComponentType type();

    /**
     * Intenta admitir un request. Si lo admite, reserva capacidad y devuelve la latencia
     * de proceso; si no, devuelve un rechazo con motivo.
     */
    Outcome receive(Request request);

    /** Libera la capacidad reservada por un request admitido, al terminar su proceso. */
    void release();

    /** Salud actual del componente. */
    Health health();

    /** Pico de requests concurrentes observado (proxy de profundidad de cola). */
    int maxInFlight();
}
