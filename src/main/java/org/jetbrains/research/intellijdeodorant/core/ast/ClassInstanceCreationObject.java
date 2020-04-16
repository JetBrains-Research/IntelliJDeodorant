package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.*;
import com.intellij.util.SmartList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ClassInstanceCreationObject extends CreationObject {

    private final SmartList<PsiType> parameterList;
    private final Set<String> thrownExceptions;

    public ClassInstanceCreationObject(String type) {
        super(type);
        this.parameterList = new SmartList<>();
        this.thrownExceptions = new LinkedHashSet<>();
    }

    public PsiNewExpression getClassInstanceCreation() {
        return (PsiNewExpression) this.creation.getElement();
    }

    public void setClassInstanceCreation(PsiExpression creation) {
        this.creation = toPointer(creation);
    }

    public void addParameter(PsiType parameterType) {
        parameterList.add(parameterType);
    }

    public List<PsiType> getParameterTypeList() {
        return this.parameterList;
    }

    public List<String> getParameterList() {
        SmartList<String> list = new SmartList<>();
        for (PsiType typeObject : parameterList)
            list.add(typeObject.toString());
        return list;
    }

    public void addThrownException(String type, PsiClass psiClass) {
        thrownExceptions.add(type);
    }

    public Set<String> getThrownExceptions() {
        return this.thrownExceptions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(getType());
        sb.append("(");
        if (!parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size() - 1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size() - 1));
        }
        sb.append(")");
        return sb.toString();
    }
}
