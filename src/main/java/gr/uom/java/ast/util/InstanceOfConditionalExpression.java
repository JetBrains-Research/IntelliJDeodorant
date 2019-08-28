package gr.uom.java.ast.util;


import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfConditionalExpression implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiConditionalExpression;
	}

}
