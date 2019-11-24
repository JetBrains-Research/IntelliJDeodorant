package core.ast.decomposition;

import java.util.List;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import core.ast.ASTInformation;
import core.ast.ASTInformationGenerator;

/**
 * Represents a PsiStatement (the equivalent of Statement from Eclipse AST).
 * JDeodorant uses Eclipse AST, which represents method body as a Block statement.
 * To represent method body, IntelliJ SDK uses PsiCodeBlock, which doesn't extend a PsiStatement.
 * It is for that reason we cover two cases (PsiStatement and PsiCodeBlock) here by using common parent PsiElement.
 */
public abstract class AbstractStatement extends AbstractMethodFragment {

    private final ASTInformation statement;
    private final StatementType type;

    AbstractStatement(PsiElement statement, StatementType type, AbstractMethodFragment parent) {
        super(parent);
        this.type = type;
        this.statement = ASTInformationGenerator.generateASTInformation(statement);
    }

    public PsiElement getStatement() {
        PsiElement element = this.statement.recoverASTNode();
        if (element instanceof PsiStatement || element instanceof PsiCodeBlock) {
            return element;
        } else {
            return null;
        }
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
