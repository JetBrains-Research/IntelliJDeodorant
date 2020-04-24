package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;

import java.util.*;

public class MyMethod extends Entity {
    private String classOrigin;
    private String methodName;
    private final String returnType;
    private final List<String> parameterList;
    private MyMethodBody methodBody;
    private boolean isAbstract;
    private String access;
    private MethodObject methodObject;
    private volatile int hashCode = 0;

    MyMethod(String classOrigin, String methodName, String returnType, List<String> parameterList) {
        this.classOrigin = classOrigin;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this.isAbstract = false;
    }

    void setMethodObject(MethodObject methodObject) {
        this.methodObject = methodObject;
    }

    MethodObject getMethodObject() {
        return this.methodObject;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    void setMethodBody(MyMethodBody methodBody) {
        this.methodBody = methodBody;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public String getClassOrigin() {
        return classOrigin;
    }

    String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterList() {
        return parameterList;
    }

    void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
        if (this.methodBody != null)
            this.methodBody.setAttributeInstructionReference(myAttributeInstruction, reference);
    }

    private ListIterator<MyMethodInvocation> getMethodInvocationIterator() {

        if (this.methodBody != null)
            return this.methodBody.getMethodInvocationIterator();
        else
            return new ArrayList<MyMethodInvocation>().listIterator();
    }

    ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        if (this.methodBody != null)
            return this.methodBody.getAttributeInstructionIterator();
        else
            return new ArrayList<MyAttributeInstruction>().listIterator();
    }

    private boolean equals(MyMethodInvocation methodInvocation) {
        return this.classOrigin.equals(methodInvocation.getClassOrigin()) &&
                this.methodName.equals(methodInvocation.getMethodName()) &&
                this.returnType.equals(methodInvocation.getReturnType()) &&
                this.parameterList.equals(methodInvocation.getParameterList());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof MyMethod) {
            MyMethod method = (MyMethod) o;
            return this.classOrigin.equals(method.classOrigin) &&
                    this.methodName.equals(method.methodName) &&
                    this.returnType.equals(method.returnType) &&
                    this.parameterList.equals(method.parameterList);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + classOrigin.hashCode();
            result = 37 * result + methodName.hashCode();
            result = 37 * result + returnType.hashCode();
            for (String parameter : parameterList) {
                result = 37 * result + parameter.hashCode();
            }
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!classOrigin.equals(methodName))
            sb.append(classOrigin).append("::");
        sb.append(methodName);
        sb.append("(");
        if (!parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size() - 1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size() - 1));
        }
        sb.append(")");
        if (returnType != null)
            sb.append(":").append(returnType);
        return sb.toString();
    }

    public Set<String> getEntitySet() {
        Set<String> set = new HashSet<>();
        ListIterator<MyAttributeInstruction> attributeInstructionIterator = getAttributeInstructionIterator();
        while (attributeInstructionIterator.hasNext()) {
            MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
            if (!attributeInstruction.isReference())
                set.add(attributeInstruction.toString());
        }
        ListIterator<MyMethodInvocation> methodInvocationIterator = getMethodInvocationIterator();
        while (methodInvocationIterator.hasNext()) {
            MyMethodInvocation methodInvocation = methodInvocationIterator.next();
            if (!this.equals(methodInvocation))
                set.add(methodInvocation.toString());
        }
        return set;
    }

    public Set<String> getFullEntitySet() {
        Set<String> set = new HashSet<>();
        set.add(this.toString());
        ListIterator<MyAttributeInstruction> attributeInstructionIterator = getAttributeInstructionIterator();
        while (attributeInstructionIterator.hasNext()) {
            MyAttributeInstruction attributeInstruction = attributeInstructionIterator.next();
            set.add(attributeInstruction.toString());
        }
        ListIterator<MyMethodInvocation> methodInvocationIterator = getMethodInvocationIterator();
        while (methodInvocationIterator.hasNext()) {
            MyMethodInvocation methodInvocation = methodInvocationIterator.next();
            set.add(methodInvocation.toString());
        }
        return set;
    }
}
