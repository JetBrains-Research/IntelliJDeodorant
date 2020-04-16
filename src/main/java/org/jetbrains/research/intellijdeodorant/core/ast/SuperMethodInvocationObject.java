package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiType;

import java.util.List;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(String originClassType, String methodName, PsiType returnType, List<PsiType> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

}
