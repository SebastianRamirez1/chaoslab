package com.chaoslab.domain.metrics;

import com.chaoslab.domain.resilience.CircuitBreakerState;
import com.chaoslab.domain.topology.Health;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Métricas de resiliencia estandarizadas, derivadas de la línea de tiempo de una corrida. Convierten
 * el "momento ajá" en números comparables y enseñables (disponibilidad, MTTR, profundidad de la
 * degradación, tiempo de detección del breaker). Es una función pura y determinista de la timeline.
 *
 * @param availability             fracción de instantes sin ningún componente DOWN, en [0,1]
 * @param meanTimeToRecoveryMillis  MTTR: duración media de los episodios de deterioro que se
 *                                  recuperaron (0 si ninguno terminó recuperándose)
 * @param longestDowntimeMillis     episodio de deterioro contiguo más largo observado (ms)
 * @param timeToFirstBreakerTripMillis primer instante con un breaker no CLOSED (-1 si ninguno abrió)
 * @param worstWindowSuccessRate    menor tasa de éxito por intervalo entre snapshots, en [0,1]
 */
public record ResilienceMetrics(double availability, long meanTimeToRecoveryMillis,
                                long longestDowntimeMillis, long timeToFirstBreakerTripMillis,
                                double worstWindowSuccessRate) {

    private static final long NO_BREAKER_TRIP = -1L;

    /** Métrica neutra cuando no hay timeline (nada observado que salga de lo normal). */
    public static ResilienceMetrics empty() {
        return new ResilienceMetrics(1.0, 0L, 0L, NO_BREAKER_TRIP, 1.0);
    }

    /** Calcula las métricas a partir de la secuencia de snapshots por segundo. */
    public static ResilienceMetrics from(List<SimulationSnapshot> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            return empty();
        }
        int healthyInstants = 0;
        long firstBreakerTrip = NO_BREAKER_TRIP;
        double worstWindow = 1.0;
        long prevCompleted = 0L;
        long prevFailed = 0L;

        // Episodios de deterioro por componente: instante de inicio del deterioro en curso.
        Map<String, Long> impairedSince = new HashMap<>();
        List<Long> recoveredDurations = new ArrayList<>();
        long longestDowntime = 0L;

        for (SimulationSnapshot snapshot : timeline) {
            if (!hasDownComponent(snapshot)) {
                healthyInstants++;
            }
            if (firstBreakerTrip == NO_BREAKER_TRIP && hasOpenBreaker(snapshot)) {
                firstBreakerTrip = snapshot.atMillis();
            }

            long windowCompleted = snapshot.completedSoFar() - prevCompleted;
            long windowFailed = snapshot.failedSoFar() - prevFailed;
            long windowTotal = windowCompleted + windowFailed;
            if (windowTotal > 0) {
                worstWindow = Math.min(worstWindow, (double) windowCompleted / windowTotal);
            }
            prevCompleted = snapshot.completedSoFar();
            prevFailed = snapshot.failedSoFar();

            longestDowntime = Math.max(longestDowntime,
                trackEpisodes(snapshot, impairedSince, recoveredDurations));
        }

        // Episodios que nunca se recuperaron: cuentan como downtime hasta el último instante.
        long lastMillis = timeline.get(timeline.size() - 1).atMillis();
        for (long since : impairedSince.values()) {
            longestDowntime = Math.max(longestDowntime, lastMillis - since);
        }

        double availability = (double) healthyInstants / timeline.size();
        long mttr = mean(recoveredDurations);
        return new ResilienceMetrics(availability, mttr, longestDowntime, firstBreakerTrip, worstWindow);
    }

    /** Actualiza los episodios de deterioro por componente y devuelve la mayor duración recuperada aquí. */
    private static long trackEpisodes(SimulationSnapshot snapshot, Map<String, Long> impairedSince,
                                      List<Long> recoveredDurations) {
        long longestRecoveredNow = 0L;
        for (ComponentSnapshot component : snapshot.components()) {
            boolean impaired = component.health() != Health.UP;
            Long since = impairedSince.get(component.id());
            if (impaired && since == null) {
                impairedSince.put(component.id(), snapshot.atMillis());
            } else if (!impaired && since != null) {
                long duration = snapshot.atMillis() - since;
                recoveredDurations.add(duration);
                longestRecoveredNow = Math.max(longestRecoveredNow, duration);
                impairedSince.remove(component.id());
            }
        }
        return longestRecoveredNow;
    }

    private static boolean hasDownComponent(SimulationSnapshot snapshot) {
        return snapshot.components().stream().anyMatch(c -> c.health() == Health.DOWN);
    }

    private static boolean hasOpenBreaker(SimulationSnapshot snapshot) {
        return snapshot.circuits().stream().anyMatch(c -> c.state() != CircuitBreakerState.CLOSED);
    }

    private static long mean(List<Long> values) {
        if (values.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return sum / values.size();
    }
}
