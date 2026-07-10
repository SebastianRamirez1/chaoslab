package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.ResilienceCheckUseCase;
import com.chaoslab.infrastructure.yaml.TopologyValidationException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Subcomando {@code check}: gate de resiliencia. Corre un conjunto de escenarios (cada uno con su
 * hipótesis de estado estable) y sale con código 1 si alguno se refuta o no declara hipótesis.
 * Pensado para CI: si la resiliencia regresa, rompe el build.
 */
@Command(name = "check",
    description = "Gate de resiliencia: verifica que cada escenario sostenga su hipótesis de estado estable.")
public final class CheckCommand implements Callable<Integer> {

    private final ResilienceCheckUseCase useCase;
    private final ConsoleReportPrinter printer;

    @Parameters(arity = "1..*", paramLabel = "TOPOLOGY", description = "archivos YAML con 'steady_state'")
    private List<Path> files;

    public CheckCommand(ResilienceCheckUseCase useCase, ConsoleReportPrinter printer) {
        this.useCase = useCase;
        this.printer = printer;
    }

    @Override
    public Integer call() {
        try {
            boolean allPassed = printer.printCheck(useCase.check(files));
            return allPassed ? 0 : 1;
        } catch (TopologyValidationException | IllegalArgumentException e) {
            System.err.println("Entrada inválida: " + e.getMessage());
            return 2;
        }
    }
}
