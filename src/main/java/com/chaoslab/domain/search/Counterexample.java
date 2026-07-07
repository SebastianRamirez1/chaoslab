package com.chaoslab.domain.search;

import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.hypothesis.HypothesisReport;
import java.util.List;

/**
 * Un contraejemplo hallado por la búsqueda: la combinación de semilla + fallos que refuta la
 * hipótesis de estado estable, junto con qué tan grave fue (tasa de éxito en el peor segundo) y el
 * veredicto por invariante.
 *
 * @param seed                   semilla del workload que reproduce el fallo
 * @param faults                 fallos inyectados que refutan la hipótesis
 * @param worstWindowSuccessRate tasa de éxito en el peor segundo (severidad; menor = peor)
 * @param hypothesis             veredicto refutado (con el desglose por invariante)
 */
public record Counterexample(long seed, List<Fault> faults, double worstWindowSuccessRate,
                             HypothesisReport hypothesis) {

    public Counterexample {
        faults = faults == null ? List.of() : List.copyOf(faults);
    }

    /** Cantidad de fallos del contraejemplo (menos = más "mínimo"). */
    public int faultCount() {
        return faults.size();
    }
}
