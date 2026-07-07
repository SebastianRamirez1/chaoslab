package com.chaoslab.domain.search;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera el catálogo de fallos candidatos que la búsqueda de caos probará sobre una topología.
 * Excluye el punto de entrada: la caída del único punto de entrada tumba todo trivialmente y no
 * enseña nada sobre patrones de resiliencia; los hallazgos interesantes están aguas abajo.
 */
public final class CandidateFaults {

    private static final long DEFAULT_LATENCY_EXTRA_MS = 300L;

    private CandidateFaults() {
    }

    /**
     * Un crash permanente y una inyección de latencia por cada componente aguas abajo.
     *
     * @param topology        topología a explorar
     * @param durationSeconds duración del workload (la latencia dura toda la corrida)
     * @return fallos candidatos (uno por tipo y componente)
     */
    public static List<Fault> forTopology(TopologyGraph topology, int durationSeconds) {
        long runMillis = durationSeconds * 1000L;
        List<Fault> candidates = new ArrayList<>();
        for (Component component : topology.components()) {
            if (component.id().equals(topology.entryPointId())) {
                continue;
            }
            String id = component.id();
            candidates.add(new CrashFault("search-crash-" + id, id, 0L, 0L));
            candidates.add(new LatencyFault(
                "search-latency-" + id, id, 0L, runMillis, DEFAULT_LATENCY_EXTRA_MS));
        }
        return candidates;
    }
}
