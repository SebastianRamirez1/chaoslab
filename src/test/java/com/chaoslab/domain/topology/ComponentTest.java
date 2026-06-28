package com.chaoslab.domain.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit puro de los componentes: capacidad, latencia, salud y enrutado del balanceador. */
class ComponentTest {

    private static Request request(long id) {
        return new Request(id, 0L);
    }

    @Test
    void serviceAdmitsUntilCapacityThenRejects() {
        Service service = new Service("svc", 2, 50L);

        Outcome first = service.receive(request(0));
        Outcome second = service.receive(request(1));
        Outcome third = service.receive(request(2));

        assertThat(first.accepted()).isTrue();
        assertThat(first.latencyMillis()).isEqualTo(50L);
        assertThat(second.accepted()).isTrue();
        assertThat(third.accepted()).isFalse();
        assertThat(third.rejectionReason()).contains("capacidad");
    }

    @Test
    void releaseFreesCapacity() {
        Service service = new Service("svc", 1, 10L);
        service.receive(request(0));
        assertThat(service.receive(request(1)).accepted()).isFalse();

        service.release();

        assertThat(service.receive(request(2)).accepted()).isTrue();
    }

    @Test
    void healthIsDegradedWhenSaturated() {
        Service service = new Service("svc", 1, 10L);
        assertThat(service.health()).isEqualTo(Health.UP);

        service.receive(request(0));

        assertThat(service.health()).isEqualTo(Health.DEGRADED);
    }

    @Test
    void tracksPeakConcurrency() {
        Service service = new Service("svc", 5, 10L);
        service.receive(request(0));
        service.receive(request(1));
        service.release();
        service.receive(request(2));

        assertThat(service.maxInFlight()).isEqualTo(2);
    }

    @Test
    void databaseAndQueueExposeTheirLatency() {
        assertThat(new Database("db", 10, 20L).receive(request(0)).latencyMillis()).isEqualTo(20L);
        assertThat(new MessageQueue("q", 100).receive(request(0)).latencyMillis()).isZero();
    }

    @Test
    void loadBalancerChoosesRoundRobin() {
        LoadBalancer balancer = new LoadBalancer("lb");
        List<String> options = List.of("a", "b");

        assertThat(balancer.chooseFrom(options)).isEqualTo("a");
        assertThat(balancer.chooseFrom(options)).isEqualTo("b");
        assertThat(balancer.chooseFrom(options)).isEqualTo("a");
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new Service("svc", 0, 10L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Service("svc", 1, -1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Service(" ", 1, 10L)).isInstanceOf(IllegalArgumentException.class);
    }
}
