package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;
import com.intellij.psi.PsiElement;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public abstract class AbstractVariable {
    final SmartPsiElementPointer<PsiElement> origin; //Could be either PsiVariable or PsiClass in order to represent "this" element, that in Eclipse is instance of IVariableBinding, but not in IDEA.
    final String name;
    final String type;
    final boolean isField;
    final boolean isParameter;
    final boolean isStatic;

    AbstractVariable(PsiVariable psiVariable) {
        this.origin = toPointer(psiVariable);
        this.name = psiVariable.getName();
        this.type = psiVariable.getType().getCanonicalText();
        this.isField = (psiVariable instanceof PsiField);
        this.isParameter = (psiVariable instanceof PsiParameter)
                && (psiVariable.getParent() instanceof PsiParameterList);
        this.isStatic = psiVariable.hasModifier(JvmModifier.STATIC);
    }

    AbstractVariable(PsiElement origin, String name, String type, boolean isField, boolean isParameter, boolean isStatic) {
        this.origin = toPointer(origin);
        this.name = name;
        this.type = type;
        this.isField = isField;
        this.isParameter = isParameter;
        this.isStatic = isStatic;
    }

    public PsiElement getOrigin() {
        return origin.getElement();
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
