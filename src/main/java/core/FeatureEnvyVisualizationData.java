package core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.psi.*;
import core.ast.*;
import core.ast.decomposition.cfg.CompositeVariable;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.decomposition.cfg.AbstractVariable;

public class FeatureEnvyVisualizationData implements VisualizationData {

    private final Map<MethodInvocationObject, Integer> sourceMethodInvocationMap;
    private final Map<MethodInvocationObject, Integer> targetMethodInvocationMap;
    private final Map<FieldInstructionObject, Integer> sourceFieldReadMap;
    private final Map<FieldInstructionObject, Integer> sourceFieldWriteMap;
    private final Map<FieldInstructionObject, Integer> targetFieldReadMap;
    private final Map<FieldInstructionObject, Integer> targetFieldWriteMap;
    private final ClassObject sourceClass;
    private final MethodObject methodToBeMoved;
    private final ClassObject targetClass;

    public FeatureEnvyVisualizationData(ClassObject sourceClass, MethodObject methodToBeMoved, ClassObject targetClass) {
        this.sourceClass = sourceClass;
        this.methodToBeMoved = methodToBeMoved;
        this.targetClass = targetClass;
        this.sourceMethodInvocationMap = new LinkedHashMap<>();
        List<MethodInvocationObject> sourceMethodInvocations = new ArrayList<>(methodToBeMoved.getNonDistinctInvokedMethodsThroughThisReference());

        this.targetMethodInvocationMap = new LinkedHashMap<>();
        List<FieldInstructionObject> fieldInstructions = new ArrayList<>(methodToBeMoved.getFieldInstructions());
        List<LocalVariableInstructionObject> localVariableInstructions = new ArrayList<>(methodToBeMoved.getLocalVariableInstructions());
        Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughFieldsMap = new LinkedHashMap<>();
        Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughFields = methodToBeMoved.getNonDistinctInvokedMethodsThroughFields();
        for (AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughFields.keySet()) {
            ArrayList<MethodInvocationObject> value = new ArrayList<>(copyFromNonDistinctInvokedMethodsThroughFields.get(key));
            externalMethodInvocationsThroughFieldsMap.put(key, value);
        }
        Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughParametersMap = new LinkedHashMap<>();
        Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughParameters = methodToBeMoved.getNonDistinctInvokedMethodsThroughParameters();
        for (AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughParameters.keySet()) {
            ArrayList<MethodInvocationObject> value = new ArrayList<>(copyFromNonDistinctInvokedMethodsThroughParameters.get(key));
            externalMethodInvocationsThroughParametersMap.put(key, value);
        }

        this.sourceFieldReadMap = new LinkedHashMap<>();
        List<PlainVariable> usedFieldsThroughThisReference = new ArrayList<>(methodToBeMoved.getNonDistinctUsedFieldsThroughThisReference());

        this.sourceFieldWriteMap = new LinkedHashMap<>();
        List<PlainVariable> definedFieldsThroughThisReference = new ArrayList<>(methodToBeMoved.getNonDistinctDefinedFieldsThroughThisReference());

        this.targetFieldReadMap = new LinkedHashMap<>();
        List<AbstractVariable> usedFieldsThroughFields = new ArrayList<>(methodToBeMoved.getNonDistinctUsedFieldsThroughFields());
        List<AbstractVariable> usedFieldsThroughParameters = new ArrayList<>(methodToBeMoved.getNonDistinctUsedFieldsThroughParameters());

        this.targetFieldWriteMap = new LinkedHashMap<>();
        List<AbstractVariable> definedFieldsThroughFields = new ArrayList<>(methodToBeMoved.getNonDistinctDefinedFieldsThroughFields());
        List<AbstractVariable> definedFieldsThroughParameters = new ArrayList<>(methodToBeMoved.getNonDistinctDefinedFieldsThroughParameters());

        for (MethodInvocationObject methodInvocation : sourceMethodInvocations) {
            boolean delegatesToTarget = false;
            MethodObject delegateMethod = sourceClass.getMethod(methodInvocation);
            if (delegateMethod != null) {
                MethodInvocationObject delegateMethodInvocation = delegateMethod.isDelegate();
                if (delegateMethodInvocation != null && delegateMethodInvocation.getOriginClassName().equals(targetClass.getName())) {
                    delegatesToTarget = true;
                    //include delegate method in the analysis
                    fieldInstructions.addAll(new ArrayList<>(delegateMethod.getFieldInstructions()));
                    localVariableInstructions.addAll(new ArrayList<>(delegateMethod.getLocalVariableInstructions()));
                    Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughFieldsMapDelegate = new LinkedHashMap<>();
                    Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughFieldsDelegate = delegateMethod.getNonDistinctInvokedMethodsThroughFields();
                    for (AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughFieldsDelegate.keySet()) {
                        ArrayList<MethodInvocationObject> value = new ArrayList<>(copyFromNonDistinctInvokedMethodsThroughFieldsDelegate.get(key));
                        externalMethodInvocationsThroughFieldsMapDelegate.put(key, value);
                    }
                    for (AbstractVariable variable : externalMethodInvocationsThroughFieldsMapDelegate.keySet()) {
                        if (externalMethodInvocationsThroughFieldsMap.containsKey(variable)) {
                            externalMethodInvocationsThroughFieldsMap.get(variable).addAll(externalMethodInvocationsThroughFieldsMapDelegate.get(variable));
                        } else {
                            externalMethodInvocationsThroughFieldsMap.put(variable, externalMethodInvocationsThroughFieldsMapDelegate.get(variable));
                        }
                    }
                    Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationsThroughParametersMapDelegate = new LinkedHashMap<>();
                    Map<AbstractVariable, ArrayList<MethodInvocationObject>> copyFromNonDistinctInvokedMethodsThroughParametersDelegate = delegateMethod.getNonDistinctInvokedMethodsThroughParameters();
                    for (AbstractVariable key : copyFromNonDistinctInvokedMethodsThroughParametersDelegate.keySet()) {
                        ArrayList<MethodInvocationObject> value = new ArrayList<>(copyFromNonDistinctInvokedMethodsThroughParametersDelegate.get(key));
                        externalMethodInvocationsThroughParametersMapDelegate.put(key, value);
                    }
                    for (AbstractVariable variable : externalMethodInvocationsThroughParametersMapDelegate.keySet()) {
                        if (externalMethodInvocationsThroughParametersMap.containsKey(variable)) {
                            externalMethodInvocationsThroughParametersMap.get(variable).addAll(externalMethodInvocationsThroughParametersMapDelegate.get(variable));
                        } else {
                            externalMethodInvocationsThroughParametersMap.put(variable, externalMethodInvocationsThroughParametersMapDelegate.get(variable));
                        }
                    }
                    usedFieldsThroughThisReference.addAll(new ArrayList<>(delegateMethod.getNonDistinctUsedFieldsThroughThisReference()));
                    definedFieldsThroughThisReference.addAll(new ArrayList<>(delegateMethod.getNonDistinctDefinedFieldsThroughThisReference()));
                    usedFieldsThroughFields.addAll(new ArrayList<>(delegateMethod.getNonDistinctUsedFieldsThroughFields()));
                    usedFieldsThroughParameters.addAll(new ArrayList<>(delegateMethod.getNonDistinctUsedFieldsThroughParameters()));
                    definedFieldsThroughFields.addAll(new ArrayList<>(delegateMethod.getNonDistinctDefinedFieldsThroughFields()));
                    definedFieldsThroughParameters.addAll(new ArrayList<>(delegateMethod.getNonDistinctDefinedFieldsThroughParameters()));
                }
            }
            if (!delegatesToTarget) {
                if (sourceMethodInvocationMap.containsKey(methodInvocation)) {
                    sourceMethodInvocationMap.put(methodInvocation, sourceMethodInvocationMap.get(methodInvocation) + 1);
                } else {
                    sourceMethodInvocationMap.put(methodInvocation, 1);
                }
            }
        }

        processExternalMethodInvocations(externalMethodInvocationsThroughFieldsMap, fieldInstructions, localVariableInstructions, targetClass);
        processExternalMethodInvocations(externalMethodInvocationsThroughParametersMap, fieldInstructions, localVariableInstructions, targetClass);

        for (PlainVariable variable : usedFieldsThroughThisReference) {
            FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
            if (fieldInstruction != null && !fieldInstruction.isStatic()) {
                if (fieldInstruction.getElement().getParent().equals(targetClass.getPsiClass())) {
                    //the used field in inherited from a superclass, which is the target
                    if (targetFieldReadMap.containsKey(fieldInstruction)) {
                        targetFieldReadMap.put(fieldInstruction, targetFieldReadMap.get(fieldInstruction) + 1);
                    } else {
                        targetFieldReadMap.put(fieldInstruction, 1);
                    }
                } else if (sourceClass.getPsiClass().equals(fieldInstruction.getElement().getParent())) {
                    if (sourceFieldReadMap.containsKey(fieldInstruction)) {
                        sourceFieldReadMap.put(fieldInstruction, sourceFieldReadMap.get(fieldInstruction) + 1);
                    } else {
                        sourceFieldReadMap.put(fieldInstruction, 1);
                    }
                }
            }
        }

        for (PlainVariable variable : definedFieldsThroughThisReference) {
            FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
            if (fieldInstruction != null && fieldInstruction.getOwnerClass().equals(targetClass.getName())) {
                //the defined field in inherited from a superclass, which is the target
                if (targetFieldWriteMap.containsKey(fieldInstruction)) {
                    targetFieldWriteMap.put(fieldInstruction, targetFieldWriteMap.get(fieldInstruction) + 1);
                } else {
                    targetFieldWriteMap.put(fieldInstruction, 1);
                }
            } else {
                if (sourceFieldWriteMap.containsKey(fieldInstruction)) {
                    sourceFieldWriteMap.put(fieldInstruction, sourceFieldWriteMap.get(fieldInstruction) + 1);
                } else {
                    sourceFieldWriteMap.put(fieldInstruction, 1);
                }
            }
        }

        handleUsedFields(usedFieldsThroughFields, fieldInstructions, targetClass);
        handleUsedFields(usedFieldsThroughParameters, fieldInstructions, targetClass);

        handleDefinedFields(definedFieldsThroughFields, fieldInstructions, localVariableInstructions, targetClass);
        handleDefinedFields(definedFieldsThroughParameters, fieldInstructions, localVariableInstructions, targetClass);
    }

