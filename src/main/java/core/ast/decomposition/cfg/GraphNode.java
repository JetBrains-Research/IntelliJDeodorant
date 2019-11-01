package core.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

public class GraphNode {
    private static int nodeNum = 0;
    protected int id;
    protected Set<GraphEdge> incomingEdges;
    protected Set<GraphEdge> outgoingEdges;

    GraphNode() {
        nodeNum++;
        this.id = nodeNum;
        this.incomingEdges = new LinkedHashSet<>();
        this.outgoingEdges = new LinkedHashSet<>();
    }

    public int getId() {
        return id;
    }

    void addIncomingEdge(GraphEdge edge) {
        incomingEdges.add(edge);
    }

    void addOutgoingEdge(GraphEdge edge) {
        outgoingEdges.add(edge);
    }

    static void resetNodeNum() {
        nodeNum = 0;
    }
}
