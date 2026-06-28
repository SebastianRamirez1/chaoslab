package com.chaoslab.domain.topology;

/**
 * Resultado de que un componente intente admitir un request.
 *
 * @param accepted        true si el componente reservó capacidad y lo procesará
 * @param latencyMillis   latencia de proceso si fue admitido (0 si fue rechazado)
 * @param rejectionReason motivo del rechazo, o cadena vacía si fue admitido
 */
public record Outcome(boolean accepted, long latencyMillis, String rejectionReason) {

    public Outcome {
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis debe ser >= 0, fue: " + latencyMillis);
        }
        if (rejectionReason == null) {
            throw new IllegalArgumentException("rejectionReason no puede ser null (usa cadena vacía)");
        }
    }

    /** Admisión con la latencia de proceso dada. */
    public static Outcome accepted(long latencyMillis) {
        return new Outcome(true, latencyMillis, "");
    }

    /** Rechazo con el motivo dado. */
    public static Outcome rejected(String reason) {
        return new Outcome(false, 0L, reason);
    }
}
