package org.jetbrains.research.intellijdeodorant.inheritance;

import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;

import java.util.*;

public class CompleteInheritanceDetection {
    private final Map<String, LinkedHashSet<String>> subclassMap;

    public CompleteInheritanceDetection(SystemObject system) {
        this.subclassMap = new LinkedHashMap<>();
        generateInheritanceHierarchies(system);
    }

    private void addSubclassToSuperclass(String superclass, String subclass) {
        if (subclassMap.containsKey(superclass)) {
            LinkedHashSet<String> subclasses = subclassMap.get(superclass);
            subclasses.add(subclass);
        } else {
            LinkedHashSet<String> subclasses = new LinkedHashSet<>();
            subclasses.add(subclass);
            subclassMap.put(superclass, subclasses);
        }
    }

    private void generateInheritanceHierarchies(SystemObject system) {
        ListIterator<ClassObject> classIterator = system.getClassListIterator();
        while (classIterator.hasNext()) {
            ClassObject classObject = classIterator.next();
            String superclassType = classObject.getSuperclass();
            if (superclassType != null) {
                if (system.getClassObject(superclassType) != null) {
                    addSubclassToSuperclass(superclassType, classObject.getName());
                }
            }
            ListIterator<String> interfaceIterator = classObject.getInterfaceIterator();
            while (interfaceIterator.hasNext()) {
                String superInterface = interfaceIterator.next();
                if (system.getClassObject(superInterface) != null) {
                    addSubclassToSuperclass(superInterface, classObject.getName());
                }
            }
        }
    }

    public InheritanceTree getTree(String className) {
        if (subclassMap.containsKey(className)) {
            InheritanceTree tree = new InheritanceTree();
            recursivelyConstructTree(tree, className);
            return tree;
        } else {
            return null;
        }
    }

    private void recursivelyConstructTree(InheritanceTree tree, String className) {
        if (subclassMap.containsKey(className)) {
            LinkedHashSet<String> subclasses = subclassMap.get(className);
            for (String subclass : subclasses) {
                tree.addChildToParent(subclass, className);
                recursivelyConstructTree(tree, subclass);
            }
        }
    }

    public Set<String> getRoots() {
        return subclassMap.keySet();
    }

    public Set<InheritanceTree> getMatchingTrees(String subclassName) {
        Set<InheritanceTree> inheritanceTrees = new LinkedHashSet<>();
        for (String superclass : subclassMap.keySet()) {
            LinkedHashSet<String> subclasses = subclassMap.get(superclass);
            boolean matchingInheritanceHierarchy = false;
            for (String subclass : subclasses) {
                if ((subclass.contains(".") && subclass.endsWith("." + subclassName)) || subclass.equals(subclassName)) {
                    matchingInheritanceHierarchy = true;
                    break;
                }
            }
            if (matchingInheritanceHierarchy)
                inheritanceTrees.add(getTree(superclass));
        }
        return inheritanceTrees;
    }
}
