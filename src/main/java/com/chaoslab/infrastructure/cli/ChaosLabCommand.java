package com.chaoslab.infrastructure.cli;

import picocli.CommandLine.Command;

/** Comando raíz de la CLI. Sin subcomando muestra la ayuda; los subcomandos hacen el trabajo. */
@Command(name = "chaoslab", mixinStandardHelpOptions = true, version = "ChaosLab 0.1.0-SNAPSHOT",
    description = "Simulador educativo de sistemas distribuidos y chaos engineering.")
public final class ChaosLabCommand implements Runnable {

    @Override
    public void run() {
        // Sin subcomando no hay trabajo que hacer; CliRunner imprime la ayuda.
    }
}
