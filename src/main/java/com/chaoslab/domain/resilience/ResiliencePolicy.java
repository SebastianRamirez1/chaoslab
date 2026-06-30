package com.chaoslab.domain.resilience;

import java.util.HashMap;
import java.util.Map;

/**
 * Políticas de resiliencia que un componente aplica a SUS llamadas salientes (hacia sus
 * destinos): timeout, reintentos y circuit breaker. El "momento ajá" del proyecto: con un
 * breaker, el sistema degrada en vez de colapsar cuando un destino cae.
 *
 * <p>Cada par (llamador, destino) tiene su propio breaker, creado de forma perezosa.
 */
public final class ResiliencePolicy {

    private final int timeoutMillis;
    private final int maxAttempts;
    private final int breakerThreshold;
    private final long breakerCooldownMillis;
    private final Map<String, CircuitBreaker> breakers = new HashMap<>();

    /**
     * @param timeoutMillis         timeout de la llamada en ms; 0 = sin timeout
     * @param maxAttempts           intentos totales por request; 1 = sin reintento
     * @param breakerThreshold      fallos consecutivos para abrir el breaker; 0 = sin breaker
     * @param breakerCooldownMillis enfriamiento del breaker en ms
     */
    public ResiliencePolicy(int timeoutMillis, int maxAttempts, int breakerThreshold, long breakerCooldownMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeout_ms debe ser >= 0, fue: " + timeoutMillis);
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("max_attempts debe ser >= 1, fue: " + maxAttempts);
        }
        if (breakerThreshold < 0) {
            throw new IllegalArgumentException("threshold debe ser >= 0, fue: " + breakerThreshold);
        }
        if (breakerCooldownMillis < 0) {
            throw new IllegalArgumentException("cooldown_ms debe ser >= 0, fue: " + breakerCooldownMillis);
        }
        this.timeoutMillis = timeoutMillis;
        this.maxAttempts = maxAttempts;
        this.breakerThreshold = breakerThreshold;
        this.breakerCooldownMillis = breakerCooldownMillis;
    }

    /** Política vacía (sin timeout, sin reintentos, sin breaker): comportamiento pass-through. */
    public static ResiliencePolicy none() {
        return new ResiliencePolicy(0, 1, 0, 0);
    }

    public boolean hasTimeout() {
        return timeoutMillis > 0;
    }

    public int timeoutMillis() {
        return timeoutMillis;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean hasBreaker() {
        return breakerThreshold > 0;
    }

    /** Breaker para un destino concreto (creado perezosamente). */
    public CircuitBreaker breakerFor(String targetId) {
        return breakers.computeIfAbsent(targetId, k -> new CircuitBreaker(breakerThreshold, breakerCooldownMillis));
    }
}
