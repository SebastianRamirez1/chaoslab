package com.chaoslab.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.application.CheckOutcome;
import com.chaoslab.application.ResilienceCheckUseCase;
import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.domain.engine.SimulationLimits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Gate de resiliencia embebido en {@code mvn verify}: cada escenario de {@code scenarios/} debe
 * sostener su hipótesis de estado estable. Si un cambio hace regresar la resiliencia (p. ej. rompe
 * el CircuitBreaker o el enrutado del retry), este test falla y rompe el build.
 */
class ResilienceRegressionTest {

    @Test
    void everyRegressionScenarioUpholdsItsHypothesis() throws IOException {
        SimulationLimits limits = SimulationLimits.defaults();
        ResilienceCheckUseCase check = new ResilienceCheckUseCase(
            new RunSimulationUseCase(new YamlTopologyLoader(limits), limits));

        List<Path> scenarios;
        try (Stream<Path> files = Files.list(Path.of("scenarios"))) {
            scenarios = files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList();
        }
        assertThat(scenarios).as("debe haber escenarios de regresión en scenarios/").isNotEmpty();

        for (CheckOutcome outcome : check.check(scenarios)) {
            assertThat(outcome.declared())
                .as("%s debe declarar una hipótesis (steady_state)", outcome.scenario()).isTrue();
            assertThat(outcome.satisfied())
                .as("%s debe sostener su hipótesis de resiliencia", outcome.scenario()).isTrue();
        }
    }
}
