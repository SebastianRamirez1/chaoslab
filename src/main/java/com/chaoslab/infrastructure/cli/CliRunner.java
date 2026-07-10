package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.ChaosSearchUseCase;
import com.chaoslab.application.ResilienceCheckUseCase;
import com.chaoslab.application.RunSimulationUseCase;
import java.util.Objects;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Adaptador que conecta la CLI de Picocli con el arranque de Spring Boot. Traduce los argumentos
 * a la ejecución del comando y expone el código de salida resultante.
 */
@Component
public final class CliRunner implements CommandLineRunner, ExitCodeGenerator {

    private final RunSimulationUseCase useCase;
    private final ChaosSearchUseCase searchUseCase;
    private final ResilienceCheckUseCase checkUseCase;
    private int exitCode;

    public CliRunner(RunSimulationUseCase useCase, ChaosSearchUseCase searchUseCase,
                     ResilienceCheckUseCase checkUseCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.searchUseCase = Objects.requireNonNull(searchUseCase, "searchUseCase");
        this.checkUseCase = Objects.requireNonNull(checkUseCase, "checkUseCase");
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            // Modo servidor: el dashboard ya está levantado; no hay comando CLI que ejecutar.
            System.out.println("ChaosLab dashboard: http://localhost:8080");
            this.exitCode = 0;
            return;
        }
        ConsoleReportPrinter printer = new ConsoleReportPrinter();
        CommandLine commandLine = new CommandLine(new ChaosLabCommand())
            .addSubcommand("run", new RunCommand(useCase, printer))
            .addSubcommand("search", new SearchCommand(searchUseCase, printer))
            .addSubcommand("check", new CheckCommand(checkUseCase, printer));
        this.exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
