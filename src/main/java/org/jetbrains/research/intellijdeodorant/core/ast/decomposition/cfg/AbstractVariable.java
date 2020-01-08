package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiVariable;

public abstract class AbstractVariable {
    final PsiVariable origin;
    final String name;
    final String type;
    final boolean isField;
    final boolean isParameter;
    final boolean isStatic;

    AbstractVariable(PsiVariable psiVariable) {
        this.origin = psiVariable;
        this.name = psiVariable.getName();
        this.type = psiVariable.getType().getCanonicalText();
        this.isField = (psiVariable instanceof PsiField);
        this.isParameter = (psiVariable instanceof PsiParameter)
                && (psiVariable.getParent() instanceof PsiParameterList);
        this.isStatic = psiVariable.hasModifier(JvmModifier.STATIC);
    }

    AbstractVariable(PsiVariable origin, String name, String type, boolean isField, boolean isParameter, boolean isStatic) {
        this.origin = origin;
        this.name = name;
        this.type = type;
        this.isField = isField;
        this.isParameter = isParameter;
        this.isStatic = isStatic;
    }

    public PsiVariable getOrigin() {
        return origin;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isField() {
        return isField;
    }

    public boolean isParameter() {
        return isParameter;
    }

    boolean isStatic() {
        return isStatic;
    }

    protected abstract boolean containsPlainVariable(PlainVariable variable);

    protected abstract boolean startsWithVariable(AbstractVariable variable);

    public abstract PlainVariable getInitialVariable();
}
