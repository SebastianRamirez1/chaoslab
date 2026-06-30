package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.metrics.MetricsCollector;
import com.chaoslab.domain.metrics.SimulationReport;
import com.chaoslab.domain.resilience.ResiliencePolicy;
import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Database;
import com.chaoslab.domain.topology.FailureReason;
import com.chaoslab.domain.topology.LoadBalancer;
import com.chaoslab.domain.topology.Request;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los patrones de resiliencia hacen que el sistema degrade en vez de colapsar ante un fallo. */
class ResilienceTest {

    private static final long MAX_EVENTS = 10_000_000L;
    private static final int REQUESTS = 100;

    /** gateway (LB, con la política dada) reparte entre dos réplicas terminales. */
    private static TopologyGraph loadBalanced(ResiliencePolicy gatewayPolicy) {
        LoadBalancer gateway = new LoadBalancer("gateway");
        gateway.configureResilience(gatewayPolicy);
        return TopologyGraph.of(
            "t",
            List.of(gateway, new Service("api-1", 1000, 10L), new Service("api-2", 1000, 10L)),
            List.of(new Connection("gateway", "api-1"), new Connection("gateway", "api-2")));
    }

    private static SimulationReport runWithApi1Crashed(TopologyGraph topology) {
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);
        engine.schedule(new FaultInjected(0L, new CrashFault("c", "api-1", 0L, 0L)));
        for (int i = 0; i < REQUESTS; i++) {
            engine.schedule(new RequestArrived(i + 1L, new Request(i, i + 1L), "gateway"));
        }
        return engine.run(1L, REQUESTS);
    }

    @Test
    void withoutCircuitBreakerHalfTheRequestsFail() {
        SimulationReport report = runWithApi1Crashed(loadBalanced(ResiliencePolicy.none()));

        // El balanceador sigue mandando ~la mitad del tráfico a la réplica caída.
        assertThat(report.failedRequests()).isBetween(45L, 55L);
        assertThat(report.failuresByReason()).containsKey(FailureReason.CRASH);
    }

    @Test
    void circuitBreakerReroutesAroundCrashedReplica() {
        // Breaker en el gateway: tras 3 fallos a api-1 deja de enrutarle y manda todo a api-2.
        SimulationReport report = runWithApi1Crashed(loadBalanced(new ResiliencePolicy(0, 1, 3, 1_000_000L)));

        assertThat(report.completedRequests()).isGreaterThanOrEqualTo(95L);
        assertThat(report.failuresByReason()).containsKey(FailureReason.CRASH);
    }

    @Test
    void retryReachesAHealthyReplica() {
        // Sin breaker pero con reintento: el request que cae en api-1 se reintenta en api-2.
        SimulationReport report = runWithApi1Crashed(loadBalanced(new ResiliencePolicy(0, 2, 0, 0L)));

        assertThat(report.completedRequests()).isGreaterThanOrEqualTo(95L);
    }

    @Test
    void timeoutFailsFastOnSlowDownstream() {
        Service api = new Service("api", 100, 10L);
        api.configureResilience(new ResiliencePolicy(50, 1, 0, 0L)); // timeout 50ms
        TopologyGraph topology = TopologyGraph.of(
            "t",
            List.of(api, new Database("db", 100, 200L)), // db tarda 200ms > 50ms
            List.of(new Connection("api", "db")));
        MetricsCollector metrics = new MetricsCollector();
        SimulationEngine engine = new SimulationEngine(topology, metrics, MAX_EVENTS);
        for (int i = 0; i < 10; i++) {
            engine.schedule(new RequestArrived(i + 1L, new Request(i, i + 1L), "api"));
        }

        SimulationReport report = engine.run(1L, 10);

        assertThat(report.completedRequests()).isZero();
        assertThat(report.failuresByReason()).containsEntry(FailureReason.TIMEOUT, 10L);
    }
}
