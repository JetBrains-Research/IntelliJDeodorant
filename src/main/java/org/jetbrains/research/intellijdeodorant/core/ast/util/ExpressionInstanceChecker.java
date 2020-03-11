package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiExpression;

interface ExpressionInstanceChecker {
    boolean instanceOf(PsiExpression expression);
}
