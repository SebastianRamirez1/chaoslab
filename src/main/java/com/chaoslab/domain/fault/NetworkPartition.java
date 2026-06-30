package com.chaoslab.domain.fault;

import com.chaoslab.domain.topology.TopologyGraph;
import java.util.Set;

/**
 * Partición de red (split-brain): corta la comunicación entre dos grupos de componentes.
 * Un request que deba cruzar de un grupo al otro falla mientras dura la partición.
 *
 * @param id             identificador del fallo
 * @param groupA         primer grupo de componentes
 * @param groupB         segundo grupo de componentes
 * @param atMillis       instante de inyección (ms)
 * @param durationMillis duración (ms); 0 = permanente
 */
public record NetworkPartition(String id, Set<String> groupA, Set<String> groupB,
                               long atMillis, long durationMillis) implements Fault {

    public NetworkPartition {
        FaultValidation.requireId(id);
        FaultValidation.requireTiming(atMillis, durationMillis);
        if (groupA == null || groupA.isEmpty() || groupB == null || groupB.isEmpty()) {
            throw new IllegalArgumentException("la partición '" + id + "' requiere group_a y group_b no vacíos");
        }
        groupA = Set.copyOf(groupA);
        groupB = Set.copyOf(groupB);
    }

    @Override
    public Set<String> targets() {
        Set<String> all = new java.util.HashSet<>(groupA);
        all.addAll(groupB);
        return Set.copyOf(all);
    }

    @Override
    public void apply(TopologyGraph topology) {
        topology.partition(groupA, groupB);
    }

    @Override
    public void clear(TopologyGraph topology) {
        topology.healPartition(groupA, groupB);
    }
}
