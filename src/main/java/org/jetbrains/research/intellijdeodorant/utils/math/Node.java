package org.jetbrains.research.intellijdeodorant.utils.math;

import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class Node implements Comparable<Node> {

    protected final SmartPsiElementPointer<PsiElement> name;
    protected int lowlink = -1;          // used for Tarjan's algorithm
    protected int index = -1;            // used for Tarjan's algorithm
    private volatile int hashCode = 0;

    public Node(final PsiElement argName) {
        name = toPointer(argName);
    }

    public PsiElement getName() {
        return name.getElement();
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Node) {
            Node node = (Node) o;
            return getName().equals(node.getName());
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + getName().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return name.toString();
    }

    public int compareTo(final Node argNode) {
        return argNode.equals(this) ? 0 : -1;
    }
}
