package com.chaoslab.domain.topology;

import java.util.List;

/**
 * Balanceador: reparte requests entre sus destinos en round-robin (determinista, sin RNG).
 * Latencia de enrutado 0 y capacidad efectivamente ilimitada.
 */
public final class LoadBalancer extends AbstractComponent {

    private int next;

    public LoadBalancer(String id) {
        super(id, Integer.MAX_VALUE);
    }

    @Override
    public ComponentType type() {
        return ComponentType.LOAD_BALANCER;
    }

    @Override
    protected long latencyMillis() {
        return 0L;
    }

    /**
     * Elige el siguiente destino entre las opciones dadas, en round-robin.
     *
     * @param options destinos posibles (no vacío)
     * @return el destino elegido
     */
    public String chooseFrom(List<String> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("el balanceador '" + id() + "' no tiene destinos");
        }
        String chosen = options.get(next % options.size());
        next = (next + 1) % options.size();
        return chosen;
    }
}
