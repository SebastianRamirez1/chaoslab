package com.chaoslab.domain.engine;

/**
 * Reloj virtual de la simulación: tiempo lógico en milisegundos que solo avanza
 * hacia adelante, nunca en tiempo real de pared.
 *
 * <p>Es la base del determinismo del motor de eventos discretos (directrices §1.1,
 * atributo de Fiabilidad): el tiempo salta de evento a evento, así que la misma
 * topología con la misma semilla produce siempre el mismo resultado.
 *
 * <p>Pertenece al dominio puro: cero dependencias de Spring, web o YAML.
 */
public final class Clock {

    private long currentTimeMillis;

    /** Crea un reloj posicionado en el instante 0. */
    public Clock() {
        this(0L);
    }

    /**
     * Crea un reloj posicionado en {@code startMillis}.
     *
     * @param startMillis instante inicial en ms; debe ser &gt;= 0
     * @throws IllegalArgumentException si {@code startMillis} es negativo
     */
    public Clock(long startMillis) {
        if (startMillis < 0) {
            throw new IllegalArgumentException("startMillis debe ser >= 0, fue: " + startMillis);
        }
        this.currentTimeMillis = startMillis;
    }

    /**
     * Instante actual del reloj.
     *
     * @return el tiempo simulado en milisegundos
     */
    public long now() {
        return currentTimeMillis;
    }

    /**
     * Avanza el reloj hasta {@code millis}. El tiempo nunca retrocede.
     *
     * @param millis instante destino en ms; debe ser &gt;= {@link #now()}
     * @throws IllegalArgumentException si {@code millis} es anterior al instante actual
     */
    public void advanceTo(long millis) {
        if (millis < currentTimeMillis) {
            throw new IllegalArgumentException(
                "el reloj no puede retroceder: " + millis + " < " + currentTimeMillis);
        }
        this.currentTimeMillis = millis;
    }
}
