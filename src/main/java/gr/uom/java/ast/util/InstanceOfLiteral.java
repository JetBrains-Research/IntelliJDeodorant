package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;

public class InstanceOfLiteral implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiLiteralExpression)
			return true;
		else
			return false;
	}

}
