package com.chaoslab.infrastructure.config;

import com.chaoslab.application.RunSimulationUseCase;
import com.chaoslab.application.TopologyLoader;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.infrastructure.yaml.YamlTopologyLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de las capas internas (dominio/aplicación) como beans. Es el único lugar de
 * infraestructura que conoce a la aplicación: la dependencia sigue apuntando hacia adentro.
 */
@Configuration
public class ChaosLabConfig {

    @Bean
    public SimulationLimits simulationLimits() {
        return SimulationLimits.defaults();
    }

    @Bean
    public TopologyLoader topologyLoader(SimulationLimits simulationLimits) {
        return new YamlTopologyLoader(simulationLimits);
    }

    @Bean
    public RunSimulationUseCase runSimulationUseCase(TopologyLoader topologyLoader, SimulationLimits simulationLimits) {
        return new RunSimulationUseCase(topologyLoader, simulationLimits);
    }
}
