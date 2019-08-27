package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;

public interface ExpressionInstanceChecker {
	public boolean instanceOf(PsiExpression expression);
}
