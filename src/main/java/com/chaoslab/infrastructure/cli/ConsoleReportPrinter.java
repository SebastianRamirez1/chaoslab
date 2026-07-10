package com.chaoslab.infrastructure.cli;

import com.chaoslab.application.CheckOutcome;
import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.fault.NetworkPartition;
import com.chaoslab.domain.hypothesis.HypothesisReport;
import com.chaoslab.domain.hypothesis.InvariantResult;
import com.chaoslab.domain.metrics.ComponentReport;
import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.ResilienceMetrics;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.search.ChaosSearchResult;
import com.chaoslab.domain.search.Counterexample;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Presenta un {@link SimulationReport} en consola, en texto legible. */
public final class ConsoleReportPrinter {

    /** Imprime el reporte en la salida estándar. */
    public void print(SimulationReport report) {
        System.out.print(format(report));
    }

    /** Imprime las métricas de resiliencia derivadas de la corrida. */
    public void printResilience(ResilienceMetrics metrics) {
        System.out.print(formatResilience(metrics));
    }

    /** Formatea las métricas de resiliencia como texto legible. */
    public String formatResilience(ResilienceMetrics m) {
        StringBuilder out = new StringBuilder(256);
        out.append(String.format(Locale.ROOT, "%nresiliencia:%n"));
        out.append(String.format(Locale.ROOT, "  disponibilidad=%.1f%%   MTTR=%.1fs   "
                + "peor caída=%.1fs   éxito en el peor segundo=%.1f%%%n",
            m.availability() * 100.0, m.meanTimeToRecoveryMillis() / 1000.0,
            m.longestDowntimeMillis() / 1000.0, m.worstWindowSuccessRate() * 100.0));
        if (m.timeToFirstBreakerTripMillis() >= 0) {
            out.append(String.format(Locale.ROOT, "  breaker abrió por primera vez en t=%.1fs%n",
                m.timeToFirstBreakerTripMillis() / 1000.0));
        }
        return out.toString();
    }

    /** Imprime el resultado del gate de resiliencia y devuelve si todos los escenarios pasaron. */
    public boolean printCheck(List<CheckOutcome> outcomes) {
        System.out.print(formatCheck(outcomes));
        return outcomes.stream().allMatch(CheckOutcome::passed);
    }

    /** Formatea el resultado del gate de resiliencia como texto legible. */
    public String formatCheck(List<CheckOutcome> outcomes) {
        StringBuilder out = new StringBuilder(256);
        out.append(String.format(Locale.ROOT, "%n=== Gate de resiliencia ===%n"));
        long passed = outcomes.stream().filter(CheckOutcome::passed).count();
        for (CheckOutcome o : outcomes) {
            String label = !o.declared() ? "SIN HIPÓTESIS" : (o.satisfied() ? "PASA" : "FALLA");
            out.append(String.format(Locale.ROOT, "  [%-13s] %s%n", label, o.scenario()));
        }
        out.append(String.format(Locale.ROOT, "%nveredicto: %d/%d escenarios sostuvieron su hipótesis%n",
            passed, outcomes.size()));
        return out.toString();
    }

    /** Imprime el resultado de una búsqueda de caos (mini-DST). */
    public void printSearch(ChaosSearchResult result) {
        System.out.print(formatSearch(result));
    }

    /** Formatea el resultado de la búsqueda como texto legible. */
    public String formatSearch(ChaosSearchResult result) {
        StringBuilder out = new StringBuilder(256);
        out.append(String.format(Locale.ROOT, "%n=== Búsqueda de caos (mini-DST) ===%n"));
        out.append(String.format(Locale.ROOT, "escenarios evaluados: %d%n", result.scenariosEvaluated()));
        if (!result.refuted()) {
            out.append(String.format(Locale.ROOT,
                "veredicto: la hipótesis RESISTIÓ todos los escenarios probados (no se halló contraejemplo)%n"));
            return out.toString();
        }
        Counterexample ce = result.minimal();
        out.append(String.format(Locale.ROOT,
            "veredicto: hipótesis REFUTADA — escenario mínimo hallado%n"));
        out.append(String.format(Locale.ROOT, "  semilla: %d%n", ce.seed()));
        out.append(String.format(Locale.ROOT, "  fallos (%d): %s%n",
            ce.faultCount(), describeFaults(ce)));
        out.append(String.format(Locale.ROOT, "  éxito en el peor segundo: %.1f%%%n",
            ce.worstWindowSuccessRate() * 100.0));
        for (InvariantResult r : ce.hypothesis().results()) {
            out.append(String.format(Locale.ROOT, "  [%s] %s %s %s (observado %s)%n",
                r.satisfied() ? "OK" : "X", r.metric().key(), r.comparison().symbol(),
                trim(r.threshold()), trim(r.actual())));
        }
        return out.toString();
    }

