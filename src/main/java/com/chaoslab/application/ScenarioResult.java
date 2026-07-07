package com.chaoslab.application;

import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.metrics.ResilienceMetrics;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.Objects;

/**
 * Resultado completo de correr un escenario: el reporte crudo de la simulación (física pura) más los
 * análisis derivados de él — el veredicto de la hipótesis de estado estable (el oráculo) y las
 * métricas de resiliencia. Se mantienen separados del reporte porque son interpretaciones post-hoc,
 * no la simulación en sí.
 *
 * @param report     reporte y línea de tiempo de la corrida
 * @param hypothesis veredicto PASA/FALLA de la hipótesis (o "no declarada")
 * @param resilience métricas de resiliencia derivadas de la timeline (disponibilidad, MTTR, …)
 */
public record ScenarioResult(SimulationReport report, HypothesisReport hypothesis,
                             ResilienceMetrics resilience) {

    public ScenarioResult {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(hypothesis, "hypothesis");
        Objects.requireNonNull(resilience, "resilience");
    }
}
