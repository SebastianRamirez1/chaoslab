package com.chaoslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque de la aplicación (capa de infraestructura).
 *
 * <p>El dominio ({@code com.chaoslab.domain}) no depende de esta clase ni de Spring:
 * la dependencia siempre apunta hacia adentro (Infrastructure → Application → Domain).
 */
@SpringBootApplication
public class ChaosLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChaosLabApplication.class, args);
    }
}
