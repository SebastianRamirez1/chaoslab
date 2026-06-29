package com.chaoslab.domain.topology;

/**
 * Resultado de que un componente intente admitir un request.
 *
 * @param accepted      true si el componente reservó capacidad y lo procesará
 * @param latencyMillis latencia de proceso si fue admitido (0 si fue rechazado)
 * @param reason        causa del rechazo, o {@code null} si fue admitido
 */
public record Outcome(boolean accepted, long latencyMillis, FailureReason reason) {

    public Outcome {
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis debe ser >= 0, fue: " + latencyMillis);
        }
        if (!accepted && reason == null) {
            throw new IllegalArgumentException("un rechazo debe indicar una causa (FailureReason)");
        }
    }

    /** Admisión con la latencia de proceso dada. */
    public static Outcome accepted(long latencyMillis) {
        return new Outcome(true, latencyMillis, null);
    }

    /** Rechazo con la causa dada. */
    public static Outcome rejected(FailureReason reason) {
        return new Outcome(false, 0L, reason);
    }
}
