package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ParameterObject extends VariableDeclarationObject {
    private final PsiType type;
    private final String name;
    private final boolean varargs;
    private SmartPsiElementPointer<PsiElement> singleVariableDeclaration;
    private volatile int hashCode = 0;

    public ParameterObject(PsiType type, String name, boolean varargs) {
        this.type = type;
        this.name = name;
        this.varargs = varargs;
    }

    public PsiType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isVarargs() {
        return varargs;
    }

    public void setSingleVariableDeclaration(PsiVariable singleVariableDeclaration) {
        this.singleVariableDeclaration = toPointer(singleVariableDeclaration);
    }

    public PsiVariable getSingleVariableDeclaration() {
        return (PsiVariable) this.singleVariableDeclaration.getElement();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ParameterObject) {
            ParameterObject parameterObject = (ParameterObject) o;
            return this.type.equals(parameterObject.type)
                    && this.name.equals(parameterObject.name)
                    && this.varargs == parameterObject.varargs;
        }

        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
            result = 37 * result + (varargs ? 1 : 0);
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return type.toString() + " " + name;
    }

    public PsiVariable getVariableDeclaration() {
        return getSingleVariableDeclaration();
    }
}
