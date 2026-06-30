package com.chaoslab.infrastructure.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.fault.NetworkPartition;
import org.junit.jupiter.api.Test;

/** Parseo de la sintaxis compacta del flag --fault. */
class FaultSpecParserTest {

    @Test
    void parsesCrashWithOptionalDuration() {
        Fault fault = FaultSpecParser.parse("crash:api-1:35:10", 0);

        assertThat(fault).isInstanceOf(CrashFault.class);
        CrashFault crash = (CrashFault) fault;
        assertThat(crash.targetId()).isEqualTo("api-1");
        assertThat(crash.atMillis()).isEqualTo(35_000L);
        assertThat(crash.durationMillis()).isEqualTo(10_000L);
    }

    @Test
    void parsesLatency() {
        Fault fault = FaultSpecParser.parse("latency:db:0:5:500", 1);

        assertThat(fault).isInstanceOf(LatencyFault.class);
        assertThat(((LatencyFault) fault).extraMillis()).isEqualTo(500L);
    }

    @Test
    void parsesPartitionWithGroups() {
        Fault fault = FaultSpecParser.parse("partition:a,b:c:0:5", 2);

        assertThat(fault).isInstanceOf(NetworkPartition.class);
        NetworkPartition partition = (NetworkPartition) fault;
        assertThat(partition.groupA()).containsExactlyInAnyOrder("a", "b");
        assertThat(partition.groupB()).containsExactly("c");
    }

    @Test
    void rejectsUnknownTypeAndBadFormat() {
        assertThatThrownBy(() -> FaultSpecParser.parse("wormhole:x:1", 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FaultSpecParser.parse("crash:onlytarget", 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FaultSpecParser.parse("latency:db:notanumber:5:500", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
