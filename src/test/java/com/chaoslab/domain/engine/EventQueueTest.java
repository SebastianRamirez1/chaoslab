package com.chaoslab.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaoslab.domain.topology.Request;
import org.junit.jupiter.api.Test;

/** La cola debe ordenar por timestamp y, ante empate, por orden de inserción (determinismo). */
class EventQueueTest {

    private static RequestArrived arrival(long timestamp, long id) {
        return new RequestArrived(timestamp, new Request(id, timestamp), "c");
    }

    @Test
    void pollsInTimestampOrder() {
        EventQueue queue = new EventQueue();
        queue.schedule(arrival(30, 0));
        queue.schedule(arrival(10, 1));
        queue.schedule(arrival(20, 2));

        assertThat(queue.poll().timestampMillis()).isEqualTo(10L);
        assertThat(queue.poll().timestampMillis()).isEqualTo(20L);
        assertThat(queue.poll().timestampMillis()).isEqualTo(30L);
    }

    @Test
    void breaksTiesByInsertionOrder() {
        EventQueue queue = new EventQueue();
        queue.schedule(arrival(5, 100));
        queue.schedule(arrival(5, 200));

        assertThat(((RequestArrived) queue.poll()).request().id()).isEqualTo(100L);
        assertThat(((RequestArrived) queue.poll()).request().id()).isEqualTo(200L);
    }

    @Test
    void pollOnEmptyReturnsNull() {
        assertThat(new EventQueue().poll()).isNull();
    }
}
