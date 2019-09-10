package core.ast.util;


import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiPrefixExpression;

public class InstanceOfPrefixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiPrefixExpression;
	}

}
