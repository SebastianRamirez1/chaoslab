package com.chaoslab.domain.topology;

/**
 * Una unidad de trabajo que viaja por la topología.
 *
 * @param id              identificador secuencial del request
 * @param entryTimeMillis instante (simulado, ms) en que entró al sistema; base de la
 *                        latencia extremo a extremo
 */
public record Request(long id, long entryTimeMillis) {

    public Request {
        if (id < 0) {
            throw new IllegalArgumentException("id debe ser >= 0, fue: " + id);
        }
        if (entryTimeMillis < 0) {
            throw new IllegalArgumentException("entryTimeMillis debe ser >= 0, fue: " + entryTimeMillis);
        }
    }
}
