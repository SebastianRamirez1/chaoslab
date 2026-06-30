package com.chaoslab.domain.fault;

/** Validaciones comunes a las invariantes de los fallos. */
final class FaultValidation {

    private FaultValidation() {
    }

    static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("el id del fallo no puede estar vacío");
        }
    }

    static void requireTarget(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("el target del fallo no puede estar vacío");
        }
    }

    static void requireTiming(long atMillis, long durationMillis) {
        if (atMillis < 0) {
            throw new IllegalArgumentException("at debe ser >= 0, fue: " + atMillis);
        }
        if (durationMillis < 0) {
            throw new IllegalArgumentException("duration debe ser >= 0, fue: " + durationMillis);
        }
    }
}
