package core.ast.util;

import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfEnhancedForStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiForeachStatement;
	}

}
