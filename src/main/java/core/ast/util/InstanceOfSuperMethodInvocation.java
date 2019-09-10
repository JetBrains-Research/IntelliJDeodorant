package core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiSuperExpression;

public class InstanceOfSuperMethodInvocation implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiSuperExpression;
	}

}
