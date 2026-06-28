package com.chaoslab.domain.workload;

import com.chaoslab.domain.engine.RequestArrived;
import com.chaoslab.domain.engine.SimulationEngine;
import com.chaoslab.domain.engine.SimulationLimitExceededException;
import com.chaoslab.domain.topology.Request;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Genera el workload como un proceso de Poisson: los tiempos entre llegadas siguen una
 * distribución exponencial derivada del PRNG sembrado. Aquí —y solo aquí— vive la aleatoriedad
 * del motor: misma semilla = mismos arribos = mismo resultado (Fiabilidad #1).
 */
public final class PoissonWorkloadGenerator {

    private final RandomGenerator random;

    public PoissonWorkloadGenerator(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Programa los arribos del workload en el motor, sobre el punto de entrada dado.
     *
     * @param engine       motor donde encolar los eventos de llegada
     * @param workload     definición del tráfico
     * @param entryPointId componente de entrada
     * @param maxRequests  límite duro de requests (anti consumo ilimitado)
     * @return cantidad de requests generados
     * @throws SimulationLimitExceededException si se supera {@code maxRequests}
     */
    public long scheduleArrivals(SimulationEngine engine, Workload workload, String entryPointId, long maxRequests) {
        long window = workload.windowMillis();
        double meanInterArrival = 1000.0 / workload.requestsPerSecond();
        long time = 0;
        long count = 0;
        while (true) {
            double uniform = random.nextDouble();
            double gap = -meanInterArrival * Math.log(1.0 - uniform);
            time += Math.max(1L, Math.round(gap));
            if (time > window) {
                return count;
            }
            if (count >= maxRequests) {
                throw new SimulationLimitExceededException("se superó el límite de requests (" + maxRequests + ")");
            }
            engine.schedule(new RequestArrived(time, new Request(count, time), entryPointId));
            count++;
        }
    }
}
