package gr.uom.java.ast.util;


import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNewExpression;
import gr.uom.java.ast.SystemObject;

public class InstanceOfClassInstanceCreation implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		if(expression instanceof PsiNewExpression)
			return true;
		else
			return false;
	}

}
