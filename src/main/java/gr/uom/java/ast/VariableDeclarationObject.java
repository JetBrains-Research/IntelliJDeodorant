package gr.uom.java.ast;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiReference;

public abstract class VariableDeclarationObject {

    protected PsiReference variableBindingKey;

    public PsiReference getVariableBindingKey() {
        return variableBindingKey;
    }

    public abstract PsiDeclarationStatement getVariableDeclaration();

    public abstract String getName();
}
