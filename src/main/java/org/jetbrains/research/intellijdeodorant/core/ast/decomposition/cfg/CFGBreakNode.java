package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiBreakStatement;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

class CFGBreakNode extends CFGNode {
    private String label;
    private CFGNode innerMostLoopNode;

    CFGBreakNode(AbstractStatement statement) {
        super(statement);
        PsiBreakStatement breakStatement = (PsiBreakStatement) statement.getStatement();
        if (breakStatement.getLabelIdentifier() != null)
            label = breakStatement.getLabelIdentifier().getText();
    }

    String getLabel() {
        return label;
    }

    boolean isLabeled() {
        return label != null;
    }

    CFGNode getInnerMostLoopNode() {
        return innerMostLoopNode;
    }

    void setInnerMostLoopNode(CFGNode innerMostLoopNode) {
        this.innerMostLoopNode = innerMostLoopNode;
    }
}
