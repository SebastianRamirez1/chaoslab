package com.chaoslab.domain.topology;

import com.chaoslab.domain.resilience.ResiliencePolicy;

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
     * de proceso; si no, devuelve un rechazo con su causa.
     */
    Outcome receive(Request request);

    /** Libera la capacidad reservada por un request admitido, al terminar su proceso. */
    void release();

    /** Salud actual del componente. */
    Health health();

    /** Pico de requests concurrentes observado (proxy de profundidad de cola). */
    int maxInFlight();

    /** Requests actualmente en vuelo (profundidad instantánea, para snapshots en el tiempo). */
    int currentInFlight();

    /** Marca el componente como caído: rechazará todo (CrashFault). */
    void crash();

    /** Restaura un componente caído. */
    void recover();

    /** Suma latencia de proceso inyectada por un fallo (LatencyFault). */
    void addLatency(long extraMillis);

    /** Quita latencia de proceso previamente inyectada. */
    void removeLatency(long extraMillis);

    /** Políticas de resiliencia que el componente aplica a sus llamadas salientes. */
    ResiliencePolicy resilience();
}
