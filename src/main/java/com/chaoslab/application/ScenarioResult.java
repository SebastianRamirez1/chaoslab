package com.chaoslab.application;

import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.Objects;

/**
 * Resultado completo de correr un escenario: el reporte de la simulación (física pura) más el
 * veredicto de su hipótesis de estado estable (el oráculo). Se mantienen separados en el dominio y
 * se agrupan acá, en la frontera de aplicación, porque evaluar una hipótesis es una preocupación
 * distinta de computar la simulación.
 *
 * @param report     reporte y línea de tiempo de la corrida
 * @param hypothesis veredicto PASA/FALLA de la hipótesis (o "no declarada")
 */
public record ScenarioResult(SimulationReport report, HypothesisReport hypothesis) {

    public ScenarioResult {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(hypothesis, "hypothesis");
    }
}
