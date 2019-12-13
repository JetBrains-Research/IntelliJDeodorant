package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfArrayAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiArrayAccessExpression;
	}

}
