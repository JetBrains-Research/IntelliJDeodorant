package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;

public class CompositeVariable extends AbstractVariable {
    private final AbstractVariable rightPart;
    private volatile int hashCode = 0;

    public CompositeVariable(PsiVariable referenceName, AbstractVariable rightPart) {
        super(referenceName);
        this.rightPart = rightPart;
    }

    public CompositeVariable(AbstractVariable argument, AbstractVariable rightPart) {
        this(argument.origin, argument.getName(),
                argument.getType(), argument.isField(), argument.isParameter(), argument.isStatic(), rightPart);
    }

    private CompositeVariable(PsiElement origin, String variableName, String variableType,
                              boolean isField, boolean isParameter, boolean isStatic, AbstractVariable rightPart) {
        super(origin, variableName, variableType, isField, isParameter, isStatic);
        this.rightPart = rightPart;
    }

    //if composite variable is "one.two.three" then right part is "two.three"
    AbstractVariable getRightPart() {
        return rightPart;
    }

    //if composite variable is "one.two.three" then left part is "one.two"
    public AbstractVariable getLeftPart() {
        if (rightPart instanceof PlainVariable) {
            return new PlainVariable(origin, name, type, isField, isParameter, isStatic);
        } else {
            CompositeVariable compositeVariable = (CompositeVariable) rightPart;
            return new CompositeVariable(origin, name, type, isField, isParameter, isStatic, compositeVariable.getLeftPart());
        }
    }

    //if composite variable is "one.two.three" then final variable is "three"
    public PlainVariable getFinalVariable() {
        if (rightPart instanceof PlainVariable) {
            return (PlainVariable) rightPart;
        } else {
            return ((CompositeVariable) rightPart).getFinalVariable();
        }
    }

    //if composite variable is "one.two.three" then initial variable is "one"
    public PlainVariable getInitialVariable() {
        return new PlainVariable(origin, name, type, isField, isParameter, isStatic);
    }

    public boolean containsPlainVariable(PlainVariable variable) {
        if (this.origin.equals(variable.origin))
            return true;
        return rightPart.containsPlainVariable(variable);
    }

    public boolean startsWithVariable(AbstractVariable variable) {
        if (variable instanceof PlainVariable) {
            return this.getInitialVariable().equals(variable);
        } else {
            CompositeVariable composite = (CompositeVariable) variable;
            if (this.getInitialVariable().equals(composite.getInitialVariable())) {
                return this.getRightPart().startsWithVariable(composite.getRightPart());
            }
            return false;
        }
    }

    private AbstractVariable getRightPartAfterPrefix(AbstractVariable variable) {
        if (variable instanceof PlainVariable) {
            if (this.getInitialVariable().equals(variable))
                return this.getRightPart();
        } else {
            CompositeVariable composite = (CompositeVariable) variable;
            if (this.getInitialVariable().equals(composite.getInitialVariable())) {
                if (this.getRightPart() instanceof CompositeVariable) {
                    return ((CompositeVariable) this.getRightPart()).getRightPartAfterPrefix(composite.getRightPart());
                }
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CompositeVariable) {
            CompositeVariable composite = (CompositeVariable) o;
            return this.origin.equals(composite.origin)
                    && this.rightPart.equals(composite.rightPart);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 31 * result + origin.hashCode();
            result = 31 * result + rightPart.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return name +
                "." +
                rightPart.toString();
    }
}
