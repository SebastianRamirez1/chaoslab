package com.chaoslab.application;

import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.workload.PoissonWorkloadGenerator;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Caso de uso: cargar una topología, generar su workload de forma determinista y correr el motor.
 * Orquesta el dominio sin acoplarse a ningún framework (directrices §1.2).
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
     * @return el reporte final
     */
    public SimulationReport run(Path topologyFile, OptionalLong seedOverride) {
        LoadedScenario scenario = loader.load(topologyFile);
        long seed = seedOverride.isPresent() ? seedOverride.getAsLong() : scenario.seed();

        RandomGenerator random = new Random(seed);
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(scenario.topology(), metrics, limits.maxEvents());
        PoissonWorkloadGenerator generator = new PoissonWorkloadGenerator(random);

        long generated = generator.scheduleArrivals(
            engine, scenario.workload(), scenario.topology().entryPointId(), limits.maxRequests());

        return engine.run(seed, generated);
    }
}
