package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiStatement;

public class InstanceOfConstructorInvocation implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiConstructorCall;
	}

}
