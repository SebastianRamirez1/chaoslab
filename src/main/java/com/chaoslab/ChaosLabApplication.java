package com.chaoslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque de la aplicación (capa de infraestructura).
 *
 * <p>Dos modos: {@code run <topologia.yaml>} corre una simulación por CLI (sin servidor);
 * sin argumentos levanta el dashboard web. El dominio ({@code com.chaoslab.domain}) no
 * depende de esta clase ni de Spring (la dependencia apunta hacia adentro).
 */
@SpringBootApplication
public class ChaosLabApplication {

    public static void main(String[] args) {
        if (args.length > 0 && "run".equals(args[0])) {
            // Modo CLI batch: sin servidor web; propaga el código de salida del comando.
            SpringApplication app = new SpringApplication(ChaosLabApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            System.exit(SpringApplication.exit(app.run(args)));
        } else {
            // Modo servidor: levanta el dashboard web.
            SpringApplication.run(ChaosLabApplication.class, args);
        }
    }
}
