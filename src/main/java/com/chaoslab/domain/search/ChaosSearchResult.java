package com.chaoslab.domain.search;

import java.util.Optional;

/**
 * Resultado de una búsqueda de caos (mini-DST): si se logró refutar la hipótesis, el contraejemplo
 * ya minimizado (shrinking) y cuántos escenarios se evaluaron.
 *
 * @param refuted             {@code true} si se encontró un escenario que refuta la hipótesis
 * @param minimal             el contraejemplo mínimo (presente solo si {@code refuted})
 * @param scenariosEvaluated  cantidad de escenarios simulados durante la búsqueda
 */
public record ChaosSearchResult(boolean refuted, Counterexample minimal, int scenariosEvaluated) {

    /** La hipótesis resistió: ningún escenario probado la refutó. */
    public static ChaosSearchResult robust(int scenariosEvaluated) {
        return new ChaosSearchResult(false, null, scenariosEvaluated);
    }

    /** Se halló (y minimizó) un contraejemplo. */
    public static ChaosSearchResult refutedBy(Counterexample minimal, int scenariosEvaluated) {
        return new ChaosSearchResult(true, minimal, scenariosEvaluated);
    }

    /** El contraejemplo mínimo, si la hipótesis fue refutada. */
    public Optional<Counterexample> counterexample() {
        return Optional.ofNullable(minimal);
    }
}
