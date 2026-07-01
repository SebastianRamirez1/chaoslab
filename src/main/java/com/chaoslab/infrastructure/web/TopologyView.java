package com.chaoslab.infrastructure.web;

import com.chaoslab.domain.topology.Component;
import com.chaoslab.domain.topology.TopologyGraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista de la estructura de la topología para dibujar el grafo en el dashboard (nodos + aristas).
 *
 * @param entryPointId punto de entrada
 * @param nodes        componentes (id + tipo)
 * @param edges        conexiones dirigidas
 */
public record TopologyView(String entryPointId, List<NodeView> nodes, List<EdgeView> edges) {

    /** Un nodo del grafo. */
    public record NodeView(String id, String type) {
    }

    /** Una arista dirigida del grafo. */
    public record EdgeView(String from, String to) {
    }

    /** Construye la vista a partir del grafo del dominio. */
    public static TopologyView from(TopologyGraph topology) {
        List<NodeView> nodes = new ArrayList<>();
        List<EdgeView> edges = new ArrayList<>();
        for (Component component : topology.components()) {
            nodes.add(new NodeView(component.id(), component.type().name()));
            for (String target : topology.downstreams(component.id())) {
                edges.add(new EdgeView(component.id(), target));
            }
        }
        return new TopologyView(topology.entryPointId(), nodes, edges);
    }
}
