package core.ast.decomposition.cfg;

import com.intellij.psi.PsiContinueStatement;
import core.ast.decomposition.AbstractStatement;

class CFGContinueNode extends CFGNode {
    private String label;
    private CFGNode innerMostLoopNode;

    CFGContinueNode(AbstractStatement statement) {
        super(statement);
        PsiContinueStatement continueStatement = (PsiContinueStatement) statement.getStatement();
        if (continueStatement.getLabelIdentifier() != null)
            label = continueStatement.getLabelIdentifier().getText();
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
