package com.chaoslab.domain.fault;

import com.chaoslab.domain.topology.TopologyGraph;
import java.util.Set;

/**
 * Degradación / red lenta: suma latencia de proceso al componente objetivo mientras dura.
 *
 * @param id             identificador del fallo
 * @param targetId       componente afectado
 * @param atMillis       instante de inyección (ms)
 * @param durationMillis duración (ms); 0 = permanente
 * @param extraMillis    latencia adicional a sumar (ms)
 */
public record LatencyFault(String id, String targetId, long atMillis, long durationMillis, long extraMillis)
    implements Fault {

    public LatencyFault {
        FaultValidation.requireId(id);
        FaultValidation.requireTarget(targetId);
        FaultValidation.requireTiming(atMillis, durationMillis);
        if (extraMillis < 0) {
            throw new IllegalArgumentException("extra_ms debe ser >= 0, fue: " + extraMillis);
        }
    }

    @Override
    public Set<String> targets() {
        return Set.of(targetId);
    }

    @Override
    public void apply(TopologyGraph topology) {
        topology.component(targetId).addLatency(extraMillis);
    }

    @Override
    public void clear(TopologyGraph topology) {
        topology.component(targetId).removeLatency(extraMillis);
    }
}
