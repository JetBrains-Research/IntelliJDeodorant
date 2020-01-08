package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSwitchStatement;

public class InstanceOfSwitchStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiSwitchStatement;
	}

}
