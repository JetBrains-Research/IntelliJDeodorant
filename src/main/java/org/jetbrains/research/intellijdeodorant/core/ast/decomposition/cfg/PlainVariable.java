package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;

public class PlainVariable extends AbstractVariable {
    private volatile int hashCode = 0;

    public PlainVariable(PsiVariable variable) {
        super(variable);
    }

    public PlainVariable(PsiElement origin, String variableName, String variableType, boolean isField,
                         boolean isParameter, boolean isStatic) {
        super(origin, variableName, variableType, isField, isParameter, isStatic);
    }

    public PlainVariable getInitialVariable() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PlainVariable) {
            PlainVariable plain = (PlainVariable) o;
            return this.getOrigin().equals(plain.getOrigin());
        }
        if (o instanceof String) {
            return o.equals("this") && this.getOrigin() instanceof PsiClass;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 31 * result + this.getOrigin().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return this.getName();
    }
}
