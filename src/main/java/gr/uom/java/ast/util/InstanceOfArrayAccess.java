package gr.uom.java.ast.util;

import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfArrayAccess implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiArrayAccessExpression)
			return true;
		else
			return false;
	}

}
