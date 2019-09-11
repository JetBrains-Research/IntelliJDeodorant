package core.ast;

import com.intellij.psi.PsiElement;

abstract class VariableDeclarationObject {

    String variableBindingKey;

    String getVariableBindingKey() {
        return variableBindingKey;
    }

    public abstract PsiElement getVariableDeclaration();

    public abstract String getName();
}
