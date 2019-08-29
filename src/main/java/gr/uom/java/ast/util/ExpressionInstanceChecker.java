package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;

interface ExpressionInstanceChecker {
    boolean instanceOf(PsiExpression expression);
}
