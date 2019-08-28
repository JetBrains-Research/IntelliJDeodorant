package gr.uom.java.ast.util;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfVariableDeclarationStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiDeclarationStatement;
	}

}
