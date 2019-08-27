package gr.uom.java.ast.util;

import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNewExpression;

public class InstanceOfArrayCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiNewExpression)
			return true;
		else
			return false;
	}

}
