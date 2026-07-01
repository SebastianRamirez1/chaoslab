package com.chaoslab.infrastructure.cli;

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
    private int exitCode;

    public CliRunner(RunSimulationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            // Modo servidor: el dashboard ya está levantado; no hay comando CLI que ejecutar.
            System.out.println("ChaosLab dashboard: http://localhost:8080");
            this.exitCode = 0;
            return;
        }
        CommandLine commandLine = new CommandLine(new ChaosLabCommand())
            .addSubcommand("run", new RunCommand(useCase, new ConsoleReportPrinter()));
        this.exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
