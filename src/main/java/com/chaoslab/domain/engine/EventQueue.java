package com.chaoslab.domain.engine;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Cola de eventos ordenada por instante simulado. Ante empates (mismo timestamp) respeta el
 * orden de inserción mediante un número de secuencia, garantizando un procesamiento determinista.
 */
public final class EventQueue {

    private final PriorityQueue<Scheduled> queue;
    private long sequence;

    public EventQueue() {
        this.queue = new PriorityQueue<>(
            Comparator.comparingLong((Scheduled s) -> s.event.timestampMillis())
                .thenComparingLong(s -> s.seq));
    }

    /** Encola un evento. */
    public void schedule(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event no puede ser null");
        }
        queue.add(new Scheduled(event, sequence++));
    }

    /** Extrae el próximo evento (el de menor timestamp), o {@code null} si la cola está vacía. */
    public Event poll() {
        Scheduled next = queue.poll();
        return next == null ? null : next.event;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    private static final class Scheduled {
        private final Event event;
        private final long seq;

        Scheduled(Event event, long seq) {
            this.event = event;
            this.seq = seq;
        }
    }
}
