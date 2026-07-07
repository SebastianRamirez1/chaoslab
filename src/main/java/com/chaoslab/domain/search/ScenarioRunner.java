package com.chaoslab.domain.search;

import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.List;

/**
 * Puerto que corre una simulación con una semilla y un conjunto de fallos dados, y devuelve su
 * reporte. La búsqueda de caos (dominio puro) lo usa sin conocer el motor ni cómo se arma la
 * topología: la implementación (capa de aplicación) inyecta ese cableado, manteniendo la
 * dependencia hacia adentro.
 *
 * <p>Cada invocación debe partir de estado fresco (misma semilla + mismos fallos = mismo reporte).
 */
@FunctionalInterface
public interface ScenarioRunner {

    /**
     * Corre una simulación reproducible.
     *
     * @param seed   semilla del workload
     * @param faults fallos a inyectar (solo estos; no los del YAML)
     * @return el reporte de la corrida
     */
    SimulationReport run(long seed, List<Fault> faults);
}
