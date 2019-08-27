package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiSuperExpression;

public class InstanceOfSuperFieldAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiSuperExpression)
			return true;
		else
			return false;
	}

}
