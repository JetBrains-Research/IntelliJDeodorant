package gr.uom.java.ast.util;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiStatement;

public class InstanceOfTypeDeclarationStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiClass;
	}

}
