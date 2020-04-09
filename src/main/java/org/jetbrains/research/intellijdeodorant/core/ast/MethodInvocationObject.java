package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;

import java.util.List;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class MethodInvocationObject extends AbstractMethodInvocationObject {

    public MethodInvocationObject(String originClassType, String methodName, PsiType returnType) {
        super(originClassType, methodName, returnType);
    }

    public MethodInvocationObject(String originClassType, String methodName, PsiType returnType, List<PsiType> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setMethodInvocation(PsiMethodCallExpression methodInvocation) {
        this.methodInvocation = toPointer(methodInvocation);
    }

    public PsiMethodCallExpression getMethodInvocation() {
        return (PsiMethodCallExpression) this.methodInvocation.getElement();
    }
}