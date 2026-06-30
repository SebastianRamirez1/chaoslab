package com.chaoslab.domain.topology;

import com.chaoslab.domain.resilience.ResiliencePolicy;

/**
 * Base de los componentes con capacidad acotada: lleva la cuenta de requests en vuelo,
 * rechaza cuando se supera la capacidad y registra el pico de concurrencia. También aplica
 * los efectos de los fallos inyectados (caída y latencia extra).
 *
 * <p>La latencia base es fija por tipo (determinista). La aleatoriedad del motor vive solo
 * en la generación del workload, para no romper la Fiabilidad (#1).
 */
public abstract class AbstractComponent implements Component {

    private final String id;
    private final int capacity;
    private int inFlight;
    private int maxInFlight;
    private boolean down;
    private long injectedLatencyMillis;
    private ResiliencePolicy resilience = ResiliencePolicy.none();

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
        if (down) {
            return Outcome.rejected(FailureReason.CRASH);
        }
        if (inFlight >= capacity) {
            return Outcome.rejected(FailureReason.CAPACITY);
        }
        inFlight++;
        if (inFlight > maxInFlight) {
            maxInFlight = inFlight;
        }
        return Outcome.accepted(latencyMillis() + injectedLatencyMillis);
    }

    @Override
    public final void release() {
        if (inFlight > 0) {
            inFlight--;
        }
    }

    @Override
    public Health health() {
        if (down) {
            return Health.DOWN;
        }
        return inFlight >= capacity ? Health.DEGRADED : Health.UP;
    }

    @Override
    public final int maxInFlight() {
        return maxInFlight;
    }

    @Override
    public final int currentInFlight() {
        return inFlight;
    }

    @Override
    public final void crash() {
        this.down = true;
    }

    @Override
    public final void recover() {
        this.down = false;
    }

    @Override
    public final void addLatency(long extraMillis) {
        if (extraMillis < 0) {
            throw new IllegalArgumentException("la latencia extra debe ser >= 0, fue: " + extraMillis);
        }
        this.injectedLatencyMillis += extraMillis;
    }

    @Override
    public final void removeLatency(long extraMillis) {
        this.injectedLatencyMillis = Math.max(0L, this.injectedLatencyMillis - extraMillis);
    }

    @Override
    public final ResiliencePolicy resilience() {
        return resilience;
    }

    /** Asigna las políticas de resiliencia del componente (lo usa el loader). */
    public final void configureResilience(ResiliencePolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("la política de resiliencia no puede ser null");
        }
        this.resilience = policy;
    }

    protected final int capacity() {
        return capacity;
    }

    /** Latencia base de proceso del componente, en milisegundos. */
    protected abstract long latencyMillis();
}
