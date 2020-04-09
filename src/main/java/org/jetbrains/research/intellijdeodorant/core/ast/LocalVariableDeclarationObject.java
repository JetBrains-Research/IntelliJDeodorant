package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;

public class LocalVariableDeclarationObject extends VariableDeclarationObject {
    private final PsiType type;
    private final String name;
    private PsiVariable variableDeclaration;
    private volatile int hashCode = 0;

    public LocalVariableDeclarationObject(PsiType type, String name) {
        this.type = type;
        this.name = name;
    }

    public PsiType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public PsiVariable getVariableDeclaration() {
        return variableDeclaration;
    }

    public void setVariableDeclaration(PsiVariable variableDeclaration) {
        this.variableDeclaration = variableDeclaration;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LocalVariableDeclarationObject) {
            LocalVariableDeclarationObject lvdo = (LocalVariableDeclarationObject) o;
            return this.name.equals(lvdo.name)
                    && this.type.equals(lvdo.type);
        }
        return false;
    }

    public boolean equals(LocalVariableInstructionObject lvio) {
        return this.name.equals(lvio.getName())
                && this.type.equals(lvio.getType());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + type.hashCode();
            result = 37 * result + name.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return type + " " + name;
    }
}
