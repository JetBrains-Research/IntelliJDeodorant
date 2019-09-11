package core.ast.decomposition.cfg;

import com.intellij.psi.PsiVariable;

public class PlainVariable extends AbstractVariable {
    private volatile int hashCode = 0;

    public PlainVariable(PsiVariable variableName) {
        super(variableName);
    }

    public PlainVariable(String variableQualifiedName, String variableName, String variableType, boolean isField, boolean isParameter, boolean isStatic) {
        super(variableQualifiedName, variableName, variableType, isField, isParameter, isStatic);
    }

    public boolean containsPlainVariable(PlainVariable variable) {
        return this.qualifiedName.equals(variable.qualifiedName);
    }

    public boolean startsWithVariable(AbstractVariable variable) {
        if (variable instanceof PlainVariable) {
            return this.equals(variable);
        }
        return false;
    }

    public PlainVariable getInitialVariable() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PlainVariable) {
            PlainVariable plain = (PlainVariable) o;
            return this.qualifiedName.equals(plain.qualifiedName);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 31 * result + qualifiedName.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return name;
    }
}
