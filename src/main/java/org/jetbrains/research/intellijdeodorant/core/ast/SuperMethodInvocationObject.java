package org.jetbrains.research.intellijdeodorant.core.ast;

import java.util.List;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

}
