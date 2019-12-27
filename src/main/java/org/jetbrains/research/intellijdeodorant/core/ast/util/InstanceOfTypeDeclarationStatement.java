package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.*;

public class InstanceOfTypeDeclarationStatement implements StatementInstanceChecker {

    public boolean instanceOf(PsiStatement statement) {
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            for (PsiElement element : declaredElements) {
                if (element instanceof PsiClass) {
                    return true;
                }
            }
        }
        return false;
    }
}
