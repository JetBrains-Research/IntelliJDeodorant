package org.jetbrains.research.intellijdeodorant.core.ast.association;

import com.intellij.psi.PsiField;
import org.jetbrains.research.intellijdeodorant.core.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;

public class AssociationDetection {
    private final List<Association> associationList;
    private final List<String> acceptableOriginClassNames;

    public AssociationDetection(SystemObject system) {
        this.associationList = new ArrayList<>();
        acceptableOriginClassNames = new ArrayList<>();
        acceptableOriginClassNames.add("java.util.Collection");
        acceptableOriginClassNames.add("java.util.AbstractCollection");
        acceptableOriginClassNames.add("java.util.List");
        acceptableOriginClassNames.add("java.util.AbstractList");
        acceptableOriginClassNames.add("java.util.ArrayList");
        acceptableOriginClassNames.add("java.util.LinkedList");
        acceptableOriginClassNames.add("java.util.Set");
        acceptableOriginClassNames.add("java.util.AbstractSet");
        acceptableOriginClassNames.add("java.util.HashSet");
        acceptableOriginClassNames.add("java.util.LinkedHashSet");
        acceptableOriginClassNames.add("java.util.SortedSet");
        acceptableOriginClassNames.add("java.util.TreeSet");
        acceptableOriginClassNames.add("java.util.Vector");
        acceptableOriginClassNames.add("java.util.Stack");
        generateAssociations(system);
    }

    public boolean containsFieldObject(PsiField field) {
        for (Association association : associationList)
            if (association.getFieldObject().equals(field))
                return true;
        return false;
    }

    public List<Association> getAssociationsOfClass(ClassObject classObject) {
        List<Association> associations = new ArrayList<>();
        for (Association association : associationList) {
            if (association.getFrom().equals(classObject.getName()))
                associations.add(association);
        }
        return associations;
    }

    private void generateAssociations(SystemObject systemObject) {
        ListIterator<ClassObject> classIt = systemObject.getClassListIterator();
        while (classIt.hasNext()) {
            ClassObject classObject = classIt.next();

            ListIterator<PsiField> fieldIt = classObject.getFieldIterator();
            while (fieldIt.hasNext()) {
                PsiField fieldObject = fieldIt.next();
                String type = fieldObject.getType().getCanonicalText();
                //cover also other collections in the future
                if (acceptableOriginClassNames.contains(type)) {
                    String genericType = fieldObject.getType().getCanonicalText();
                    for (String className : systemObject.getClassNames()) {
                        if (genericType.contains(className)) {
                            Association association = new Association(fieldObject, classObject.getName(), className);
                            association.setContainer(true);
                            if (!associationList.contains(association))
                                associationList.add(association);
                        }
                    }
                } else if (systemObject.getClassObject(type) != null) {
                    Association association = new Association(fieldObject, classObject.getName(), type);
                    if (fieldObject.getType().getArrayDimensions() > 0)
                        association.setContainer(true);
                    if (!associationList.contains(association))
                        associationList.add(association);
                }
            }
        }
    }

    private Association checkCollectionAttribute(SystemObject systemObject, ClassObject classObject, PsiField field) {
        ListIterator<MethodObject> methodIt = classObject.getMethodIterator();
        while (methodIt.hasNext()) {
            MethodObject method = methodIt.next();
            PsiField fieldInstruction = method.isCollectionAdder();
            if (field.equals(fieldInstruction)) {
                List<String> parameterList = method.getParameterList();
                if (parameterList.size() == 1 && systemObject.getClassObject(parameterList.get(0)) != null) {
                    Association association = new Association(field, classObject.getName(), parameterList.get(0));
                    association.setContainer(true);
                    return association;
                }
            }
        }
        return null;
    }

    public boolean checkForContainerAssociation(String from, String to) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(from);
        recurse(root);
        DefaultMutableTreeNode firstLeaf = root.getFirstLeaf();
        if (checkFullPathForContainerAssociation(firstLeaf, to))
            return true;
        DefaultMutableTreeNode leaf = firstLeaf.getNextLeaf();
        while (leaf != null) {
            if (checkFullPathForContainerAssociation(leaf, to))
                return true;
            leaf = leaf.getNextLeaf();
        }
        return false;
    }

    private boolean checkFullPathForContainerAssociation(DefaultMutableTreeNode leaf, String nodeName) {
        if (leaf.getUserObject().equals(nodeName))
            return checkPathForContainerAssociation(leaf);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) leaf.getParent();
        while (node != null) {
            if (node.getUserObject().equals(nodeName))
                return checkPathForContainerAssociation(node);
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return false;
    }

    // node is the target class of move operation
    private boolean checkPathForContainerAssociation(DefaultMutableTreeNode node) {
        while (node.getParent() != null) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            Association association = getAssociation((String) node.getUserObject(), (String) parent.getUserObject());
            if (association.isContainer())
                return true;
            node = parent;
        }
        return false;
    }

    private void recurse(DefaultMutableTreeNode root) {
        List<Association> endingAssociations = getAssociationsEndingTo((String) root.getUserObject());
        for (Association association : endingAssociations) {
            Object[] pathFromRoot = root.getUserObjectPath();
            boolean cycle = false;
            for (Object node : pathFromRoot) {
                if (association.getFrom().equals(node)) {
                    cycle = true;
                    break;
                }
            }
            if (!cycle) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(association.getFrom());
                root.add(node);
                recurse(node);
            }
        }
    }

    private List<Association> getAssociationsEndingTo(String to) {
        List<Association> list = new ArrayList<>();
        for (Association association : associationList) {
            if (association.getTo().equals(to))
                list.add(association);
        }
        return list;
    }

    public Association getAssociation(String from, String to) {
        for (Association association : associationList) {
            if (association.getFrom().equals(from) && association.getTo().equals(to))
                return association;
        }
        return null;
    }
}
