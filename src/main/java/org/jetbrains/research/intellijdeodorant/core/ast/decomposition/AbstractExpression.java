package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.SmartPsiElementPointer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;

import java.util.List;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class AbstractExpression extends AbstractMethodFragment {
    private final SmartPsiElementPointer<PsiExpression> expression;

    public AbstractExpression(@NotNull PsiExpression expression) {
        super(null);
        this.expression = toPointer(expression);
        processExpression(expression);
    }

    public AbstractExpression(@NotNull PsiExpression expression, AbstractMethodFragment parent) {
        super(parent);
        this.expression = toPointer(expression);
        processExpression(expression);
    }

    private void processExpression(PsiExpression expression) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(expression);
        List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(expression);
        processVariables(expressionExtractor.getVariableInstructions(expression), assignments, postfixExpressions, prefixExpressions);
        processMethodInvocations(expressionExtractor.getMethodInvocations(expression));
        processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(expression));
        processArrayCreations(expressionExtractor.getArrayCreations(expression));
        processLiterals(expressionExtractor.getLiterals(expression));
    }

    private PsiExpression getExpression() {
        return this.expression.getElement();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((expression == null) ? 0 : getExpression().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractExpression other = (AbstractExpression) obj;
        if (expression == null) {
            return other.expression == null;
        } else return getExpression().equals(other.getExpression());
    }

    public String toString() {
        PsiExpression expression = getExpression();
        if (expression == null || expression.getType() == null) {
            return "";
        } else {
            return expression.getType().getCanonicalText();
        }
    }
}
