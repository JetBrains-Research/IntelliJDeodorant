package org.jetbrains.research.intellijdeodorant.util.math;

import com.intellij.psi.PsiElement;

public class Node implements Comparable<Node> {

	protected final PsiElement name;
	protected boolean visited = false;   // used for Kosaraju's algorithm and Edmonds's algorithm
	protected int lowlink = -1;          // used for Tarjan's algorithm
	protected int index = -1;            // used for Tarjan's algorithm
	private volatile int hashCode = 0;

	public Node(final PsiElement argName) {
		name = argName;
	}

	public PsiElement getName() {
		return name;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(o instanceof Node) {
			Node node = (Node)o;
			return this.name.equals(node.name);
		}
		return false;
	}

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37 * result + name.hashCode();
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
