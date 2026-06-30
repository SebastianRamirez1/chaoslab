package com.chaoslab.domain.topology;

/**
 * Arista dirigida de la topología: un request fluye de {@code from} hacia {@code to}.
 *
 * @param from id del componente origen
 * @param to   id del componente destino
 */
public record Connection(String from, String to) {

    public Connection {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("'from' de la conexión no puede estar vacío");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("'to' de la conexión no puede estar vacío");
        }
    }
}
