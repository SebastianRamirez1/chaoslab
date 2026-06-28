package com.chaoslab.domain.topology;

/**
 * Base de los componentes con capacidad acotada: lleva la cuenta de requests en vuelo,
 * rechaza cuando se supera la capacidad y registra el pico de concurrencia.
 *
 * <p>La latencia de proceso es fija por tipo (determinista). La aleatoriedad del motor
 * vive solo en la generación del workload, para no romper la Fiabilidad (#1).
 */
public abstract class AbstractComponent implements Component {

    private final String id;
    private final int capacity;
    private int inFlight;
    private int maxInFlight;

    protected AbstractComponent(String id, int capacity) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("el id del componente no puede estar vacío");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("la capacidad de '" + id + "' debe ser > 0, fue: " + capacity);
        }
        this.id = id;
        this.capacity = capacity;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final Outcome receive(Request request) {
        if (inFlight >= capacity) {
            return Outcome.rejected("capacidad excedida en '" + id + "' (capacidad=" + capacity + ")");
        }
        inFlight++;
        if (inFlight > maxInFlight) {
            maxInFlight = inFlight;
        }
        return Outcome.accepted(latencyMillis());
    }

    @Override
    public final void release() {
        if (inFlight > 0) {
            inFlight--;
        }
    }

    @Override
    public Health health() {
        return inFlight >= capacity ? Health.DEGRADED : Health.UP;
    }

    @Override
    public final int maxInFlight() {
        return maxInFlight;
    }

    protected final int capacity() {
        return capacity;
    }

    /** Latencia de proceso del componente, en milisegundos. */
    protected abstract long latencyMillis();
}
