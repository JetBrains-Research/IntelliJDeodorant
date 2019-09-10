package core.ast.util;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfTypeDeclarationStatement implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiDeclarationStatement;
	}

}
