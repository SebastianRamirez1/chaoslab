package com.chaoslab.infrastructure.yaml;

import com.chaoslab.application.LoadedScenario;
import com.chaoslab.application.TopologyLoader;
import com.chaoslab.domain.engine.SimulationLimits;
import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.ComponentType;
import com.chaoslab.domain.topology.Connection;
import com.chaoslab.domain.topology.Database;
import com.chaoslab.domain.topology.LoadBalancer;
import com.chaoslab.domain.topology.MessageQueue;
import com.chaoslab.domain.topology.Service;
import com.chaoslab.domain.topology.TopologyGraph;
import com.chaoslab.domain.workload.Workload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Carga topologías desde YAML. Trata el YAML como entrada externa NO confiable (directrices §5.6):
 * usa {@link SafeConstructor} (sin instanciar tipos arbitrarios), aplica límites de parseo
 * (tamaño, profundidad, alias) y valida estructura, rangos y referencias antes de tocar el motor.
 */
public final class YamlTopologyLoader implements TopologyLoader {

    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_NESTING_DEPTH = 50;
    private static final int MAX_ALIASES = 50;

    private final SimulationLimits limits;

    public YamlTopologyLoader(SimulationLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Override
    public LoadedScenario load(Path topologyFile) {
        Objects.requireNonNull(topologyFile, "topologyFile");
        if (!Files.isRegularFile(topologyFile)) {
            throw new TopologyValidationException("no existe el archivo de topología: " + topologyFile);
        }
        String content = read(topologyFile);
        Map<String, Object> root = asMap(parse(content), "el documento");

        try {
            String name = reqString(root, "name", "el documento");
            long seed = optLong(root, "seed", 0L);
            List<Component> components = parseComponents(root);
            List<Connection> connections = parseConnections(root);
            Workload workload = parseWorkload(root);
            TopologyGraph topology = TopologyGraph.of(name, components, connections);
            return new LoadedScenario(topology, workload, seed);
        } catch (IllegalArgumentException e) {
            // Mensajes de invariantes del dominio (rangos, referencias) -> error de validación.
            throw new TopologyValidationException(e.getMessage(), e);
        }
    }

    private String read(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length > MAX_BYTES) {
                throw new TopologyValidationException(
                    "el archivo supera el límite de " + MAX_BYTES + " bytes");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TopologyValidationException("no se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    private Object parse(String content) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_BYTES);
        options.setNestingDepthLimit(MAX_NESTING_DEPTH);
        options.setMaxAliasesForCollections(MAX_ALIASES);
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        Object root;
        try {
            root = yaml.load(content);
        } catch (YAMLException e) {
            // YAML malformado, claves duplicadas o tags no permitidos (p. ej. !!java...).
            throw new TopologyValidationException("YAML inválido: " + e.getMessage(), e);
        }
        if (root == null) {
            throw new TopologyValidationException("el documento YAML está vacío");
        }
        return root;
    }

    private List<Component> parseComponents(Map<String, Object> root) {
        List<?> raw = reqList(root.get("components"), "components");
        if (raw.isEmpty()) {
            throw new TopologyValidationException("'components' no puede estar vacío");
        }
        if (raw.size() > limits.maxComponents()) {
            throw new TopologyValidationException(
                "demasiados componentes: " + raw.size() + " (máximo " + limits.maxComponents() + ")");
        }
        List<Component> components = new ArrayList<>();
        for (Object element : raw) {
            Map<String, Object> map = asMap(element, "un componente");
            String id = reqString(map, "id", "un componente");
            String typeStr = reqString(map, "type", "el componente '" + id + "'");
            components.add(buildComponent(id, typeStr, map));
        }
        return components;
    }

    private Component buildComponent(String id, String typeStr, Map<String, Object> map) {
        ComponentType type = parseType(typeStr, id);
        String ctx = "el componente '" + id + "'";
        return switch (type) {
            case SERVICE -> new Service(id, reqInt(map, "capacity", ctx), reqLong(map, "base_latency_ms", ctx));
            case QUEUE -> new MessageQueue(id, reqInt(map, "max_size", ctx));
            case DATABASE -> new Database(id, reqInt(map, "max_connections", ctx),
                reqLong(map, "read_latency_ms", ctx));
            case LOAD_BALANCER -> new LoadBalancer(id);
        };
    }

    private static ComponentType parseType(String typeStr, String id) {
        return switch (typeStr.toLowerCase(Locale.ROOT)) {
            case "service" -> ComponentType.SERVICE;
            case "queue" -> ComponentType.QUEUE;
            case "database" -> ComponentType.DATABASE;
            case "loadbalancer", "load_balancer" -> ComponentType.LOAD_BALANCER;
            default -> throw new TopologyValidationException(
                "tipo desconocido '" + typeStr + "' en el componente '" + id
                + "' (válidos: Service, Queue, Database, LoadBalancer)");
        };
    }

    private List<Connection> parseConnections(Map<String, Object> root) {
        Object raw = root.get("connections");
        if (raw == null) {
            return List.of();
        }
        List<Connection> connections = new ArrayList<>();
        for (Object element : reqList(raw, "connections")) {
            Map<String, Object> map = asMap(element, "una conexión");
            String from = reqString(map, "from", "una conexión");
            Object to = map.get("to");
            if (to == null) {
                throw new TopologyValidationException("falta 'to' en la conexión desde '" + from + "'");
            }
            if (to instanceof String target) {
                connections.add(new Connection(from, target));
            } else if (to instanceof List<?> targets) {
                for (Object target : targets) {
                    if (!(target instanceof String s)) {
                        throw new TopologyValidationException(
                            "los destinos de '" + from + "' deben ser ids de texto");
                    }
                    connections.add(new Connection(from, s));
                }
            } else {
                throw new TopologyValidationException(
                    "'to' en la conexión desde '" + from + "' debe ser un id o una lista de ids");
            }
        }
        return connections;
    }

    private Workload parseWorkload(Map<String, Object> root) {
        Map<String, Object> map = asMap(req(root, "workload"), "workload");
        int rps = reqInt(map, "requests_per_second", "workload");
        int duration = reqInt(map, "duration_seconds", "workload");
        if (rps > limits.maxRequestsPerSecond()) {
            throw new TopologyValidationException(
                "requests_per_second=" + rps + " supera el máximo " + limits.maxRequestsPerSecond());
        }
        if (duration > limits.maxDurationSeconds()) {
            throw new TopologyValidationException(
                "duration_seconds=" + duration + " supera el máximo " + limits.maxDurationSeconds());
        }
        return new Workload(rps, duration);
    }

    // ---- helpers de lectura tipada con mensajes claros ----

    private static Object req(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new TopologyValidationException("falta la sección obligatoria '" + key + "'");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String ctx) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new TopologyValidationException(ctx + " debe ser un mapa de clave/valor");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static List<?> reqList(Object value, String ctx) {
        if (value == null) {
            throw new TopologyValidationException("falta la sección obligatoria '" + ctx + "'");
        }
        if (!(value instanceof List<?> list)) {
            throw new TopologyValidationException("'" + ctx + "' debe ser una lista");
        }
        return list;
    }

    private static String reqString(Map<String, Object> map, String key, String ctx) {
        Object value = map.get(key);
        if (value == null) {
            throw new TopologyValidationException("falta '" + key + "' en " + ctx);
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new TopologyValidationException("'" + key + "' en " + ctx + " debe ser texto no vacío");
        }
        return text;
    }

    private static int reqInt(Map<String, Object> map, String key, String ctx) {
        long value = reqLong(map, key, ctx);
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new TopologyValidationException("'" + key + "' en " + ctx + " está fuera de rango");
        }
        return (int) value;
    }

    private static long reqLong(Map<String, Object> map, String key, String ctx) {
        Object value = map.get(key);
        if (value == null) {
            throw new TopologyValidationException("falta '" + key + "' en " + ctx);
        }
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw new TopologyValidationException("'" + key + "' en " + ctx + " debe ser un entero");
    }

    private static long optLong(Map<String, Object> map, String key, long fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw new TopologyValidationException("'" + key + "' debe ser un entero");
    }
}
