package com.chaoslab.domain.hypothesis;

import java.util.Locale;

/**
 * Comparador de un invariante: relaciona el valor observado de una métrica con un umbral.
 * Acepta tanto símbolos ({@code >=}, {@code <=}) como palabras ({@code gte}, {@code lte}) en el YAML.
 */
public enum Comparison {

    GTE(">=", "gte"),
    LTE("<=", "lte"),
    GT(">", "gt"),
    LT("<", "lt"),
    EQ("==", "eq");

    private final String symbol;
    private final String word;

    Comparison(String symbol, String word) {
        this.symbol = symbol;
        this.word = word;
    }

    /** Representación canónica para mostrar (p. ej. {@code >=}). */
    public String symbol() {
        return symbol;
    }

    /**
     * Evalúa la comparación. Usa {@link Double#compare} para no disparar comparación directa de
     * coma flotante y mantener resultados estables.
     */
    public boolean test(double actual, double threshold) {
        int cmp = Double.compare(actual, threshold);
        return switch (this) {
            case GTE -> cmp >= 0;
            case LTE -> cmp <= 0;
            case GT -> cmp > 0;
            case LT -> cmp < 0;
            case EQ -> cmp == 0;
        };
    }

    /** Resuelve un comparador por símbolo o palabra; lanza {@link IllegalArgumentException} si no existe. */
    public static Comparison fromToken(String rawToken) {
        String normalized = rawToken == null ? "" : rawToken.trim().toLowerCase(Locale.ROOT);
        for (Comparison comparison : values()) {
            if (comparison.symbol.equals(normalized) || comparison.word.equals(normalized)) {
                return comparison;
            }
        }
        throw new IllegalArgumentException(
            "comparador desconocido: '" + rawToken + "' (válidos: >=, <=, >, <, == o gte, lte, gt, lt, eq)");
    }
}
