package com.chaoslab.domain.topology;

/** Servicio que procesa requests con una latencia base y una capacidad de concurrencia. */
public final class Service extends AbstractComponent {

    private final long baseLatencyMillis;

    public Service(String id, int capacity, long baseLatencyMillis) {
        super(id, capacity);
        if (baseLatencyMillis < 0) {
            throw new IllegalArgumentException(
                "base_latency_ms de '" + id + "' debe ser >= 0, fue: " + baseLatencyMillis);
        }
        this.baseLatencyMillis = baseLatencyMillis;
    }

    @Override
    public ComponentType type() {
        return ComponentType.SERVICE;
    }

    @Override
    protected long latencyMillis() {
        return baseLatencyMillis;
    }
}
