package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.ChaosSearchUseCase;
import com.chaoslab.domain.search.ChaosSearchResult;
import com.chaoslab.infrastructure.yaml.TopologyValidationException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Subcomando {@code search}: búsqueda de caos (mini-DST). Dada una topología con hipótesis de estado
 * estable, busca automáticamente el escenario de fallos que la refuta y lo minimiza. Sale con código
 * 1 si halla un contraejemplo (útil como gate de resiliencia en CI).
 */
@Command(name = "search",
    description = "Busca el escenario de fallos que refuta la hipótesis de estado estable (mini-DST).")
public final class SearchCommand implements Callable<Integer> {

    private final ChaosSearchUseCase useCase;
    private final ConsoleReportPrinter printer;

    @Parameters(index = "0", paramLabel = "TOPOLOGY", description = "archivo YAML con 'steady_state'")
    private Path topologyFile;

    @Option(names = "--seeds", description = "cantidad de semillas a probar (por defecto 3)")
    private int seeds = 3;

    @Option(names = "--budget", description = "tope de escenarios a simular (por defecto 80)")
    private int budget = 80;

    public SearchCommand(ChaosSearchUseCase useCase, ConsoleReportPrinter printer) {
        this.useCase = useCase;
        this.printer = printer;
    }

    @Override
    public Integer call() {
        try {
            ChaosSearchResult result = useCase.search(topologyFile, seeds, budget);
            printer.printSearch(result);
            // Exit 1 = se halló un escenario que refuta la hipótesis (falla el build en CI).
            return result.refuted() ? 1 : 0;
        } catch (TopologyValidationException | IllegalArgumentException e) {
            System.err.println("Entrada inválida: " + e.getMessage());
            return 2;
        }
    }
}
