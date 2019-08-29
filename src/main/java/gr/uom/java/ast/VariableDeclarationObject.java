package gr.uom.java.ast;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiReference;

abstract class VariableDeclarationObject {

    PsiReference variableBindingKey;

    PsiReference getVariableBindingKey() {
        return variableBindingKey;
    }

    public abstract PsiDeclarationStatement getVariableDeclaration();

    public abstract String getName();
}
