package core.ast.decomposition.cfg;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;

public abstract class AbstractVariable {
    final String qualifiedName;
    final String name;
    final String type;
    final boolean isField;
    final boolean isParameter;
    final boolean isStatic;

    AbstractVariable(PsiVariable psiVariable) {
        this.qualifiedName = psiVariable.getType().getCanonicalText();
        this.name = psiVariable.getText();
        this.type = psiVariable.getType().getPresentableText();
        this.isField = (psiVariable instanceof PsiField);
        this.isParameter = (psiVariable instanceof PsiParameter);
        this.isStatic = psiVariable.hasModifier(JvmModifier.STATIC);
    }

    AbstractVariable(String qualifiedName, String name, String type, boolean isField, boolean isParameter, boolean isStatic) {
        this.qualifiedName = qualifiedName;
        this.name = name;
        this.type = type;
        this.isField = isField;
        this.isParameter = isParameter;
        this.isStatic = isStatic;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isField() {
        return isField;
    }

    public boolean isParameter() {
        return isParameter;
    }

    boolean isStatic() {
        return isStatic;
    }

    protected abstract boolean containsPlainVariable(PlainVariable variable);

    protected abstract boolean startsWithVariable(AbstractVariable variable);

    public abstract PlainVariable getInitialVariable();
}
