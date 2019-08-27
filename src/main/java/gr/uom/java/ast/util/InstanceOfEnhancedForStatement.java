package gr.uom.java.ast.util;

import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfEnhancedForStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		if(statement instanceof PsiForeachStatement)
			return true;
		else
			return false;
	}

}
