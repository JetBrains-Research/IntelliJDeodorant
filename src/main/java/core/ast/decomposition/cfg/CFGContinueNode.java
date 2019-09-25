package core.ast.decomposition.cfg;

import com.intellij.psi.PsiContinueStatement;
import core.ast.decomposition.AbstractStatement;

public class CFGContinueNode extends CFGNode {
    private String label;
    private CFGNode innerMostLoopNode;

    public CFGContinueNode(AbstractStatement statement) {
        super(statement);
        PsiContinueStatement continueStatement = (PsiContinueStatement) statement.getStatement();
        if (continueStatement.getLabelIdentifier() != null)
            label = continueStatement.getLabelIdentifier().getText();
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
