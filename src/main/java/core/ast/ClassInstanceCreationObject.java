package core.ast;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class ClassInstanceCreationObject extends CreationObject {

    private final List<TypeObject> parameterList;
    private final Set<String> thrownExceptions;

    public ClassInstanceCreationObject(TypeObject type) {
        super(type);
        this.parameterList = new ArrayList<>();
        this.thrownExceptions = new LinkedHashSet<>();
    }

    public PsiExpression getClassInstanceCreation() {
        return (PsiExpression) this.creation.recoverASTNode();
    }

    public void setClassInstanceCreation(PsiExpression creation) {
        this.creation = ASTInformationGenerator.generateASTInformation(creation);
    }

    public void addParameter(TypeObject parameterType) {
        parameterList.add(parameterType);
    }

    public ListIterator<TypeObject> getParameterListIterator() {
        return parameterList.listIterator();
    }

    public List<TypeObject> getParameterTypeList() {
        return this.parameterList;
    }

    public List<String> getParameterList() {
        List<String> list = new ArrayList<>();
        for (TypeObject typeObject : parameterList)
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
        sb.append(getType().toString());
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
