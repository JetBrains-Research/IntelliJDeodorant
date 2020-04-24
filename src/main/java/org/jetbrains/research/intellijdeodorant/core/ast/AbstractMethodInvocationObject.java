package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.SmartPsiElementPointer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractMethodInvocationObject {
    private final TypeObject originClassType;
    private final String methodName;
    private final TypeObject returnType;
    private final List<TypeObject> parameterList;
    private final Set<String> thrownExceptions;
    private boolean _static;
    SmartPsiElementPointer<PsiExpression> methodInvocation;
    private volatile int hashCode = 0;

    AbstractMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        this.originClassType = originClassType;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = new ArrayList<>();
        this.thrownExceptions = new LinkedHashSet<>();
        this._static = false;
    }

    AbstractMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        this.originClassType = originClassType;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this.thrownExceptions = new LinkedHashSet<>();
        this._static = false;
    }


    public List<TypeObject> getParameterTypeList() {
        return this.parameterList;
    }

    public TypeObject getReturnType() {
        return returnType;
    }

    public String getOriginClassName() {
        return this.originClassType.getClassType();
    }

    public String getMethodName() {
        return this.methodName;
    }

    public List<String> getParameterList() {
        List<String> list = new ArrayList<>();
        for (TypeObject typeObject : parameterList)
            list.add(typeObject.toString());
        return list;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public Set<String> getThrownExceptions() {
        return this.thrownExceptions;
    }

    public TypeObject getType() {
        return getReturnType();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof AbstractMethodInvocationObject) {
            AbstractMethodInvocationObject methodInvocationObject = (AbstractMethodInvocationObject) o;

            return originClassType.equals(methodInvocationObject.originClassType) &&
                    methodName.equals(methodInvocationObject.methodName) &&
                    returnType.equals(methodInvocationObject.returnType) &&
                    parameterList.equals(methodInvocationObject.parameterList);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + originClassType.hashCode();
            result = 37 * result + methodName.hashCode();
            result = 37 * result + returnType.hashCode();
            for (TypeObject parameter : parameterList)
                result = 37 * result + parameter.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(originClassType).append("::");
        sb.append(methodName);
        sb.append("(");
        if (!parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size() - 1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size() - 1));
        }
        sb.append(")");
        sb.append(":").append(returnType);
        return sb.toString();
    }

}
