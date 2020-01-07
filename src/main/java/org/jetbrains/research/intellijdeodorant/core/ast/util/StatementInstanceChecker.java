package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiStatement;

interface StatementInstanceChecker {
	boolean instanceOf(PsiStatement statement);
}
