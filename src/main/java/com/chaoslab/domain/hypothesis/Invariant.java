package com.chaoslab.domain.hypothesis;

import com.chaoslab.domain.metrics.SimulationReport;
import java.util.Objects;

/**
 * Un invariante declarado del estado estable: la afirmación de que cierta {@link Metric} debe
 * mantener una relación ({@link Comparison}) con un umbral. Es el "oráculo" que el experimento de
 * caos intenta refutar.
 *
 * @param metric     métrica a observar
 * @param comparison relación esperada contra el umbral
 * @param threshold  umbral declarado
 */
public record Invariant(Metric metric, Comparison comparison, double threshold) {

    public Invariant {
        Objects.requireNonNull(metric, "metric");
        Objects.requireNonNull(comparison, "comparison");
    }

    /** Evalúa este invariante contra un reporte ya calculado. */
    public InvariantResult evaluate(SimulationReport report) {
        Objects.requireNonNull(report, "report");
        double actual = metric.extract(report);
        return new InvariantResult(metric, comparison, threshold, actual, comparison.test(actual, threshold));
    }
}
