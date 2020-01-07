package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.*;

public class InstanceOfVariableModifier implements ExpressionInstanceChecker {

    public boolean instanceOf(PsiExpression expression) {
        return expression instanceof PsiAssignmentExpression || expression instanceof PsiPrefixExpression
                || expression instanceof PsiPostfixExpression || expression instanceof PsiMethodCallExpression;
    }
}
