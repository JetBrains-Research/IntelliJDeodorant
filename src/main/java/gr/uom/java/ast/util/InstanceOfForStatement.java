package gr.uom.java.ast.util;

import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfForStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiForStatement;
	}

}
