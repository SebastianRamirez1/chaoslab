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
        CommandLine commandLine = new CommandLine(new ChaosLabCommand())
            .addSubcommand("run", new RunCommand(useCase, new ConsoleReportPrinter()));
        if (args.length == 0) {
            commandLine.usage(System.out);
            this.exitCode = 0;
            return;
        }
        this.exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
