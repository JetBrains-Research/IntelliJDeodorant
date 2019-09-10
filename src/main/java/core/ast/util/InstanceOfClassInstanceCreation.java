package core.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNewExpression;

public class InstanceOfClassInstanceCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiNewExpression;
	}

}
