package gr.uom.java.ast.util;

import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSuperExpression;

public class InstanceOfConstructorInvocation implements StatementInstanceChecker {

	public boolean instanceOf(PsiStatement statement) {
		return statement instanceof PsiConstructorCall;
	}

}
