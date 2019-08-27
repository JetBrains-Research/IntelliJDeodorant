package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiVariable;

public class InstanceOfVariableDeclarationExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiVariable)
			return true;
		else
			return false;
	}

}
