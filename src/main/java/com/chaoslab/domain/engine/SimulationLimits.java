package com.chaoslab.domain.engine;

/**
 * Límites duros de la simulación (directrices §5.6: prevenir consumo ilimitado / DoS a partir
 * de un YAML no confiable). Acotan tamaño de topología, duración y volumen de trabajo.
 *
 * @param maxComponents        máximo de componentes en una topología
 * @param maxDurationSeconds   máxima duración simulada
 * @param maxRequestsPerSecond máxima tasa de generación de workload
 * @param maxRequests          máximo de requests generados en una corrida
 * @param maxEvents            máximo de eventos procesados antes de abortar
 */
public record SimulationLimits(int maxComponents, int maxDurationSeconds, int maxRequestsPerSecond,
                               long maxRequests, long maxEvents) {

    public SimulationLimits {
        requirePositive(maxComponents, "maxComponents");
        requirePositive(maxDurationSeconds, "maxDurationSeconds");
        requirePositive(maxRequestsPerSecond, "maxRequestsPerSecond");
        requirePositive(maxRequests, "maxRequests");
        requirePositive(maxEvents, "maxEvents");
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " debe ser > 0, fue: " + value);
        }
    }

    /** Límites por defecto, razonables para uso educativo en una sola máquina. */
    public static SimulationLimits defaults() {
        return new SimulationLimits(100, 3600, 100_000, 5_000_000L, 20_000_000L);
    }
}
