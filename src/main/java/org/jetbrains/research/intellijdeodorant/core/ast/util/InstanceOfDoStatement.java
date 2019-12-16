package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfDoStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiDoWhileStatement;
	}

}
