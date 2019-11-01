package core.ast.decomposition.cfg;

public class PDGControlDependence extends PDGDependence {
    private boolean trueControlDependence;
    private volatile int hashCode = 0;

    PDGControlDependence(PDGNode src, PDGNode dst, boolean trueControlDependence) {
        super(src, dst, PDGDependenceType.CONTROL);
        this.trueControlDependence = trueControlDependence;
        src.addOutgoingEdge(this);
        dst.addIncomingEdge(this);
    }

    boolean isTrueControlDependence() {
        return trueControlDependence;
    }

    public boolean isFalseControlDependence() {
        return !trueControlDependence;
    }

    public boolean sameLabel(PDGControlDependence other) {
        return this.trueControlDependence == other.trueControlDependence;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof PDGControlDependence) {
            PDGControlDependence controlDependence = (PDGControlDependence) o;
            return this.src.equals(controlDependence.src)
                    && this.dst.equals(controlDependence.dst)
                    && this.trueControlDependence == controlDependence.trueControlDependence;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + src.hashCode();
            result = 37 * result + dst.hashCode();
            result = 37 * result + Boolean.valueOf(trueControlDependence).hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder type = new StringBuilder();
        if (trueControlDependence)
            type.append("T");
        else
            type.append("F");
        return src.toString() + "-->" + type.toString() + "\n" + dst.toString();
    }
}

