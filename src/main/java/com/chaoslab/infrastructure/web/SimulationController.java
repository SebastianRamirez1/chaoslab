package com.chaoslab.infrastructure.web;

import com.chaoslab.application.LoadedScenario;
import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.application.TopologyLoader;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.infrastructure.cli.FaultSpecParser;
import com.chaoslab.infrastructure.yaml.TopologyValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del dashboard: lista topologías y corre simulaciones (con fallos opcionales), devolviendo
 * el reporte completo con su línea de tiempo para que el frontend la reproduzca.
 */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final RunSimulationUseCase useCase;
    private final TopologyCatalog catalog;
    private final TopologyLoader loader;

    public SimulationController(RunSimulationUseCase useCase, TopologyCatalog catalog, TopologyLoader loader) {
        this.useCase = Objects.requireNonNull(useCase);
        this.catalog = Objects.requireNonNull(catalog);
        this.loader = Objects.requireNonNull(loader);
    }

    /** Petición de simulación: topología, semilla opcional y fallos opcionales (sintaxis --fault). */
    public record RunRequest(String topology, Long seed, List<String> faults) {
    }

    @GetMapping("/topologies")
    public List<String> topologies() {
        return catalog.names();
    }

    @PostMapping("/run")
    public SimulationResponse run(@RequestBody RunRequest request) throws IOException {
        if (request == null || request.topology() == null || request.topology().isBlank()) {
            throw new IllegalArgumentException("falta el campo 'topology'");
        }
        Path file = catalog.materialize(request.topology());
        List<Fault> faults = new ArrayList<>();
        List<String> specs = request.faults();
        if (specs != null) {
            for (int i = 0; i < specs.size(); i++) {
                faults.add(FaultSpecParser.parse(specs.get(i), i));
            }
        }
        OptionalLong seed = request.seed() == null ? OptionalLong.empty() : OptionalLong.of(request.seed());

        LoadedScenario scenario = loader.load(file);
        SimulationReport report = useCase.run(file, seed, faults);
        return new SimulationResponse(TopologyView.from(scenario.topology()), report);
    }

    @ExceptionHandler({TopologyValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> onBadInput(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
