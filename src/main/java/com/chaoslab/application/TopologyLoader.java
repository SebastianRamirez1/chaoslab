package com.chaoslab.application;

import java.nio.file.Path;

/**
 * Puerto de entrada de topologías. El dominio/aplicación define el contrato; la infraestructura
 * (parser YAML) lo implementa, manteniendo la dependencia apuntando hacia adentro.
 */
public interface TopologyLoader {

    /**
     * Carga y valida un escenario desde un archivo.
     *
     * @param topologyFile ruta al archivo de topología
     * @return el escenario validado
     */
    LoadedScenario load(Path topologyFile);
}
