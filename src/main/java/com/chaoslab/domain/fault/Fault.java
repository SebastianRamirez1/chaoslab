package com.chaoslab.domain.fault;

import com.chaoslab.domain.topology.TopologyGraph;
import java.util.Set;

/**
 * Un fallo inyectable en la simulación (el diferenciador del proyecto). Tipo sellado: el
 * conjunto de fallos del MVP es cerrado y conocido.
 *
 * <p>Se inyecta vía evento en {@link #atMillis()} y, si tiene duración, se revierte sola.
 */
public sealed interface Fault permits LatencyFault, CrashFault, NetworkPartition {

    /** Identificador del fallo (para reportarlo). */
    String id();

    /** Instante simulado (ms) en que se inyecta. */
    long atMillis();

    /** Duración del fallo en ms; 0 = permanente (no se revierte). */
    long durationMillis();

    /** Componentes que el fallo afecta (para validar que existan en la topología). */
    Set<String> targets();

    /** Aplica el efecto del fallo sobre la topología. */
    void apply(TopologyGraph topology);

    /** Revierte el efecto del fallo. */
    void clear(TopologyGraph topology);
}
