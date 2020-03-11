package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

public abstract class PDGAbstractDataDependence extends PDGDependence {
    private final AbstractVariable data;
    private final CFGBranchNode loop;
    private volatile int hashCode = 0;

    PDGAbstractDataDependence(PDGNode src, PDGNode dst, PDGDependenceType type,
                              AbstractVariable data, CFGBranchNode loop) {
        super(src, dst, type);
        this.data = data;
        this.loop = loop;
        src.addOutgoingEdge(this);
        dst.addIncomingEdge(this);
    }

    public AbstractVariable getData() {
        return data;
    }

    public CFGBranchNode getLoop() {
        return loop;
    }

    public boolean isLoopCarried() {
        return loop != null;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof PDGAbstractDataDependence) {
            PDGAbstractDataDependence dataDependence = (PDGAbstractDataDependence) o;
            boolean equalLoop = false;
            if (this.loop != null && dataDependence.loop != null)
                equalLoop = this.loop.equals(dataDependence.loop);
            if (this.loop == null && dataDependence.loop == null)
                equalLoop = true;
            return this.src.equals(dataDependence.src)
                    && this.dst.equals(dataDependence.dst)
                    && this.data.equals(dataDependence.data)
                    && equalLoop
                    && this.getType().equals(dataDependence.getType());
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + src.hashCode();
            result = 37 * result + dst.hashCode();
            result = 37 * result + data.hashCode();
            if (loop != null)
                result = 37 * result + loop.hashCode();
            result = 37 * result + getType().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        String loopInfo = isLoopCarried() ? " through loop " + loop.getId() : "";
        return src.toString() + "-->" + data.toString() +
                " <" + getType().toString().toLowerCase() + ">" +
                loopInfo +
                "\n" + dst.toString();
    }
}
