package gr.uom.java.ast.decomposition;

import java.util.List;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;

public abstract class AbstractStatement extends AbstractMethodFragment {

    private ASTInformation statement;
    private StatementType type;

    public AbstractStatement(PsiElement statement, StatementType type, AbstractMethodFragment parent) {
        super(parent);
        this.type = type;
        this.statement = ASTInformationGenerator.generateASTInformation(statement);
    }

    public PsiStatement getStatement() {
        PsiElement element = this.statement.recoverASTNode();
        if (!(element instanceof PsiStatement)) return null;
        return (PsiStatement) element;
    }

    public StatementType getType() {
        return type;
    }

    public int getNestingDepth() {
        AbstractStatement parent = (AbstractStatement) this.getParent();
        int depth = 0;
        while (parent != null) {
            if (!parent.getType().equals(StatementType.BLOCK)) {
                depth++;
            }
            parent = (AbstractStatement) parent.getParent();
        }
        return depth;
    }

    public abstract List<String> stringRepresentation();
}
