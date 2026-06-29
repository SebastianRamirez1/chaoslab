package com.chaoslab.infrastructure.cli;

import com.chaoslab.domain.fault.CrashFault;
import com.chaoslab.domain.fault.Fault;
import com.chaoslab.domain.fault.LatencyFault;
import com.chaoslab.domain.fault.NetworkPartition;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser de la sintaxis compacta del flag {@code --fault} de la CLI:
 * <ul>
 *   <li>{@code crash:<target>:<atSeg>[:<durSeg>]}</li>
 *   <li>{@code latency:<target>:<atSeg>:<durSeg>:<extraMs>}</li>
 *   <li>{@code partition:<a1,a2>:<b1,b2>:<atSeg>:<durSeg>}</li>
 * </ul>
 */
final class FaultSpecParser {

    private FaultSpecParser() {
    }

    static Fault parse(String spec, int index) {
        String[] parts = spec.split(":");
        String type = parts[0].toLowerCase(Locale.ROOT);
        String id = "cli-" + type + "-" + index;
        try {
            return switch (type) {
                case "crash" -> {
                    require(parts.length == 3 || parts.length == 4, spec);
                    long duration = parts.length == 4 ? seconds(parts[3]) : 0L;
                    yield new CrashFault(id, parts[1], seconds(parts[2]), duration);
                }
                case "latency" -> {
                    require(parts.length == 5, spec);
                    yield new LatencyFault(
                        id, parts[1], seconds(parts[2]), seconds(parts[3]), Long.parseLong(parts[4]));
                }
                case "partition" -> {
                    require(parts.length == 5, spec);
                    yield new NetworkPartition(
                        id, group(parts[1]), group(parts[2]), seconds(parts[3]), seconds(parts[4]));
                }
                default -> throw new IllegalArgumentException(
                    "tipo de fallo desconocido en --fault '" + spec + "' (válidos: crash, latency, partition)");
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("número inválido en --fault '" + spec + "'", e);
        }
    }

    private static void require(boolean condition, String spec) {
        if (!condition) {
            throw new IllegalArgumentException("formato inválido en --fault '" + spec + "'");
        }
    }

    private static long seconds(String value) {
        return Long.parseLong(value.trim()) * 1000L;
    }

    private static Set<String> group(String csv) {
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
