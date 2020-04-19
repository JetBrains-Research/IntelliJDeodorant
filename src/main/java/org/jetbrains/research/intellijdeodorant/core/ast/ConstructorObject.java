package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ConstructorObject implements AbstractMethodDeclaration {

    String name;
    final List<PsiParameter> parameterList;
    Access access;
    String className;
    private MethodBodyObject methodBody;
    private final Set<String> exceptionsInJavaDocThrows;
    private SmartPsiElementPointer<PsiElement> methodDeclaration;
    private volatile int hashCode = 0;

    public ConstructorObject() {
        this.parameterList = new ArrayList<>();
        this.exceptionsInJavaDocThrows = new LinkedHashSet<>();
        this.access = Access.NONE;
    }

    public void setMethodDeclaration(PsiMethod methodDeclaration) {
        this.methodDeclaration = toPointer(methodDeclaration);
    }

    public PsiMethod getMethodDeclaration() {
        return (PsiMethod) this.methodDeclaration.getElement();
    }

    public void setMethodBody(MethodBodyObject methodBody) {
        this.methodBody = methodBody;
    }

    public MethodBodyObject getMethodBody() {
        return this.methodBody;
    }

    public void addExceptionInJavaDocThrows(String exception) {
        this.exceptionsInJavaDocThrows.add(exception);
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public void addParameter(PsiParameter parameter) {
        parameterList.add(parameter);
    }

    public ListIterator<PsiParameter> getParameterListIterator() {
        return parameterList.listIterator();
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        if (methodBody != null)
            return methodBody.getMethodInvocations();
        else
            return new ArrayList<>();
    }

    public List<PsiField> getFieldInstructions() {
        if (methodBody != null)
            return methodBody.getFieldInstructions();
        else
            return new ArrayList<>();
    }

    public List<PsiVariable> getLocalVariableDeclarations() {
        if (methodBody != null)
            return methodBody.getLocalVariableDeclarations();
        else
            return new ArrayList<>();
    }

    public List<PsiVariable> getLocalVariableInstructions() {
        if (methodBody != null)
            return methodBody.getLocalVariableInstructions();
        else
            return new ArrayList<>();
    }

    public List<CreationObject> getCreations() {
        if (methodBody != null)
            return methodBody.getCreations();
        else
            return new ArrayList<>();
    }

    public List<LiteralObject> getLiterals() {
        if (methodBody != null)
            return methodBody.getLiterals();
        else
            return new ArrayList<>();
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        if (methodBody != null)
            return methodBody.getAnonymousClassDeclarations();
        else
            return new ArrayList<>();
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
        if (methodBody != null)
            return methodBody.containsMethodInvocation(methodInvocation);
        else
            return false;
    }

    public boolean containsFieldInstruction(PsiField fieldInstruction) {
        if (methodBody != null)
            return methodBody.containsFieldInstruction(fieldInstruction);
        else
            return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        if (methodBody != null)
            return methodBody.containsSuperMethodInvocation(superMethodInvocation);
        else
            return false;
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields() {
        if (methodBody != null)
            return methodBody.getNonDistinctInvokedMethodsThroughFields();
        else
            return new LinkedHashMap<>();
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters() {
        if (methodBody != null)
            return methodBody.getNonDistinctInvokedMethodsThroughParameters();
        else
            return new LinkedHashMap<>();
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference() {
        if (methodBody != null)
            return methodBody.getNonDistinctInvokedMethodsThroughThisReference();
        else
            return new ArrayList<>();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
        if (methodBody != null)
            return methodBody.getNonDistinctDefinedFieldsThroughFields();
        else
            return new ArrayList<>();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
        if (methodBody != null)
            return methodBody.getNonDistinctUsedFieldsThroughFields();
        else
            return new ArrayList<>();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
        if (methodBody != null)
            return methodBody.getNonDistinctDefinedFieldsThroughParameters();
        else
            return new ArrayList<>();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
        if (methodBody != null)
            return methodBody.getNonDistinctUsedFieldsThroughParameters();
        else
            return new ArrayList<>();
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
        if (methodBody != null)
            return methodBody.getDefinedFieldsThroughThisReference();
        else
            return new LinkedHashSet<>();
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference() {
        if (methodBody != null)
            return methodBody.getNonDistinctDefinedFieldsThroughThisReference();
        else
            return new ArrayList<>();
    }

    public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference() {
        if (methodBody != null)
            return methodBody.getNonDistinctUsedFieldsThroughThisReference();
        else
            return new ArrayList<>();
    }

    public Set<PlainVariable> getDefinedLocalVariables() {
        if (methodBody != null)
            return methodBody.getDefinedLocalVariables();
        else
            return new LinkedHashSet<>();
    }

    public boolean containsSuperMethodInvocation() {
        if (methodBody != null)
            return methodBody.containsSuperMethodInvocation();
        else
            return false;
    }

    public boolean containsSuperFieldAccess() {
        if (methodBody != null)
            return methodBody.containsSuperFieldAccess();
        else
            return false;
    }

    public List<PsiType> getParameterTypeList() {
        List<PsiType> list = new ArrayList<>();
        for (PsiParameter parameterObject : parameterList)
            list.add(parameterObject.getType());
        return list;
    }

    public List<String> getParameterList() {
        List<String> list = new ArrayList<>();
        for (PsiParameter parameterObject : parameterList)
            list.add(parameterObject.getType().toString());
        return list;
    }

    public boolean equals(ClassInstanceCreationObject creationObject) {
        return this.className.equals(creationObject.getType()) &&
                equalParameterTypes(this.getParameterTypeList(), creationObject.getParameterTypeList());
    }

    public boolean equals(ConstructorInvocationObject constructorInvocationObject) {
        return this.className.equals(constructorInvocationObject.getOriginClassType()) &&
                equalParameterTypes(this.getParameterTypeList(), constructorInvocationObject.getParameterTypeList());
    }

    private boolean equalParameterTypes(List<PsiType> list1, List<PsiType> list2) {
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            PsiType type1 = list1.get(i);
            PsiType type2 = list2.get(i);
            if (!type1.equals(type2))
                return false;
            if (type1.getArrayDimensions() != type2.getArrayDimensions())
                return false;
        }
        return true;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ConstructorObject) {
            ConstructorObject constructorObject = (ConstructorObject) o;

            return this.className.equals(constructorObject.className) && this.name.equals(constructorObject.name) &&
                    this.parameterList.equals(constructorObject.parameterList);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + className.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + parameterList.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        sb.append(name);
        sb.append("(");
        if (!parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size() - 1; i++)
                sb.append(parameterList.get(i).toString()).append(", ");
            sb.append(parameterList.get(parameterList.size() - 1).toString());
        }
        sb.append(")");
        return sb.toString();
    }

}