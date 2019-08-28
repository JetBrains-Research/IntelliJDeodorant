package gr.uom.java.ast.util;

import com.intellij.psi.PsiBreakStatement;
import com.intellij.psi.PsiContinueStatement;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;

public class InstanceOfBranchingStatement implements StatementInstanceChecker {

    public boolean instanceOf(PsiStatement statement) {
        return statement instanceof PsiBreakStatement || statement instanceof PsiContinueStatement
                || statement instanceof PsiReturnStatement;
    }

}
