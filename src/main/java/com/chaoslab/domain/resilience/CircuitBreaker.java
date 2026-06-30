package com.chaoslab.domain.resilience;

/**
 * Circuit breaker: tras {@code threshold} fallos consecutivos se abre y corta las llamadas
 * (fail-fast) durante {@code cooldownMillis}; pasado el enfriamiento permite una llamada de
 * prueba (semiabierto) y, según su resultado, se cierra o se vuelve a abrir.
 *
 * <p>Determinista: el tiempo lo provee el reloj virtual del motor (sin relojes de pared).
 */
public final class CircuitBreaker {

    private final int threshold;
    private final long cooldownMillis;
    private int consecutiveFailures;
    private boolean open;
    private long openedAtMillis;

    public CircuitBreaker(int threshold, long cooldownMillis) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold debe ser > 0, fue: " + threshold);
        }
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldown_ms debe ser >= 0, fue: " + cooldownMillis);
        }
        this.threshold = threshold;
        this.cooldownMillis = cooldownMillis;
    }

    /** ¿Se permite una llamada en este instante? (sin efectos secundarios) */
    public boolean isCallPermitted(long nowMillis) {
        if (!open) {
            return true;
        }
        return nowMillis - openedAtMillis >= cooldownMillis;
    }

    /** Registra una llamada exitosa: cierra el breaker y reinicia el conteo. */
    public void recordSuccess() {
        this.open = false;
        this.consecutiveFailures = 0;
    }

    /** Registra una llamada fallida: abre el breaker si se alcanza el umbral. */
    public void recordFailure(long nowMillis) {
        this.consecutiveFailures++;
        if (consecutiveFailures >= threshold) {
            this.open = true;
            this.openedAtMillis = nowMillis;
        }
    }

    /** Estado observable del breaker en este instante. */
    public CircuitBreakerState state(long nowMillis) {
        if (!open) {
            return CircuitBreakerState.CLOSED;
        }
        return nowMillis - openedAtMillis >= cooldownMillis
            ? CircuitBreakerState.HALF_OPEN
            : CircuitBreakerState.OPEN;
    }
}
