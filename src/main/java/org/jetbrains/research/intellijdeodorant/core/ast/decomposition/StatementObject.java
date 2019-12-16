package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * The class represents the following types of {@link PsiStatement}:
 * {@link PsiExpressionStatement}
 * {@link PsiDeclarationStatement}
 * {@link PsiConstructorCall}
 * {@link PsiReturnStatement}
 * {@link PsiAssertStatement}
 * {@link PsiBreakStatement}
 * {@link PsiContinueStatement}
 * {@link PsiSwitchStatement}
 * {@link PsiEmptyStatement}
 * {@link PsiThrowStatement}
 */

public class StatementObject extends AbstractStatement {

    public StatementObject(PsiStatement statement, StatementType type, AbstractMethodFragment parent) {
        super(statement, type, parent);

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
        List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
        processVariables(expressionExtractor.getVariableInstructions(statement), assignments, postfixExpressions, prefixExpressions);
        processLocalVariableDeclaration(statement);
        processMethodInvocations(expressionExtractor.getMethodInvocations(statement));
        processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(statement));
        processArrayCreations(expressionExtractor.getArrayCreations(statement));
        processLiterals(expressionExtractor.getLiterals(statement));
        if (statement instanceof PsiThrowStatement) {
            processThrowStatement((PsiThrowStatement) statement);
        }
    }

    public String toString() {
        return getStatement().toString();
    }

    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        return stringRepresentation;
    }
}
