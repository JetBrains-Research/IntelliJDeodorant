package org.jetbrains.research.intellijdeodorant.inheritance;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

public class InheritanceTree {
    private DefaultMutableTreeNode rootNode;

    public InheritanceTree() {
        this.rootNode = null;
    }

    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    public boolean contains(String nodeName) {
        Enumeration e = rootNode.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject().equals(nodeName)) {
                return true;
            }
        }
        return false;
    }

    public DefaultMutableTreeNode getNode(String nodeName) {
        if (rootNode != null) {
            Enumeration e = rootNode.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (node.getUserObject().equals(nodeName)) {
                    return node;
                }
            }
        }
        return null;
    }

    public void addChildToParent(String childNode, String parentNode) {
        DefaultMutableTreeNode cNode = getNode(childNode);
        if (cNode == null) {
            cNode = new DefaultMutableTreeNode(childNode);
        }
        DefaultMutableTreeNode pNode = getNode(parentNode);
        if (pNode == null) {
            pNode = new DefaultMutableTreeNode(parentNode);
            rootNode = pNode;
        }
        pNode.add(cNode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof InheritanceTree) {
            InheritanceTree inheritanceTree = (InheritanceTree) o;
            return rootNode.getUserObject().equals(inheritanceTree.rootNode.getUserObject());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 37 * hash + rootNode.getUserObject().hashCode();
        return hash;
    }
}
