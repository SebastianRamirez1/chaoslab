package com.chaoslab.infrastructure.cli;

import com.chaoslab.domain.metrics.ComponentReport;
import com.chaoslab.domain.metrics.LatencyStats;
import com.chaoslab.domain.metrics.SimulationReport;
import java.util.Locale;

/** Presenta un {@link SimulationReport} en consola, en texto legible. */
public final class ConsoleReportPrinter {

    /** Imprime el reporte en la salida estándar. */
    public void print(SimulationReport report) {
        System.out.print(format(report));
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
