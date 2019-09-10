package core.ast.decomposition;

import java.util.List;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import core.ast.ASTInformation;
import core.ast.ASTInformationGenerator;

public abstract class AbstractStatement extends AbstractMethodFragment {

    private final ASTInformation statement;
    private final StatementType type;

    AbstractStatement(PsiElement statement, StatementType type, AbstractMethodFragment parent) {
        super(parent);
        this.type = type;
        this.statement = ASTInformationGenerator.generateASTInformation(statement);
    }

    public PsiStatement getStatement() {
        PsiElement element = this.statement.recoverASTNode();
        if (!(element instanceof PsiStatement)) return null;
        return (PsiStatement) element;
    }

    StatementType getType() {
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

    protected abstract List<String> stringRepresentation();
}
