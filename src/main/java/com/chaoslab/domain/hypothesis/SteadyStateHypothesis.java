package com.chaoslab.domain.hypothesis;

import com.chaoslab.domain.metrics.SimulationReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hipótesis de estado estable: el conjunto de invariantes que definen "operación normal" y que un
 * experimento de caos intenta refutar (principio formal de Chaos Engineering). Declarada en el YAML
 * de la topología y evaluada sobre el reporte final de la corrida.
 *
 * @param invariants invariantes declarados (puede estar vacío)
 */
public record SteadyStateHypothesis(List<Invariant> invariants) {

    public SteadyStateHypothesis {
        invariants = invariants == null ? List.of() : List.copyOf(invariants);
    }

    /** Hipótesis vacía: no hay invariantes declarados. */
    public static SteadyStateHypothesis none() {
        return new SteadyStateHypothesis(List.of());
    }

    /** {@code true} si hay al menos un invariante que evaluar. */
    public boolean isDeclared() {
        return !invariants.isEmpty();
    }

    /** Evalúa todos los invariantes contra el reporte y produce el veredicto PASA/FALLA. */
    public HypothesisReport evaluate(SimulationReport report) {
        Objects.requireNonNull(report, "report");
        if (invariants.isEmpty()) {
            return HypothesisReport.notDeclared();
        }
        List<InvariantResult> results = new ArrayList<>(invariants.size());
        boolean allSatisfied = true;
        for (Invariant invariant : invariants) {
            InvariantResult result = invariant.evaluate(report);
            results.add(result);
            allSatisfied &= result.satisfied();
        }
        return new HypothesisReport(true, allSatisfied, results);
    }
}
