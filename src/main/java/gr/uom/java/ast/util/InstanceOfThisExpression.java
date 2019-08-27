package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiThisExpression;

public class InstanceOfThisExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiThisExpression)
			return true;
		else
			return false;
	}

}