    private void handleUsedFields(List<AbstractVariable> usedFields, List<FieldInstructionObject> fieldInstructions, ClassObject targetClass) {
        for (AbstractVariable abstractVariable : usedFields) {
            CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
            AbstractVariable leftPart = compositeVariable.getLeftPart();
            PlainVariable variable;
            if (leftPart instanceof CompositeVariable) {
                variable = ((CompositeVariable) leftPart).getFinalVariable();
            } else {
                variable = (PlainVariable) leftPart;
            }

            PsiClass superClassTarget = targetClass.getPsiClass().getSuperClass();
            if (variable.getType().equals(targetClass.getName()) ||
                    superClassTarget != null && variable.getType().equals(superClassTarget.getQualifiedName())) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(compositeVariable.getFinalVariable(), fieldInstructions);
                if (fieldInstruction != null && !fieldInstruction.isStatic()) {
                    if (targetFieldReadMap.containsKey(fieldInstruction)) {
                        targetFieldReadMap.put(fieldInstruction, targetFieldReadMap.get(fieldInstruction) + 1);
                    } else {
                        targetFieldReadMap.put(fieldInstruction, 1);
                    }
                }
            }
        }
    }

    private void handleDefinedFields(List<AbstractVariable> definedFields, List<FieldInstructionObject> fieldInstructions,
                                     List<LocalVariableInstructionObject> localVariableInstructions, ClassObject targetClass) {
        for (AbstractVariable abstractVariable : definedFields) {
            CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
            AbstractVariable leftPart = compositeVariable.getLeftPart();
            PlainVariable variable;
            if (leftPart instanceof CompositeVariable) {
                variable = ((CompositeVariable) leftPart).getFinalVariable();
            } else {
                variable = (PlainVariable) leftPart;
            }
            PsiElement variableTypeBinding = null;
            if (variable.isField()) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
                if (fieldInstruction != null)
                    variableTypeBinding = fieldInstruction.getElement();
            } else if (variable.isParameter()) {
                LocalVariableInstructionObject localVariableInstruction = findLocalVariableInstruction(variable, localVariableInstructions);
                if (localVariableInstruction != null)
                    variableTypeBinding = localVariableInstruction.getReference();
            }
            PsiClass targetClassBinding = (PsiClass) targetClass.getAbstractTypeDeclaration().recoverASTNode();
            if (variable.getType().equals(targetClass.getName()) ||
                    (variableTypeBinding != null && targetClassBinding.equals(variableTypeBinding.getOriginalElement()))) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(compositeVariable.getFinalVariable(), fieldInstructions);
                if (targetFieldWriteMap.containsKey(fieldInstruction)) {
                    targetFieldWriteMap.put(fieldInstruction, targetFieldWriteMap.get(fieldInstruction) + 1);
                } else {
                    targetFieldWriteMap.put(fieldInstruction, 1);
                }
            }
        }
    }

    private FieldInstructionObject findFieldInstruction(PlainVariable variable, List<FieldInstructionObject> fieldInstructions) {
        for (FieldInstructionObject fieldInstruction : fieldInstructions) {
            PsiElement psiElement = fieldInstruction.getElement();
            if (psiElement instanceof PsiField) {
                PsiField psiField = (PsiField) psiElement;
                if (psiField.getName() != null && psiField.getName().equals(variable.getName()))
                    return fieldInstruction;
            }
        }
        return null;
    }

    private LocalVariableInstructionObject findLocalVariableInstruction(PlainVariable variable, List<LocalVariableInstructionObject> localVariableInstructions) {
        for (LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
            PsiElement resolvedElement = localVariableInstruction.getReference().resolve();
            if (variable.getOrigin().equals(resolvedElement)) {
                return localVariableInstruction;
            }
        }
        return null;
    }

    private void processExternalMethodInvocations(Map<AbstractVariable, ArrayList<MethodInvocationObject>> externalMethodInvocationMap,
                                                  List<FieldInstructionObject> fieldInstructions, List<LocalVariableInstructionObject> localVariableInstructions, ClassObject targetClassObject) {
        for (AbstractVariable abstractVariable : externalMethodInvocationMap.keySet()) {
            PlainVariable variable;
            if (abstractVariable instanceof CompositeVariable) {
                variable = ((CompositeVariable) abstractVariable).getFinalVariable();
            } else {
                variable = (PlainVariable) abstractVariable;
            }
            PsiElement variableType = null;
            if (variable.isField()) {
                FieldInstructionObject fieldInstruction = findFieldInstruction(variable, fieldInstructions);
                if (fieldInstruction != null)
                    variableType = fieldInstruction.getElement();
            } else if (variable.isParameter()) {
                LocalVariableInstructionObject localVariableInstruction = findLocalVariableInstruction(variable, localVariableInstructions);
                if (localVariableInstruction != null)
                    variableType = localVariableInstruction.getReference();
            }
            PsiElement targetPsiClass = targetClassObject.getAbstractTypeDeclaration().recoverASTNode();
            if (variable.getType().equals(targetClassObject.getPsiClass().getQualifiedName())) {
                List<MethodInvocationObject> externalMethodInvocations = externalMethodInvocationMap.get(abstractVariable);
                handleExternalMethodInvocation(externalMethodInvocations);
            } else if (variableType instanceof PsiClass) {
                PsiClass psiClass = ((PsiClass) targetPsiClass);
                PsiClass targetClass = (PsiClass) targetPsiClass;
                if (targetClass.getQualifiedName() != null && psiClass.getSuperClass() != null
                        && targetClass.getQualifiedName().equals(psiClass.getSuperClass().getQualifiedName())) {
                    List<MethodInvocationObject> externalMethodInvocations = externalMethodInvocationMap.get(abstractVariable);
                    handleExternalMethodInvocation(externalMethodInvocations);
                }
            }
        }
    }

    private void handleExternalMethodInvocation(List<MethodInvocationObject> externalMethodInvocations) {
        for (MethodInvocationObject methodInvocation : externalMethodInvocations) {
            if (targetMethodInvocationMap.containsKey(methodInvocation)) {
                targetMethodInvocationMap.put(methodInvocation, targetMethodInvocationMap.get(methodInvocation) + 1);
            } else {
                targetMethodInvocationMap.put(methodInvocation, 1);
            }
        }
    }

    private Map<MethodInvocationObject, Integer> getSourceMethodInvocationMap() {
        return sourceMethodInvocationMap;
    }

    private Map<MethodInvocationObject, Integer> getTargetMethodInvocationMap() {
        return targetMethodInvocationMap;
    }

    private Map<FieldInstructionObject, Integer> getSourceFieldReadMap() {
        return sourceFieldReadMap;
    }

    private Map<FieldInstructionObject, Integer> getSourceFieldWriteMap() {
        return sourceFieldWriteMap;
    }

    private Map<FieldInstructionObject, Integer> getTargetFieldReadMap() {
        return targetFieldReadMap;
    }

    private Map<FieldInstructionObject, Integer> getTargetFieldWriteMap() {
        return targetFieldWriteMap;
    }

    public ClassObject getSourceClass() {
        return sourceClass;
    }

    public MethodObject getMethodToBeMoved() {
        return methodToBeMoved;
    }

    public ClassObject getTargetClass() {
        return targetClass;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--SOURCE FIELD READS--").append("\n");
        Map<FieldInstructionObject, Integer> sourceFieldReadMap = getSourceFieldReadMap();
        for (FieldInstructionObject fieldInstruction : sourceFieldReadMap.keySet()) {
            sb.append(fieldInstruction).append("\t").append(sourceFieldReadMap.get(fieldInstruction)).append("\n");
        }
        sb.append("--SOURCE FIELD WRITES--").append("\n");
        Map<FieldInstructionObject, Integer> sourceFieldWriteMap = getSourceFieldWriteMap();
        for (FieldInstructionObject fieldInstruction : sourceFieldWriteMap.keySet()) {
            sb.append(fieldInstruction).append("\t").append(sourceFieldWriteMap.get(fieldInstruction)).append("\n");
        }
        sb.append("--SOURCE METHOD CALLS--").append("\n");
        Map<MethodInvocationObject, Integer> sourceMethodInvocationMap = getSourceMethodInvocationMap();
        for (MethodInvocationObject methodInvocation : sourceMethodInvocationMap.keySet()) {
            sb.append(methodInvocation).append("\t").append(sourceMethodInvocationMap.get(methodInvocation)).append("\n");
        }
        sb.append("\n");
        sb.append("--TARGET FIELD READS--").append("\n");
        Map<FieldInstructionObject, Integer> targetFieldReadMap = getTargetFieldReadMap();
        for (FieldInstructionObject fieldInstruction : targetFieldReadMap.keySet()) {
            sb.append(fieldInstruction).append("\t").append(targetFieldReadMap.get(fieldInstruction)).append("\n");
        }
        sb.append("--TARGET FIELD WRITES--").append("\n");
        Map<FieldInstructionObject, Integer> targetFieldWriteMap = getTargetFieldWriteMap();
        for (FieldInstructionObject fieldInstruction : targetFieldWriteMap.keySet()) {
            sb.append(fieldInstruction).append("\t").append(targetFieldWriteMap.get(fieldInstruction)).append("\n");
        }
        sb.append("--TARGET METHOD CALLS--").append("\n");
        Map<MethodInvocationObject, Integer> targetMethodInvocationMap = getTargetMethodInvocationMap();
        for (MethodInvocationObject methodInvocation : targetMethodInvocationMap.keySet()) {
            sb.append(methodInvocation).append("\t").append(targetMethodInvocationMap.get(methodInvocation)).append("\n");
        }
        return sb.toString();
    }

    public int getDistinctSourceDependencies() {
        Set<FieldInstructionObject> fields = new LinkedHashSet<>();
        fields.addAll(sourceFieldReadMap.keySet());
        fields.addAll(sourceFieldWriteMap.keySet());
        return fields.size() + sourceMethodInvocationMap.size();
    }

    public int getDistinctTargetDependencies() {
        Set<FieldInstructionObject> fields = new LinkedHashSet<>();
        fields.addAll(targetFieldReadMap.keySet());
        fields.addAll(targetFieldWriteMap.keySet());
        return fields.size() + targetMethodInvocationMap.size();
    }
}
