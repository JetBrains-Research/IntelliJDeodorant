package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

class CFGBlockNode extends CFGNode {

    private CFGNode controlParent;

    CFGBlockNode(AbstractStatement statement) {
        super(statement);
    }

    CFGNode getControlParent() {
        return controlParent;
    }

    void setControlParent(CFGNode controlParent) {
        this.controlParent = controlParent;
    }

}