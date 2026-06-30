package com.chaoslab.domain.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El grafo del sistema: componentes conectados por aristas dirigidas. Hace cumplir sus
 * propias invariantes estructurales (ids únicos, referencias válidas, un único punto de
 * entrada), independientes del formato de entrada (YAML, etc.).
 */
public final class TopologyGraph {

    private final String name;
    private final Map<String, Component> components;
    private final Map<String, List<String>> adjacency;
    private final String entryPointId;
    private final List<Partition> activePartitions = new ArrayList<>();

    private TopologyGraph(String name, Map<String, Component> components,
                          Map<String, List<String>> adjacency, String entryPointId) {
        this.name = name;
        this.components = components;
        this.adjacency = adjacency;
        this.entryPointId = entryPointId;
    }

    /**
     * Construye y valida una topología.
     *
     * @param name        nombre de la topología
     * @param components  componentes (al menos uno, ids únicos)
     * @param connections aristas dirigidas; cada extremo debe referenciar un componente existente
     * @throws IllegalArgumentException si una referencia no existe o no hay exactamente un punto de entrada
     */
    public static TopologyGraph of(String name, List<Component> components, List<Connection> connections) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("la topología debe tener 'name'");
        }
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("la topología debe tener al menos un componente");
        }

        Map<String, Component> byId = new LinkedHashMap<>();
        for (Component c : components) {
            if (byId.put(c.id(), c) != null) {
                throw new IllegalArgumentException("id de componente duplicado: '" + c.id() + "'");
            }
        }

        Map<String, List<String>> adj = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (Component c : components) {
            adj.put(c.id(), new ArrayList<>());
            inDegree.put(c.id(), 0);
        }

        List<Connection> edges = connections == null ? List.of() : connections;
        for (Connection edge : edges) {
            if (!byId.containsKey(edge.from())) {
                throw new IllegalArgumentException(
                    "la conexión referencia un origen inexistente: '" + edge.from() + "'");
            }
            if (!byId.containsKey(edge.to())) {
                throw new IllegalArgumentException(
                    "la conexión referencia un destino inexistente: '" + edge.to() + "'");
            }
            adj.get(edge.from()).add(edge.to());
            inDegree.merge(edge.to(), 1, Integer::sum);
        }

        List<String> entryPoints = inDegree.entrySet().stream()
            .filter(e -> e.getValue() == 0)
            .map(Map.Entry::getKey)
            .toList();
        if (entryPoints.size() != 1) {
            throw new IllegalArgumentException(
                "la topología debe tener exactamente un punto de entrada (componente sin conexiones"
                + " entrantes); se encontraron: " + entryPoints);
        }

        return new TopologyGraph(name, byId, adj, entryPoints.get(0));
    }

    /** ¿Existe un componente con este id? */
    public boolean contains(String id) {
        return components.containsKey(id);
    }

    /** Componente por id. */
    public Component component(String id) {
        Component c = components.get(id);
        if (c == null) {
            throw new IllegalArgumentException("componente inexistente: '" + id + "'");
        }
        return c;
    }

    /**
     * Destinos a los que enrutar un request que terminó de procesarse en {@code fromId}.
     * Un {@link LoadBalancer} elige uno (round-robin); los demás reenvían a todos sus destinos.
     * Lista vacía = componente terminal.
     */
    public List<String> route(String fromId) {
        List<String> outgoing = adjacency.getOrDefault(fromId, List.of());
        if (outgoing.isEmpty()) {
            return List.of();
        }
        Component c = components.get(fromId);
        if (c instanceof LoadBalancer lb) {
            return List.of(lb.chooseFrom(outgoing));
        }
        return List.copyOf(outgoing);
    }

    public String name() {
        return name;
    }

    public String entryPointId() {
        return entryPointId;
    }

    /** Componentes en orden de declaración. */
    public Collection<Component> components() {
        return List.copyOf(components.values());
    }

    /** Activa una partición de red entre dos grupos de componentes (NetworkPartition). */
    public void partition(Set<String> groupA, Set<String> groupB) {
        activePartitions.add(new Partition(Set.copyOf(groupA), Set.copyOf(groupB)));
    }

    /** Cura (desactiva) una partición previamente activada entre los mismos grupos. */
    public void healPartition(Set<String> groupA, Set<String> groupB) {
        Set<String> a = Set.copyOf(groupA);
        Set<String> b = Set.copyOf(groupB);
        activePartitions.removeIf(p
            -> (p.groupA().equals(a) && p.groupB().equals(b))
            || (p.groupA().equals(b) && p.groupB().equals(a)));
    }

    /**
     * ¿Puede un request viajar de {@code from} a {@code to}? Falso si alguna partición activa
     * separa ambos componentes (uno en cada grupo).
     */
    public boolean isReachable(String from, String to) {
        for (Partition p : activePartitions) {
            boolean separated = (p.groupA().contains(from) && p.groupB().contains(to))
                || (p.groupB().contains(from) && p.groupA().contains(to));
            if (separated) {
                return false;
            }
        }
        return true;
    }

    private record Partition(Set<String> groupA, Set<String> groupB) {
    }
}
