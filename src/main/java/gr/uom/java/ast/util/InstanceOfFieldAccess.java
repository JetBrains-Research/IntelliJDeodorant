package gr.uom.java.ast.util;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;

public class InstanceOfFieldAccess implements ExpressionInstanceChecker {
    public boolean instanceOf(PsiExpression expression) {
        return PsiTreeUtil.findChildrenOfType(expression, PsiField.class).size() > 0;
    }

}