    private static String describeFaults(Counterexample ce) {
        return ce.faults().stream().map(ConsoleReportPrinter::describeFault).collect(Collectors.joining(", "));
    }

    /** Descripción corta de un fallo para el reporte de búsqueda. */
    private static String describeFault(Fault fault) {
        return switch (fault) {
            case CrashFault crash -> "crash " + crash.targetId();
            case LatencyFault latency -> "latency " + latency.targetId() + " +" + latency.extraMillis() + "ms";
            case NetworkPartition partition -> "partition " + partition.targets();
        };
    }

    /** Imprime el veredicto de la hipótesis de estado estable (si fue declarada). */
    public void printHypothesis(HypothesisReport hypothesis) {
        String text = formatHypothesis(hypothesis);
        if (!text.isEmpty()) {
            System.out.print(text);
        }
    }

    /**
     * Formatea el veredicto de la hipótesis. Devuelve cadena vacía si no se declaró ninguna,
     * para no ensuciar la salida de corridas exploratorias.
     */
    public String formatHypothesis(HypothesisReport hypothesis) {
        if (hypothesis == null || !hypothesis.declared()) {
            return "";
        }
        // Marcadores ASCII (no ✓/✗) para que se lean bien también en consolas Windows (cp1252).
        StringBuilder out = new StringBuilder(256);
        out.append(String.format(Locale.ROOT, "%nhipótesis de estado estable: %s%n",
            hypothesis.satisfied() ? "PASA" : "FALLA"));
        for (InvariantResult r : hypothesis.results()) {
            out.append(String.format(Locale.ROOT, "  [%s] %s %s %s (observado %s)%n",
                r.satisfied() ? "OK" : "X", r.metric().key(), r.comparison().symbol(),
                trim(r.threshold()), trim(r.actual())));
        }
        return out.toString();
    }

    /** Muestra enteros sin decimales y fracciones con 3 dígitos, para leer bien umbrales y tasas. */
    private static String trim(double value) {
        if (Double.isFinite(value) && Double.compare(value, Math.rint(value)) == 0) {
            return String.format(Locale.ROOT, "%d", (long) value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** Formatea el reporte como texto (separado de la impresión para poder testearlo). */
    public String format(SimulationReport report) {
        StringBuilder out = new StringBuilder(512);
        out.append(String.format(Locale.ROOT, "%n=== ChaosLab — %s ===%n", report.topologyName()));
        out.append(String.format(Locale.ROOT, "seed=%d   duración simulada=%.1fs%n",
            report.seed(), report.simulatedDurationMillis() / 1000.0));
        out.append(String.format(Locale.ROOT,
            "requests: generados=%d  completados=%d  fallidos=%d  éxito=%.1f%%%n",
            report.generatedRequests(), report.completedRequests(), report.failedRequests(),
            report.successRate() * 100.0));

        if (!report.failuresByReason().isEmpty()) {
            StringBuilder reasons = new StringBuilder();
            report.failuresByReason().forEach((reason, count) -> {
                if (reasons.length() > 0) {
                    reasons.append("  ");
                }
                reasons.append(reason).append('=').append(count);
            });
            out.append(String.format(Locale.ROOT, "fallos por causa: %s%n", reasons));
        }

        LatencyStats latency = report.latency();
        out.append(String.format(Locale.ROOT,
            "latencia e2e (ms): p50=%d  p95=%d  p99=%d  min=%d  max=%d  (n=%d)%n",
            latency.p50(), latency.p95(), latency.p99(), latency.min(), latency.max(), latency.count()));

        out.append(String.format(Locale.ROOT, "%ncomponentes:%n"));
        for (ComponentReport component : report.components()) {
            out.append(String.format(Locale.ROOT,
                "  %-16s %-14s llegadas=%-8d rechazos=%-8d pico=%-6d salud=%s%n",
                component.id(), "[" + component.type() + "]", component.arrived(),
                component.rejected(), component.maxInFlight(), component.health()));
        }
        return out.toString();
    }
}
