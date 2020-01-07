package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

public class InstanceOfMethodInvocation implements ExpressionInstanceChecker {

    public boolean instanceOf(PsiExpression expression) {
        return expression instanceof PsiMethodCallExpression;
    }
}
