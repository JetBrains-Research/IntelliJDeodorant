package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;

public class InstanceOfVariableDeclarationStatement implements StatementInstanceChecker {

    public boolean instanceOf(PsiStatement statement) {
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            for (PsiElement element : declaredElements) {
                if (element instanceof PsiVariable) {
                    return true;
                }
            }
        }
        return false;
    }

}
