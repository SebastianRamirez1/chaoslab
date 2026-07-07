package com.chaoslab.application;

import com.chaoslab.domain.engine.FaultInjected;
import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.search.CandidateFaults;
import com.chaoslab.domain.search.ChaosSearch;
import com.chaoslab.domain.search.ChaosSearchResult;
import com.chaoslab.domain.search.ScenarioRunner;
import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.PoissonWorkloadGenerator;
import com.chaoslab.domain.workload.Workload;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.LongStream;

/**
 * Caso de uso: dada una topología con hipótesis de estado estable, busca automáticamente el
 * escenario de fallos que la refuta (mini-DST) y lo minimiza. Orquesta el dominio sin frameworks.
 *
 * <p>La búsqueda parte de un baseline limpio: usa solo los fallos candidatos generados, ignorando
 * los {@code faults:} declarados en el YAML (que son para la corrida interactiva/demo).
 */
public final class ChaosSearchUseCase {

    private final TopologyLoader loader;
    private final SimulationLimits limits;

    public ChaosSearchUseCase(TopologyLoader loader, SimulationLimits limits) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Corre la búsqueda de caos.
     *
     * @param topologyFile archivo YAML (debe declarar {@code steady_state})
     * @param seedCount    cantidad de semillas a probar (0..seedCount-1)
     * @param budget       tope de escenarios a simular
     * @return el resultado, con el contraejemplo mínimo si la hipótesis fue refutada
     */
    public ChaosSearchResult search(Path topologyFile, int seedCount, int budget) {
        if (seedCount <= 0) {
            throw new IllegalArgumentException("seeds debe ser > 0, fue: " + seedCount);
        }
        LoadedScenario scenario = loader.load(topologyFile);
        if (!scenario.hypothesis().isDeclared()) {
            throw new IllegalArgumentException(
                "la búsqueda de caos necesita una hipótesis: declará 'steady_state' en el YAML");
        }
        Workload workload = scenario.workload();
        ScenarioRunner runner = (seed, faults) -> runOnce(topologyFile, workload, seed, faults);

        List<Long> seeds = LongStream.range(0, seedCount).boxed().toList();
        List<Fault> candidates = CandidateFaults.forTopology(scenario.topology(), workload.durationSeconds());
        return new ChaosSearch(runner, scenario.hypothesis()).search(seeds, candidates, budget);
    }

    /** Corre una simulación con estado fresco: recarga la topología porque el motor muta su estado. */
    private SimulationReport runOnce(Path topologyFile, Workload workload, long seed, List<Fault> faults) {
        TopologyGraph topology = loader.load(topologyFile).topology();
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, limits.maxEvents());
        for (Fault fault : faults) {
            engine.schedule(new FaultInjected(fault.atMillis(), fault));
        }
        PoissonWorkloadGenerator generator = new PoissonWorkloadGenerator(new Random(seed));
        long generated = generator.scheduleArrivals(
            engine, workload, topology.entryPointId(), limits.maxRequests());
        return engine.run(seed, generated);
    }
}
