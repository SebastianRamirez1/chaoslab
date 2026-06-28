package com.chaoslab.domain.topology;

/** Tipos de componente del MVP (directrices: alcance acotado, no sobre-modelar). */
public enum ComponentType {
    SERVICE,
    QUEUE,
    DATABASE,
    LOAD_BALANCER
}
