package com.chaoslab.application;

import com.chaoslab.domain.engine.FaultInjected;
import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.PoissonWorkloadGenerator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Caso de uso: cargar una topología, inyectar los fallos programados y correr el motor de forma
 * determinista. Orquesta el dominio sin acoplarse a ningún framework (directrices §1.2).
 */
public final class RunSimulationUseCase {

    private final TopologyLoader loader;
    private final SimulationLimits limits;

    public RunSimulationUseCase(TopologyLoader loader, SimulationLimits limits) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Corre una simulación a partir de un archivo de topología.
     *
     * @param topologyFile archivo YAML
     * @param seedOverride semilla que sobrescribe la del archivo (opcional)
     * @param extraFaults  fallos adicionales inyectados desde fuera del YAML (p. ej. la CLI)
     * @return el reporte final
     */
    public SimulationReport run(Path topologyFile, OptionalLong seedOverride, List<Fault> extraFaults) {
        LoadedScenario scenario = loader.load(topologyFile);
        long seed = seedOverride.isPresent() ? seedOverride.getAsLong() : scenario.seed();

        RandomGenerator random = new Random(seed);
        MetricsCollector metrics = new MetricsCollector();
        TopologyGraph topology = scenario.topology();
        SimulationEngine engine = new SimulationEngine(topology, metrics, limits.maxEvents());

        scheduleFaults(engine, topology, mergeFaults(scenario.faults(), extraFaults));

        PoissonWorkloadGenerator generator = new PoissonWorkloadGenerator(random);
        long generated = generator.scheduleArrivals(
            engine, scenario.workload(), topology.entryPointId(), limits.maxRequests());

        return engine.run(seed, generated);
    }

    private List<Fault> mergeFaults(List<Fault> fromScenario, List<Fault> extra) {
        List<Fault> all = new ArrayList<>(fromScenario);
        if (extra != null) {
            all.addAll(extra);
        }
        if (all.size() > limits.maxFaults()) {
            throw new IllegalArgumentException(
                "demasiados fallos: " + all.size() + " (máximo " + limits.maxFaults() + ")");
        }
        return all;
    }

    private void scheduleFaults(SimulationEngine engine, TopologyGraph topology, List<Fault> faults) {
        for (Fault fault : faults) {
            for (String target : fault.targets()) {
                if (!topology.contains(target)) {
                    throw new IllegalArgumentException(
                        "el fallo '" + fault.id() + "' referencia un componente inexistente: '" + target + "'");
                }
            }
            engine.schedule(new FaultInjected(fault.atMillis(), fault));
        }
    }
}
