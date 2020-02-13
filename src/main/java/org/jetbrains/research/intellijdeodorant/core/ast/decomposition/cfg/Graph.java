package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

public class Graph {
    protected final Set<GraphNode> nodes;
    protected final Set<GraphEdge> edges;

    Graph() {
        this.nodes = new LinkedHashSet<>();
        this.edges = new LinkedHashSet<>();
    }

    public Set<GraphNode> getNodes() {
        return nodes;
    }

    public Set<GraphEdge> getEdges() {
        return edges;
    }

    public void addNode(GraphNode node) {
        nodes.add(node);
    }

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }
}
