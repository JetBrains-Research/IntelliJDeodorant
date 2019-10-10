package core.ast.decomposition.cfg;

import com.intellij.psi.PsiBreakStatement;
import core.ast.decomposition.AbstractStatement;

public class CFGBreakNode extends CFGNode {
    private String label;
    private CFGNode innerMostLoopNode;

    public CFGBreakNode(AbstractStatement statement) {
        super(statement);
        PsiBreakStatement breakStatement = (PsiBreakStatement) statement.getStatement();
        if (breakStatement.getLabelExpression() != null
                && breakStatement.getLabelExpression().getQualifierExpression() != null)
            label = breakStatement.getLabelExpression().getQualifiedName();
    }

    public String getLabel() {
        return label;
    }

    public boolean isLabeled() {
        return label != null;
    }

    public CFGNode getInnerMostLoopNode() {
        return innerMostLoopNode;
    }

    public void setInnerMostLoopNode(CFGNode innerMostLoopNode) {
        this.innerMostLoopNode = innerMostLoopNode;
    }
}
