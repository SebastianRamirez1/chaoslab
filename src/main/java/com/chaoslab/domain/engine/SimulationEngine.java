package com.chaoslab.domain.engine;

import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.Outcome;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import java.util.Objects;

/**
 * Motor de eventos discretos: un reloj virtual salta de evento a evento (nunca en tiempo real),
 * procesando la cola de prioridad hasta agotarla. Misma topología + mismos eventos = mismo
 * resultado siempre: ese determinismo es el atributo de Fiabilidad (#1).
 */
public final class SimulationEngine {

    private final Clock clock = new Clock();
    private final EventQueue events = new EventQueue();
    private final TopologyGraph topology;
    private final MetricsCollector metrics;
    private final long maxEvents;

    public SimulationEngine(TopologyGraph topology, MetricsCollector metrics, long maxEvents) {
        this.topology = Objects.requireNonNull(topology, "topology");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        if (maxEvents <= 0) {
            throw new IllegalArgumentException("maxEvents debe ser > 0, fue: " + maxEvents);
        }
        this.maxEvents = maxEvents;
    }

    /** Encola un evento (lo usa el generador de workload y la inyección de fallos). */
    public void schedule(Event event) {
        events.schedule(event);
    }

    /**
     * Corre la simulación hasta agotar la cola de eventos.
     *
     * @param seed              semilla usada (solo para reportarla)
     * @param generatedRequests cantidad de requests que generó el workload
     * @return el reporte final de métricas
     * @throws SimulationLimitExceededException si se supera el límite de eventos
     */
    public SimulationReport run(long seed, long generatedRequests) {
        long processed = 0;
        while (!events.isEmpty()) {
            if (processed >= maxEvents) {
                throw new SimulationLimitExceededException(
                    "se superó el límite de eventos (" + maxEvents + ")");
            }
            Event event = events.poll();
            clock.advanceTo(event.timestampMillis());
            handle(event);
            processed++;
        }
        return metrics.report(topology, clock.now(), generatedRequests, seed);
    }

    private void handle(Event event) {
        switch (event) {
            case RequestArrived arrived -> onArrived(arrived);
            case RequestDeparted departed -> onDeparted(departed);
            case RequestCompleted completed ->
                metrics.recordCompletion(completed.timestampMillis() - completed.request().entryTimeMillis());
            case RequestFailed failed -> metrics.recordFailure(failed.componentId(), failed.reason());
        }
    }

    private void onArrived(RequestArrived arrived) {
        Component component = topology.component(arrived.componentId());
        metrics.recordArrival(arrived.componentId());
        Outcome outcome = component.receive(arrived.request());
        if (outcome.accepted()) {
            long departAt = clock.now() + outcome.latencyMillis();
            events.schedule(new RequestDeparted(departAt, arrived.request(), arrived.componentId()));
        } else {
            events.schedule(
                new RequestFailed(clock.now(), arrived.request(), arrived.componentId(), outcome.rejectionReason()));
        }
    }

    private void onDeparted(RequestDeparted departed) {
        topology.component(departed.componentId()).release();
        List<String> nextHops = topology.route(departed.componentId());
        if (nextHops.isEmpty()) {
            events.schedule(new RequestCompleted(clock.now(), departed.request()));
        } else {
            for (String hop : nextHops) {
                events.schedule(new RequestArrived(clock.now(), departed.request(), hop));
            }
        }
    }
}
