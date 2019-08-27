package gr.uom.java.ast.util;

import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiExpression;

public class InstanceOfAssignment implements ExpressionInstanceChecker {

	public boolean instanceOf(PsiExpression expression) {
		return expression instanceof PsiAssignmentExpression;
	}

}
