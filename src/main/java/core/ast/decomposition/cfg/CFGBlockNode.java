package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

public class CFGBlockNode extends CFGNode {

    private CFGNode controlParent;

    public CFGBlockNode(AbstractStatement statement) {
        super(statement);
    }

    public CFGNode getControlParent() {
        return controlParent;
    }

    public void setControlParent(CFGNode controlParent) {
        this.controlParent = controlParent;
    }

}