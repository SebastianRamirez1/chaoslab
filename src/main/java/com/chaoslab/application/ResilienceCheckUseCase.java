package com.chaoslab.application;

import com.chaoslab.domain.hypothesis.HypothesisReport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Caso de uso del <b>gate de resiliencia</b>: corre un conjunto de escenarios (cada uno con su
 * hipótesis de estado estable declarada) y reporta si cada uno la sostiene. Materializa el "caos
 * continuo en CI/CD": si la resiliencia regresa, algún escenario deja de pasar y el build falla.
 */
public final class ResilienceCheckUseCase {

    private final RunSimulationUseCase runUseCase;

    public ResilienceCheckUseCase(RunSimulationUseCase runUseCase) {
        this.runUseCase = Objects.requireNonNull(runUseCase, "runUseCase");
    }

    /**
     * Verifica cada escenario con su semilla y fallos declarados en el YAML.
     *
     * @param topologyFiles archivos de escenario a verificar
     * @return el veredicto por escenario, en el mismo orden
     */
    public List<CheckOutcome> check(List<Path> topologyFiles) {
        Objects.requireNonNull(topologyFiles, "topologyFiles");
        List<CheckOutcome> outcomes = new ArrayList<>(topologyFiles.size());
        for (Path file : topologyFiles) {
            HypothesisReport hypothesis = runUseCase.run(file, OptionalLong.empty(), List.of()).hypothesis();
            Path fileName = file.getFileName();
            String name = fileName == null ? file.toString() : fileName.toString();
            outcomes.add(new CheckOutcome(name, hypothesis.declared(), hypothesis.satisfied()));
        }
        return outcomes;
    }
}
