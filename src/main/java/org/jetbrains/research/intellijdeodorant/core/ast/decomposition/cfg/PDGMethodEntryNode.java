package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiParameter;
import org.jetbrains.research.intellijdeodorant.core.ast.AbstractMethodDeclaration;

import java.util.ListIterator;

public class PDGMethodEntryNode extends PDGNode {
    private final AbstractMethodDeclaration method;

    PDGMethodEntryNode(AbstractMethodDeclaration method) {
        super();
        this.method = method;
        this.id = 0;
        ListIterator<PsiParameter> parameterIterator = method.getParameterListIterator();
        while (parameterIterator.hasNext()) {
            PsiParameter parameter = parameterIterator.next();
            PlainVariable parameterVariable = new PlainVariable(parameter);
            declaredVariables.add(parameterVariable);
            definedVariables.add(parameterVariable);
        }
    }

    public AbstractMethodDeclaration getMethod() {
        return method;
    }

    public BasicBlock getBasicBlock() {
        return null;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof PDGMethodEntryNode) {
            PDGMethodEntryNode pdgNode = (PDGMethodEntryNode) o;
            return this.method.equals(pdgNode.method);
        }
        return false;
    }

    public int hashCode() {
        return method.hashCode();
    }

    public String toString() {
        return id + "\t" + method.getName();
    }
}
