package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.SmartPsiElementPointer;

import java.util.List;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

/**
 * Represents a PsiStatement (the equivalent of Statement from Eclipse AST).
 * JDeodorant uses Eclipse AST, which represents method body as a Block statement.
 * To represent method body, IntelliJ SDK uses PsiCodeBlock, which doesn't extend a PsiStatement.
 * It is for that reason we cover two cases (PsiStatement and PsiCodeBlock) here by using common parent PsiElement.
 */
public abstract class AbstractStatement extends AbstractMethodFragment {
    private final SmartPsiElementPointer<PsiElement> statement;
    private final StatementType type;

    AbstractStatement(PsiElement statement, StatementType type, AbstractMethodFragment parent) {
        super(parent);
        this.type = type;
        this.statement = toPointer(statement);
    }

    public PsiElement getStatement() {
        PsiElement element = this.statement.getElement();
        if (element instanceof PsiStatement || element instanceof PsiCodeBlock) {
            return element;
        } else {
            return null;
        }
    }

    StatementType getType() {
        return type;
    }

    protected abstract List<String> stringRepresentation();
}
