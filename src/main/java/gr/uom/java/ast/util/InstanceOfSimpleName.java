package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceExpression;

public class InstanceOfSimpleName implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiReferenceExpression)
			return true;
		else
			return false;
	}

}
