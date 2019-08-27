package gr.uom.java.ast.util;

import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiTryStatement;

public class InstanceOfTryStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		if(statement instanceof PsiTryStatement)
			return true;
		else
			return false;
	}

}
