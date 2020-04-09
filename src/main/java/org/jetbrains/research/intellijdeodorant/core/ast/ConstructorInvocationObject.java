package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiType;

import java.util.List;

public class ConstructorInvocationObject extends AbstractMethodInvocationObject {

    public ConstructorInvocationObject(String originClassType, String methodName, PsiType returnType) {
        super(originClassType, methodName, returnType);
    }

    public ConstructorInvocationObject(String originClassType, String methodName, PsiType returnType, List<PsiType> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

/*    public void setConstructorInvocation(PsiConstructorCall constructorInvocation) {
        this.methodInvocation = toPointer(constructorInvocation);
    }*/

    public PsiConstructorCall getConstructorInvocation() {
        return (PsiConstructorCall) this.methodInvocation.getElement();
    }
}
