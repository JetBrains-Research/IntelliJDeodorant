package org.jetbrains.research.intellijdeodorant.core;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.research.intellijdeodorant.core.ast.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;

import java.util.*;

public class GodClassVisualizationData implements VisualizationData {
    //the MethodObject keys correspond to the methods suggested to be extracted in a new class
    private final Map<MethodObject, Map<MethodInvocationObject, Integer>> internalMethodInvocationMap;
    private final Map<MethodObject, Map<MethodInvocationObject, Integer>> externalMethodInvocationMap;
    private final Map<MethodObject, Map<FieldInstructionObject, Integer>> internalFieldReadMap;
    private final Map<MethodObject, Map<FieldInstructionObject, Integer>> internalFieldWriteMap;
    private final Map<MethodObject, Map<FieldInstructionObject, Integer>> externalFieldReadMap;
    private final Map<MethodObject, Map<FieldInstructionObject, Integer>> externalFieldWriteMap;
    private final Set<MethodObject> extractedMethods;
    private final Set<FieldObject> extractedFields;
    private final ClassObject sourceClass;

    public GodClassVisualizationData(ClassObject sourceClass, Set<MethodObject> extractedMethods, Set<FieldObject> extractedFields) {
        this.sourceClass = sourceClass;
        this.extractedMethods = extractedMethods;
        this.extractedFields = extractedFields;
        this.internalMethodInvocationMap = new LinkedHashMap<>();
        this.externalMethodInvocationMap = new LinkedHashMap<>();
        this.internalFieldReadMap = new LinkedHashMap<>();
        this.internalFieldWriteMap = new LinkedHashMap<>();
        this.externalFieldReadMap = new LinkedHashMap<>();
        this.externalFieldWriteMap = new LinkedHashMap<>();

        for (MethodObject method : extractedMethods) {
            List<MethodInvocationObject> methodInvocations = method.getNonDistinctInvokedMethodsThroughThisReference();
            for (MethodInvocationObject invocation : methodInvocations) {
                if (isInvocationToExtractedMethod(invocation, extractedMethods)) {
                    insertToMap(method, invocation, internalMethodInvocationMap);
                } else {
                    insertToMap(method, invocation, externalMethodInvocationMap);
                }
            }
            List<FieldInstructionObject> fieldInstructions = method.getFieldInstructions();
            List<PlainVariable> fieldAccesses = method.getNonDistinctUsedFieldsThroughThisReference();
            for (PlainVariable fieldAccess : fieldAccesses) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(fieldAccess, fieldInstructions);
                //exclude accesses to static fields
                if (!fieldInstruction.isStatic()) {
                    if (isAccessToExtractedField(fieldInstruction, extractedFields)) {
                        insertToMap(method, fieldInstruction, internalFieldReadMap, 1);
                    } else {
                        insertToMap(method, fieldInstruction, externalFieldReadMap, 1);
                    }
                }
            }
            List<PlainVariable> fieldWrites = method.getNonDistinctDefinedFieldsThroughThisReference();
            for (PlainVariable fieldWrite : fieldWrites) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(fieldWrite, fieldInstructions);
                //exclude accesses to static fields
                if (!fieldInstruction.isStatic()) {
                    if (isAccessToExtractedField(fieldInstruction, extractedFields)) {
                        insertToMap(method, fieldInstruction, internalFieldWriteMap, 1);
                    } else {
                        insertToMap(method, fieldInstruction, externalFieldWriteMap, 1);
                    }
                }
            }
        }
        for (MethodObject method : extractedMethods) {
            //replace getter method calls in Source class with field accesses in Extracted class
            Map<MethodInvocationObject, Integer> methodInvocationMap = externalMethodInvocationMap.get(method);
            if (methodInvocationMap != null) {
                Set<MethodInvocationObject> invocationsToBeRemoved = new LinkedHashSet<>();
                for (MethodInvocationObject invocation : methodInvocationMap.keySet()) {
                    int count = methodInvocationMap.get(invocation);
                    MethodObject methodDeclaration = sourceClass.getMethod(invocation);
                    if (methodDeclaration != null) {
                        FieldInstructionObject getterFieldInstruction = methodDeclaration.isGetter();
                        if (getterFieldInstruction != null) {
                            if (isAccessToExtractedField(getterFieldInstruction, extractedFields)) {
                                //remove getter method calls in source class
                                invocationsToBeRemoved.add(invocation);
                                //add field reads in extracted class
                                insertToMap(method, getterFieldInstruction, internalFieldReadMap, count);
                            }
                        }
                        FieldInstructionObject setterFieldInstruction = methodDeclaration.isSetter();
                        if (setterFieldInstruction != null) {
                            if (isAccessToExtractedField(setterFieldInstruction, extractedFields)) {
                                //remove setter method calls in source class
                                invocationsToBeRemoved.add(invocation);
                                //add field writes in extracted class
                                insertToMap(method, setterFieldInstruction, internalFieldWriteMap, count);
                            }
                        }
                    }
                }
                for (MethodInvocationObject invocation : invocationsToBeRemoved) {
                    methodInvocationMap.remove(invocation);
                }
                if (methodInvocationMap.isEmpty()) {
                    externalMethodInvocationMap.remove(method);
                }
            }
        }
    }

    private void insertToMap(MethodObject method, MethodInvocationObject methodInvocation,
                             Map<MethodObject, Map<MethodInvocationObject, Integer>> map) {
        if (map.containsKey(method)) {
            Map<MethodInvocationObject, Integer> invocationMap = map.get(method);
            if (invocationMap.containsKey(methodInvocation)) {
                invocationMap.put(methodInvocation, invocationMap.get(methodInvocation) + 1);
            } else {
                invocationMap.put(methodInvocation, 1);
            }
        } else {
            Map<MethodInvocationObject, Integer> invocationMap = new LinkedHashMap<>();
            invocationMap.put(methodInvocation, 1);
            map.put(method, invocationMap);
        }
    }

    private void insertToMap(MethodObject method, FieldInstructionObject fieldInstruction,
                             Map<MethodObject, Map<FieldInstructionObject, Integer>> map, int count) {
        if (map.containsKey(method)) {
            Map<FieldInstructionObject, Integer> fieldAccessMap = map.get(method);
            if (fieldAccessMap.containsKey(fieldInstruction)) {
                fieldAccessMap.put(fieldInstruction, fieldAccessMap.get(fieldInstruction) + count);
            } else {
                fieldAccessMap.put(fieldInstruction, count);
            }
        } else {
            Map<FieldInstructionObject, Integer> fieldAccessMap = new LinkedHashMap<>();
            fieldAccessMap.put(fieldInstruction, count);
            map.put(method, fieldAccessMap);
        }
    }

    private boolean isInvocationToExtractedMethod(MethodInvocationObject invocation, Set<MethodObject> extractedMethods) {
        for (MethodObject method : extractedMethods) {
            if (method.equals(invocation))
                return true;
        }
        return false;
    }

    private boolean isAccessToExtractedField(FieldInstructionObject instruction, Set<FieldObject> extractedFields) {
        for (FieldObject field : extractedFields) {
            if (field.equals(instruction))
                return true;
        }
        return false;
    }

    private FieldInstructionObject findFieldInstruction(PlainVariable variable, List<FieldInstructionObject> fieldInstructions) {
        for (FieldInstructionObject fieldInstruction : fieldInstructions) {
            PsiElement psiElement = fieldInstruction.getElement();
            if (psiElement instanceof PsiField) {
                PsiField psiField = (PsiField) psiElement;
                if (psiField.getName().equals(variable.getName()))
                    return fieldInstruction;
            }
        }
        return null;
    }

    public Map<MethodObject, Map<MethodInvocationObject, Integer>> getInternalMethodInvocationMap() {
        return internalMethodInvocationMap;
    }

    public Map<MethodObject, Map<MethodInvocationObject, Integer>> getExternalMethodInvocationMap() {
        return externalMethodInvocationMap;
    }

    public Map<MethodObject, Map<FieldInstructionObject, Integer>> getInternalFieldReadMap() {
        return internalFieldReadMap;
    }

    public Map<MethodObject, Map<FieldInstructionObject, Integer>> getInternalFieldWriteMap() {
        return internalFieldWriteMap;
    }

    public Map<MethodObject, Map<FieldInstructionObject, Integer>> getExternalFieldReadMap() {
        return externalFieldReadMap;
    }

    public Map<MethodObject, Map<FieldInstructionObject, Integer>> getExternalFieldWriteMap() {
        return externalFieldWriteMap;
    }

    public String toString() {

        return "--FIELD READS FROM EXTRACTED METHODS TO EXTRACTED FIELDS--" + "\n" +
                fieldAccessMapToString(getInternalFieldReadMap()) +
                "--FIELD WRITES FROM EXTRACTED METHODS TO EXTRACTED FIELDS--" + "\n" +
                fieldAccessMapToString(getInternalFieldWriteMap()) +
                "--METHOD CALLS BETWEEN EXTRACTED METHODS--" + "\n" +
                methodCallMapToString(getInternalMethodInvocationMap()) +
                "\n" +
                "--FIELD READS FROM EXTRACTED METHODS TO SOURCE CLASS FIELDS--" + "\n" +
                fieldAccessMapToString(getExternalFieldReadMap()) +
                "--FIELD WRITES FROM EXTRACTED METHODS TO SOURCE CLASS FIELDS--" + "\n" +
                fieldAccessMapToString(getExternalFieldWriteMap()) +
                "--METHOD CALLS FROM EXTRACTED METHODS TO SOURCE CLASS METHODS--" + "\n" +
                methodCallMapToString(getExternalMethodInvocationMap());
    }

    private String methodCallMapToString(Map<MethodObject, Map<MethodInvocationObject, Integer>> map) {
        StringBuilder sb = new StringBuilder();
        for (MethodObject method : map.keySet()) {
            sb.append(method).append("\n");
            Map<MethodInvocationObject, Integer> invocationMap = map.get(method);
            for (MethodInvocationObject invocation : invocationMap.keySet()) {
                sb.append("\t").append(invocation).append(" : ").append(invocationMap.get(invocation)).append("\n");
            }
        }
        return sb.toString();
    }

    private String fieldAccessMapToString(Map<MethodObject, Map<FieldInstructionObject, Integer>> map) {
        StringBuilder sb = new StringBuilder();
        for (MethodObject method : map.keySet()) {
            sb.append(method).append("\n");
            Map<FieldInstructionObject, Integer> fieldAccessMap = map.get(method);
            for (FieldInstructionObject instruction : fieldAccessMap.keySet()) {
                sb.append("\t").append(instruction).append(" : ").append(fieldAccessMap.get(instruction)).append("\n");
            }
        }
        return sb.toString();
    }

    public int getDistinctTargetDependencies() {
        Set<FieldInstructionObject> fields = new LinkedHashSet<>();
        for (MethodObject key : internalFieldReadMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = internalFieldReadMap.get(key);
            fields.addAll(value.keySet());
        }
        for (MethodObject key : internalFieldWriteMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = internalFieldWriteMap.get(key);
            fields.addAll(value.keySet());
        }
        Set<MethodInvocationObject> methods = new LinkedHashSet<>();
        for (MethodObject key : internalMethodInvocationMap.keySet()) {
            Map<MethodInvocationObject, Integer> value = internalMethodInvocationMap.get(key);
            methods.addAll(value.keySet());
        }
        return fields.size() + methods.size();
    }

    public int getDistinctSourceDependencies() {
        Set<FieldInstructionObject> fields = new LinkedHashSet<>();
        for (MethodObject key : externalFieldReadMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = externalFieldReadMap.get(key);
            fields.addAll(value.keySet());
        }
        for (MethodObject key : externalFieldWriteMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = externalFieldWriteMap.get(key);
            fields.addAll(value.keySet());
        }
        Set<MethodInvocationObject> methods = new LinkedHashSet<>();
        for (MethodObject key : externalMethodInvocationMap.keySet()) {
            Map<MethodInvocationObject, Integer> value = externalMethodInvocationMap.get(key);
            methods.addAll(value.keySet());
        }
        return fields.size() + methods.size();
    }

    public boolean containsNonAccessedFieldInExtractedClass() {
        Set<FieldInstructionObject> fields = new LinkedHashSet<>();
        for (MethodObject key : internalFieldReadMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = internalFieldReadMap.get(key);
            fields.addAll(value.keySet());
        }
        for (MethodObject key : internalFieldWriteMap.keySet()) {
            Map<FieldInstructionObject, Integer> value = internalFieldWriteMap.get(key);
            fields.addAll(value.keySet());
        }
        for (FieldObject field : extractedFields) {
            if (!fields.contains(field.generateFieldInstruction())) {
                return true;
            }
        }
        return false;
    }
}
