package com.chaoslab.domain.hypothesis;

import java.util.List;

/**
 * Veredicto de una hipótesis de estado estable sobre una corrida: PASA/FALLA con el desglose por
 * invariante. Es un value object inmutable y determinista (mismo reporte -> mismo veredicto).
 *
 * @param declared  {@code true} si la topología declaró al menos un invariante
 * @param satisfied {@code true} si la hipótesis se cumple (todos los invariantes, o no hay ninguno)
 * @param results   resultado de cada invariante evaluado
 */
public record HypothesisReport(boolean declared, boolean satisfied, List<InvariantResult> results) {

    public HypothesisReport {
        results = results == null ? List.of() : List.copyOf(results);
    }

    /** Veredicto cuando la topología no declara hipótesis: no hay nada que refutar. */
    public static HypothesisReport notDeclared() {
        return new HypothesisReport(false, true, List.of());
    }
}
