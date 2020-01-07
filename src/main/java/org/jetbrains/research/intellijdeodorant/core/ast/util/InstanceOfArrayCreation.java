package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNewExpression;

public class InstanceOfArrayCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiNewExpression;
	}

}
