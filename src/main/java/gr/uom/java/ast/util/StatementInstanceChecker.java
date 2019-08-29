package gr.uom.java.ast.util;

import com.intellij.psi.PsiStatement;

interface StatementInstanceChecker {
	boolean instanceOf(PsiStatement statement);
}
