package core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceExpression;

public class InstanceOfSimpleName implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiReferenceExpression;
	}

}
