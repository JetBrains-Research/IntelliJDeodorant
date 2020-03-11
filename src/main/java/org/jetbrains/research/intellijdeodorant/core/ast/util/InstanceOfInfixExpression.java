package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfInfixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiBinaryExpression;
	}

}
