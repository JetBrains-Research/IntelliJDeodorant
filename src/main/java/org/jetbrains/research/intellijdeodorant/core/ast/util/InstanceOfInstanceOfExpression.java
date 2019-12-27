package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiInstanceOfExpression;

public class InstanceOfInstanceOfExpression implements
		ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiInstanceOfExpression;
	}

}
