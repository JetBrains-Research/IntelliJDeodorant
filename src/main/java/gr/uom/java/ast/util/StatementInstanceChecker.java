package gr.uom.java.ast.util;

import com.intellij.psi.PsiStatement;

public interface StatementInstanceChecker {
	public boolean instanceOf(PsiStatement statement);
}
