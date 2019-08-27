package gr.uom.java.ast.util;

import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfInfixExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiBinaryExpression)
			return true;
		else
			return false;
	}

}
