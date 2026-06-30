package com.chaoslab.domain.topology;

/**
 * Cola/buffer acotado (tipo Kafka/RabbitMQ). En el MVP modela un buffer con límite:
 * absorbe hasta {@code maxSize} mensajes en vuelo y rechaza al llenarse (modelo de pérdida).
 *
 * <p>La latencia de tránsito es 0; el comportamiento de espera FIFO con servicio diferido
 * es una mejora explícita de fases posteriores (no sobre-modelar en el MVP).
 */
public final class MessageQueue extends AbstractComponent {

    public MessageQueue(String id, int maxSize) {
        super(id, maxSize);
    }

    @Override
    public ComponentType type() {
        return ComponentType.QUEUE;
    }

    @Override
    protected long latencyMillis() {
        return 0L;
    }
}
