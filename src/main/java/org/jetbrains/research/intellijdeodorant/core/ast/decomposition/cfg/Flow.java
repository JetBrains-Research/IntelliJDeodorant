package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.annotations.NotNull;

class Flow extends GraphEdge {
    private boolean loopbackFlow = false;
    private boolean trueControlFlow = false;
    private boolean falseControlFlow = false;

    Flow(@NotNull CFGNode src, @NotNull CFGNode dst) {
        super(src, dst);
        src.addOutgoingEdge(this);
        dst.addIncomingEdge(this);
    }

    boolean isLoopbackFlow() {
        return loopbackFlow;
    }

    void setLoopbackFlow(boolean loopbackFlow) {
        this.loopbackFlow = loopbackFlow;
    }

    boolean isTrueControlFlow() {
        return trueControlFlow;
    }

    void setTrueControlFlow(boolean trueControlFlow) {
        this.trueControlFlow = trueControlFlow;
    }

    boolean isFalseControlFlow() {
        return falseControlFlow;
    }

    public void setFalseControlFlow(boolean falseControlFlow) {
        this.falseControlFlow = falseControlFlow;
    }

    public String toString() {
        StringBuilder type = new StringBuilder();
        if (trueControlFlow)
            type.append("T");
        if (falseControlFlow)
            type.append("F");
        if (loopbackFlow)
            type.append("LB");
        return src.toString() + "-->" + type.toString() + "\n" + dst.toString();
    }
}
