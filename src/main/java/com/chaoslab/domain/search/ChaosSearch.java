package com.chaoslab.domain.search;

import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.hypothesis.SteadyStateHypothesis;
import com.chaoslab.domain.metrics.ResilienceMetrics;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Búsqueda de caos determinista (mini-DST): dada una hipótesis de estado estable (el oráculo),
 * explora combinaciones de semilla y fallos buscando el escenario que la refuta, y luego lo
 * <b>minimiza</b> (shrinking) al conjunto de fallos más chico y de menor intensidad que todavía la
 * rompe. Convierte "reproduce un escenario" en "encuentra el escenario que te rompe".
 *
 * <p>Todo es determinista: el barrido es sistemático (fallos individuales, luego pares) y el
 * {@link ScenarioRunner} inyectado es reproducible; no hay aleatoriedad suelta.
 */
public final class ChaosSearch {

    /** Un contraejemplo es "más mínimo" con menos fallos; a igualdad, el más severo y la menor semilla. */
    private static final Comparator<Counterexample> BY_MINIMALITY =
        Comparator.comparingInt(Counterexample::faultCount)
            .thenComparingDouble(Counterexample::worstWindowSuccessRate)
            .thenComparingLong(Counterexample::seed);

    private final ScenarioRunner runner;
    private final SteadyStateHypothesis hypothesis;

    public ChaosSearch(ScenarioRunner runner, SteadyStateHypothesis hypothesis) {
        this.runner = Objects.requireNonNull(runner, "runner");
        this.hypothesis = Objects.requireNonNull(hypothesis, "hypothesis");
        if (!hypothesis.isDeclared()) {
            throw new IllegalArgumentException("la búsqueda necesita una hipótesis con invariantes");
        }
    }

    /**
     * Busca un contraejemplo dentro del presupuesto dado.
     *
     * @param seeds        semillas a probar (el workload varía con la semilla)
     * @param candidates   fallos candidatos a inyectar
     * @param maxScenarios tope de escenarios a simular (presupuesto)
     * @return el resultado, con el contraejemplo mínimo si la hipótesis fue refutada
     */
    public ChaosSearchResult search(List<Long> seeds, List<Fault> candidates, int maxScenarios) {
        Objects.requireNonNull(seeds, "seeds");
        Objects.requireNonNull(candidates, "candidates");
        if (seeds.isEmpty()) {
            throw new IllegalArgumentException("se necesita al menos una semilla");
        }
        Budget budget = new Budget(maxScenarios);
        List<Counterexample> found = new ArrayList<>();

        sweepSingles(seeds, candidates, budget, found);
        // Solo buscamos pares si ningún fallo individual refutó: un par nunca será más "mínimo".
        if (found.isEmpty()) {
            sweepPairs(seeds, candidates, budget, found);
        }

        if (found.isEmpty()) {
            return ChaosSearchResult.robust(budget.spent());
        }
        Counterexample worst = found.stream().min(BY_MINIMALITY).orElseThrow();
        return ChaosSearchResult.refutedBy(shrink(worst), budget.spent());
    }

    private void sweepSingles(List<Long> seeds, List<Fault> candidates, Budget budget,
                              List<Counterexample> found) {
        for (Fault candidate : candidates) {
            for (long seed : seeds) {
                if (budget.exhausted()) {
                    return;
                }
                Counterexample ce = evaluate(seed, List.of(candidate), budget);
                if (ce != null) {
                    found.add(ce);
                    break; // este candidato ya refuta; probamos el siguiente
                }
            }
        }
    }

    private void sweepPairs(List<Long> seeds, List<Fault> candidates, Budget budget,
                            List<Counterexample> found) {
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                for (long seed : seeds) {
                    if (budget.exhausted()) {
                        return;
                    }
                    Counterexample ce = evaluate(seed, List.of(candidates.get(i), candidates.get(j)), budget);
                    if (ce != null) {
                        found.add(ce);
                        break;
                    }
                }
            }
        }
    }

    /** Corre un escenario y devuelve el contraejemplo si refuta la hipótesis, o {@code null}. */
    private Counterexample evaluate(long seed, List<Fault> faults, Budget budget) {
        budget.spendOne();
        SimulationReport report = runner.run(seed, faults);
        HypothesisReport verdict = hypothesis.evaluate(report);
        if (verdict.satisfied()) {
            return null;
        }
        double worst = ResilienceMetrics.from(report.timeline()).worstWindowSuccessRate();
        return new Counterexample(seed, faults, worst, verdict);
    }

    /** Reduce el contraejemplo: quita fallos redundantes y baja la intensidad de las latencias. */
    private Counterexample shrink(Counterexample counterexample) {
        long seed = counterexample.seed();
        List<Fault> faults = removeRedundantFaults(seed, new ArrayList<>(counterexample.faults()));
        faults = reduceLatencyIntensities(seed, faults);
        // El shrinking no cuenta contra el presupuesto de exploración; siempre refuta.
        Counterexample minimal = evaluate(seed, faults, Budget.unlimited());
        return minimal != null ? minimal : counterexample;
    }

    private List<Fault> removeRedundantFaults(long seed, List<Fault> faults) {
        boolean changed = true;
        while (changed && faults.size() > 1) {
            changed = false;
            for (int i = 0; i < faults.size(); i++) {
                List<Fault> reduced = new ArrayList<>(faults);
                reduced.remove(i);
                if (evaluate(seed, reduced, Budget.unlimited()) != null) {
                    faults = reduced;
                    changed = true;
                    break;
                }
            }
        }
        return faults;
    }

    private List<Fault> reduceLatencyIntensities(long seed, List<Fault> faults) {
        List<Fault> result = new ArrayList<>(faults);
        for (int i = 0; i < result.size(); i++) {
            if (!(result.get(i) instanceof LatencyFault latency) || latency.extraMillis() <= 1) {
                continue;
            }
            long extra = latency.extraMillis();
            while (extra > 1) {
                long half = extra / 2;
                List<Fault> trial = new ArrayList<>(result);
                trial.set(i, new LatencyFault(latency.id(), latency.targetId(),
                    latency.atMillis(), latency.durationMillis(), half));
                if (evaluate(seed, trial, Budget.unlimited()) == null) {
                    break; // con menos latencia ya no refuta: nos quedamos con la anterior
                }
                result = trial;
                extra = half;
            }
        }
        return result;
    }

    /** Contador de presupuesto de escenarios (mutable, de un solo uso por búsqueda). */
    private static final class Budget {
        private final int max;
        private int spent;

        Budget(int max) {
            if (max <= 0) {
                throw new IllegalArgumentException("el presupuesto debe ser > 0, fue: " + max);
            }
            this.max = max;
        }

        static Budget unlimited() {
            return new Budget(Integer.MAX_VALUE);
        }

        boolean exhausted() {
            return spent >= max;
        }

        void spendOne() {
            spent++;
        }

        int spent() {
            return spent;
        }
    }
}
