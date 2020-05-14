package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

public abstract class PDGDependence extends GraphEdge {
    private final PDGDependenceType type;

    PDGDependence(PDGNode src, PDGNode dst, PDGDependenceType type) {
        super(src, dst);
        this.type = type;
    }

    public GraphNode getSrc() {
        return src;
    }

    public GraphNode getDst() {
        return dst;
    }

    PDGDependenceType getType() {
        return type;
    }
}
