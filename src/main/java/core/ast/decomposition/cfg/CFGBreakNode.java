package core.ast.decomposition.cfg;

import com.intellij.psi.PsiBreakStatement;
import core.ast.decomposition.AbstractStatement;

class CFGBreakNode extends CFGNode {
    private String label;
    private CFGNode innerMostLoopNode;

    CFGBreakNode(AbstractStatement statement) {
        super(statement);
        PsiBreakStatement breakStatement = (PsiBreakStatement) statement.getStatement();
        if (breakStatement.getLabelExpression() != null
                && breakStatement.getLabelExpression().getQualifierExpression() != null)
            label = breakStatement.getLabelExpression().getQualifiedName();
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
