package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiType;

import java.util.List;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(PsiType originClassType, String methodName, PsiType returnType) {
        super(originClassType.getCanonicalText(), methodName, returnType);
    }

    public SuperMethodInvocationObject(String originClassType, String methodName, PsiType returnType, List<PsiType> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setSuperMethodInvocation(PsiSuperExpression superMethodInvocation) {
        this.methodInvocation = toPointer(superMethodInvocation);
    }

    public PsiSuperExpression getSuperMethodInvocation() {
        return (PsiSuperExpression) this.methodInvocation.getElement();
    }
}
