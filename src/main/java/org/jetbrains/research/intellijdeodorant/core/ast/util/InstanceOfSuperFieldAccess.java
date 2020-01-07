package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiSuperExpression;

public class InstanceOfSuperFieldAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiSuperExpression;
	}

}
