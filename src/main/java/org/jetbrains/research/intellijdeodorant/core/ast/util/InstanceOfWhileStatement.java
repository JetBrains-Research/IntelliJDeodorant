package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiWhileStatement;

public class InstanceOfWhileStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiWhileStatement;
	}

}
