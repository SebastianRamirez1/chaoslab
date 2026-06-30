package com.chaoslab.domain.fault;

import com.chaoslab.domain.topology.TopologyGraph;
import java.util.Set;

/**
 * Caída de un nodo: el componente objetivo deja de responder y rechaza todos sus requests
 * mientras dura el fallo.
 *
 * @param id             identificador del fallo
 * @param targetId       componente a tumbar
 * @param atMillis       instante de inyección (ms)
 * @param durationMillis duración (ms); 0 = permanente
 */
public record CrashFault(String id, String targetId, long atMillis, long durationMillis) implements Fault {

    public CrashFault {
        FaultValidation.requireId(id);
        FaultValidation.requireTarget(targetId);
        FaultValidation.requireTiming(atMillis, durationMillis);
    }

    @Override
    public Set<String> targets() {
        return Set.of(targetId);
    }

    @Override
    public void apply(TopologyGraph topology) {
        topology.component(targetId).crash();
    }

    @Override
    public void clear(TopologyGraph topology) {
        topology.component(targetId).recover();
    }
}
