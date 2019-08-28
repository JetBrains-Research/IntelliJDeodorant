package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;

public interface ExpressionInstanceChecker {
    boolean instanceOf(PsiExpression expression);
}
