package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiInstanceOfExpression;

public class InstanceOfInstanceofExpression implements
		ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiInstanceOfExpression)
			return true;
		else
			return false;
	}

}
