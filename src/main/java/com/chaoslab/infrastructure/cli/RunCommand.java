package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.domain.engine.SimulationLimitExceededException;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.infrastructure.yaml.TopologyValidationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    @Option(names = "--fault", description = "inyecta un fallo, p. ej. crash:api-1:35:10 (repetible)")
    private List<String> faultSpecs = new ArrayList<>();

    public RunCommand(RunSimulationUseCase useCase, ConsoleReportPrinter printer) {
        this.useCase = useCase;
        this.printer = printer;
    }

    @Override
    public Integer call() {
        try {
            OptionalLong override = seed == null ? OptionalLong.empty() : OptionalLong.of(seed);
            List<Fault> faults = new ArrayList<>();
            for (int i = 0; i < faultSpecs.size(); i++) {
                faults.add(FaultSpecParser.parse(faultSpecs.get(i), i));
            }
            SimulationReport report = useCase.run(topologyFile, override, faults);
            printer.print(report);
            return 0;
        } catch (TopologyValidationException | IllegalArgumentException e) {
            System.err.println("Entrada inválida: " + e.getMessage());
            return 2;
        } catch (SimulationLimitExceededException e) {
            System.err.println("Límite de simulación excedido: " + e.getMessage());
            return 3;
        }
    }
}
