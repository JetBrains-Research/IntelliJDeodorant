package org.jetbrains.research.intellijdeodorant.core.ast.association;

import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.FieldInstructionObject;
import org.jetbrains.research.intellijdeodorant.core.ast.FieldObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

    public boolean containsFieldObject(FieldObject field) {
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

            ListIterator<FieldObject> fieldIt = classObject.getFieldIterator();
            while (fieldIt.hasNext()) {
                FieldObject fieldObject = fieldIt.next();
                String type = fieldObject.getType().getClassType();
                //cover also other collections in the future
                if (acceptableOriginClassNames.contains(type)) {
                    String genericType = fieldObject.getType().getGenericType();
                    if (genericType != null) {
                        for (String className : systemObject.getClassNames()) {
                            if (genericType.contains(className)) {
                                Association association = new Association(fieldObject, classObject.getName(), className);
                                association.setContainer(true);
                                if (!associationList.contains(association))
                                    associationList.add(association);
                            }
                        }
                    } else {
                        Association association = checkCollectionAttribute(systemObject, classObject, fieldObject);
                        if (association != null && !associationList.contains(association))
                            associationList.add(association);
                    }
                } else if (systemObject.getClassObject(type) != null) {
                    Association association = new Association(fieldObject, classObject.getName(), type);
                    if (fieldObject.getType().getArrayDimension() > 0)
                        association.setContainer(true);
                    if (!associationList.contains(association))
                        associationList.add(association);
                }
            }
        }
    }

    private Association checkCollectionAttribute(SystemObject systemObject, ClassObject classObject, FieldObject field) {
        ListIterator<MethodObject> methodIt = classObject.getMethodIterator();
        while (methodIt.hasNext()) {
            MethodObject method = methodIt.next();
            FieldInstructionObject fieldInstruction = method.isCollectionAdder();
            if (fieldInstruction != null && field.equals(fieldInstruction)) {
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

    public Association getAssociation(String from, String to) {
        for (Association association : associationList) {
            if (association.getFrom().equals(from) && association.getTo().equals(to))
                return association;
        }
        return null;
    }
}
