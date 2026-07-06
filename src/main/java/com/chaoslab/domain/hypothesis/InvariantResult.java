package com.chaoslab.domain.hypothesis;

/**
 * Resultado de evaluar un único invariante contra una corrida.
 *
 * @param metric     métrica observada
 * @param comparison comparador aplicado
 * @param threshold  umbral declarado
 * @param actual     valor observado de la métrica
 * @param satisfied  {@code true} si {@code actual comparison threshold} se cumple
 */
public record InvariantResult(Metric metric, Comparison comparison, double threshold,
                              double actual, boolean satisfied) {
}
