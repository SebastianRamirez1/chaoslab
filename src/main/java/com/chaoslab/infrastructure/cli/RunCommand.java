package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.domain.engine.SimulationLimitExceededException;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.infrastructure.yaml.TopologyValidationException;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Subcomando {@code run}: corre una simulación a partir de un archivo de topología YAML. */
@Command(name = "run", description = "Corre una simulación a partir de un archivo de topología YAML.")
public final class RunCommand implements Callable<Integer> {

    private final RunSimulationUseCase useCase;
    private final ConsoleReportPrinter printer;

    @Parameters(index = "0", paramLabel = "TOPOLOGY", description = "archivo YAML de la topología")
    private Path topologyFile;

    @Option(names = "--seed", description = "semilla que sobrescribe la del YAML (reproducibilidad)")
    private Long seed;

    public RunCommand(RunSimulationUseCase useCase, ConsoleReportPrinter printer) {
        this.useCase = useCase;
        this.printer = printer;
    }

    @Override
    public Integer call() {
        try {
            OptionalLong override = seed == null ? OptionalLong.empty() : OptionalLong.of(seed);
            SimulationReport report = useCase.run(topologyFile, override);
            printer.print(report);
            return 0;
        } catch (TopologyValidationException e) {
            System.err.println("Topología inválida: " + e.getMessage());
            return 2;
        } catch (SimulationLimitExceededException e) {
            System.err.println("Límite de simulación excedido: " + e.getMessage());
            return 3;
        }
    }
}
