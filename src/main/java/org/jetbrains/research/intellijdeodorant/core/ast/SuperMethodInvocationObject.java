package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiSuperExpression;

import java.util.List;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setSuperMethodInvocation(PsiSuperExpression superMethodInvocation) {
        this.methodInvocation = toPointer(superMethodInvocation);
    }

    public PsiSuperExpression getSuperMethodInvocation() {
        return (PsiSuperExpression) this.methodInvocation.getElement();
    }
}
