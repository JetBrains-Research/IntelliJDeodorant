package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfReturnStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiReturnStatement;
	}

}
