package com.chaoslab.domain.topology;

/** Base de datos: latencia de lectura fija y un límite de conexiones concurrentes. */
public final class Database extends AbstractComponent {

    private final long readLatencyMillis;

    public Database(String id, int maxConnections, long readLatencyMillis) {
        super(id, maxConnections);
        if (readLatencyMillis < 0) {
            throw new IllegalArgumentException(
                "read_latency_ms de '" + id + "' debe ser >= 0, fue: " + readLatencyMillis);
        }
        this.readLatencyMillis = readLatencyMillis;
    }

    @Override
    public ComponentType type() {
        return ComponentType.DATABASE;
    }

    @Override
    protected long latencyMillis() {
        return readLatencyMillis;
    }
}
