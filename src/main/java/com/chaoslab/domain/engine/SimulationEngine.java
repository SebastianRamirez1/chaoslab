package com.chaoslab.domain.engine;

import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.resilience.ResiliencePolicy;
import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.LoadBalancer;
import com.chaoslab.domain.topology.Outcome;
import com.chaoslab.domain.topology.Request;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import java.util.Objects;

/**
 * Motor de eventos discretos: un reloj virtual salta de evento a evento (nunca en tiempo real),
 * procesando la cola de prioridad hasta agotarla. Misma topología + mismos eventos = mismo
 * resultado siempre: ese determinismo es el atributo de Fiabilidad (#1).
 *
 * <p>Cada salto entre componentes se modela como una "llamada" del llamador al destino; el
 * llamador aplica sus políticas de resiliencia (circuit breaker, timeout, reintentos).
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
            case FaultInjected injected -> onFaultInjected(injected);
            case FaultCleared cleared -> cleared.fault().clear(topology);
        }
    }

    private void onArrived(RequestArrived arrived) {
        Component target = topology.component(arrived.componentId());
        ResiliencePolicy callerPolicy = callerPolicyOf(arrived.fromId());
        long now = clock.now();

        if (callerPolicy.hasBreaker() && !callerPolicy.breakerFor(target.id()).isCallPermitted(now)) {
            handleFailedCall(arrived, FailureReason.CIRCUIT_OPEN);
            return;
        }

        metrics.recordArrival(target.id());
        Outcome outcome = target.receive(arrived.request());
        if (!outcome.accepted()) {
            recordBreakerFailure(callerPolicy, target.id(), now);
            handleFailedCall(arrived, outcome.reason());
            return;
        }

        long latency = outcome.latencyMillis();
        if (callerPolicy.hasTimeout() && latency > callerPolicy.timeoutMillis()) {
            target.release();
            recordBreakerFailure(callerPolicy, target.id(), now);
            handleFailedCall(arrived, FailureReason.TIMEOUT);
            return;
        }

        if (callerPolicy.hasBreaker()) {
            callerPolicy.breakerFor(target.id()).recordSuccess();
        }
        events.schedule(new RequestDeparted(now + latency, arrived.request(), target.id()));
    }

    private void onDeparted(RequestDeparted departed) {
        String fromId = departed.componentId();
        Component caller = topology.component(fromId);
        caller.release();

        List<String> downstreams = topology.downstreams(fromId);
        if (downstreams.isEmpty()) {
            events.schedule(new RequestCompleted(clock.now(), departed.request()));
            return;
        }

        if (caller instanceof LoadBalancer balancer) {
            String target = chooseTarget(balancer, downstreams, caller.resilience(), clock.now());
            if (target == null) {
                events.schedule(new RequestFailed(clock.now(), departed.request(), fromId, FailureReason.CIRCUIT_OPEN));
            } else {
                forward(fromId, target, departed.request(), 0);
            }
        } else {
            for (String target : downstreams) {
                forward(fromId, target, departed.request(), 0);
            }
        }
    }

    private void onFaultInjected(FaultInjected injected) {
        Fault fault = injected.fault();
        fault.apply(topology);
        if (fault.durationMillis() > 0) {
            events.schedule(new FaultCleared(clock.now() + fault.durationMillis(), fault));
        }
    }

    /** Programa la llegada a {@code target}, salvo que una partición de red lo impida. */
    private void forward(String fromId, String target, Request request, int attempt) {
        if (topology.isReachable(fromId, target)) {
            events.schedule(new RequestArrived(clock.now(), request, target, fromId, attempt));
        } else {
            events.schedule(new RequestFailed(clock.now(), request, target, FailureReason.NETWORK_PARTITION));
        }
    }

    /** Tras una llamada fallida, reintenta (si la política lo permite) o falla definitivamente. */
    private void handleFailedCall(RequestArrived call, FailureReason reason) {
        String fromId = call.fromId();
        if (fromId != null) {
            ResiliencePolicy policy = topology.component(fromId).resilience();
            int nextAttempt = call.attempt() + 1;
            if (nextAttempt < policy.maxAttempts()) {
                String retryTarget = chooseRetryTarget(topology.component(fromId), call.componentId(), policy);
                if (retryTarget != null) {
                    forward(fromId, retryTarget, call.request(), nextAttempt);
                    return;
                }
            }
        }
        events.schedule(new RequestFailed(clock.now(), call.request(), call.componentId(), reason));
    }

    private String chooseTarget(LoadBalancer balancer, List<String> downstreams, ResiliencePolicy policy, long now) {
        if (!policy.hasBreaker()) {
            return balancer.chooseFrom(downstreams);
        }
        List<String> permitted = downstreams.stream()
            .filter(target -> policy.breakerFor(target).isCallPermitted(now))
            .toList();
        return permitted.isEmpty() ? null : balancer.chooseFrom(permitted);
    }

    private String chooseRetryTarget(Component caller, String failedTarget, ResiliencePolicy policy) {
        if (caller instanceof LoadBalancer balancer) {
            return chooseTarget(balancer, topology.downstreams(caller.id()), policy, clock.now());
        }
        return failedTarget;
    }

    private ResiliencePolicy callerPolicyOf(String fromId) {
        return fromId == null ? ResiliencePolicy.none() : topology.component(fromId).resilience();
    }

    private static void recordBreakerFailure(ResiliencePolicy policy, String targetId, long now) {
        if (policy.hasBreaker()) {
            policy.breakerFor(targetId).recordFailure(now);
        }
    }
}
