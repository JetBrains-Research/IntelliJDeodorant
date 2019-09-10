package core.ast.util;


import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiPostfixExpression;

public class InstanceOfPostfixExpression implements ExpressionInstanceChecker {

    public boolean instanceOf(PsiExpression expression) {
        return expression instanceof PsiPostfixExpression;
    }

}
