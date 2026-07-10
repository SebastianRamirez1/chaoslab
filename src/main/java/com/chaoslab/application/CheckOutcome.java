package com.chaoslab.application;

/**
 * Resultado de verificar un escenario en el gate de resiliencia.
 *
 * @param scenario  nombre del archivo del escenario
 * @param declared  {@code true} si el escenario declaró una hipótesis de estado estable
 * @param satisfied {@code true} si la hipótesis se sostuvo
 */
public record CheckOutcome(String scenario, boolean declared, boolean satisfied) {

    /** El escenario pasa el gate solo si declaró una hipótesis y la sostuvo. */
    public boolean passed() {
        return declared && satisfied;
    }
}
