package core.ast.decomposition.cfg;

import core.ast.*;
import core.ast.decomposition.AbstractExpression;
import core.ast.decomposition.CompositeStatementObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDGBlockNode extends PDGNode {
    CFGNode controlParent;

    PDGBlockNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                 Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
    }

    public PDGNode getControlDependenceParent() {
        if (controlParent != null) {
            if (controlParent.getPDGNode() != null)
                return controlParent.getPDGNode();
        }
        return super.getControlDependenceParent();
    }

    protected void determineDefinedAndUsedVariables() {
        CFGNode cfgNode = getCFGNode();
        if (cfgNode.getStatement() instanceof CompositeStatementObject) {
            CompositeStatementObject compositeStatement = (CompositeStatementObject) cfgNode.getStatement();
            List<AbstractExpression> expressions = compositeStatement.getExpressions();
            for (AbstractExpression expression : expressions) {
                List<CreationObject> creations = expression.getCreations();
                for (CreationObject creation : creations) {
                    createdTypes.add(creation);
                    if (creation instanceof ClassInstanceCreationObject) {
                        ClassInstanceCreationObject classInstanceCreation = (ClassInstanceCreationObject) creation;
                        Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> variablesAssignedWithClassInstanceCreations =
                                expression.getVariablesAssignedWithClassInstanceCreations();
                        PlainVariable variable = null;
                        for (PlainVariable key : variablesAssignedWithClassInstanceCreations.keySet()) {
                            if (variablesAssignedWithClassInstanceCreations.get(key).contains(classInstanceCreation)
                                    && (expression.getDefinedFieldsThroughThisReference().contains(key)
                                    || expression.getDefinedLocalVariables().contains(key)
                                    || expression.getDeclaredLocalVariables().contains(key))) {
                                variable = key;
                                break;
                            }
                        }
                        if (variable != null) {
                            processArgumentsOfInternalClassInstanceCreation(classInstanceCreation, variable);
                        }
                        thrownExceptionTypes.addAll(classInstanceCreation.getThrownExceptions());
                    }
                }
                for (PlainVariable variable : expression.getDeclaredLocalVariables()) {
                    declaredVariables.add(variable);
                    definedVariables.add(variable);
                }
                definedVariables.addAll(expression.getDefinedLocalVariables());
                usedVariables.addAll(expression.getUsedLocalVariables());
                Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables =
                        expression.getInvokedMethodsThroughLocalVariables();
                for (AbstractVariable variable : invokedMethodsThroughLocalVariables.keySet()) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(variable);
                    for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                        thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                        processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
                    }
                }
                Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters =
                        expression.getInvokedMethodsThroughParameters();
                for (AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
                    for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                        thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                        processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
                    }
                }

                definedVariables.addAll(expression.getDefinedFieldsThroughThisReference());
                usedVariables.addAll(expression.getUsedFieldsThroughThisReference());
                definedVariables.addAll(expression.getDefinedFieldsThroughFields());
                usedVariables.addAll(expression.getUsedFieldsThroughFields());
                definedVariables.addAll(expression.getDefinedFieldsThroughParameters());
                usedVariables.addAll(expression.getUsedFieldsThroughParameters());
                definedVariables.addAll(expression.getDefinedFieldsThroughLocalVariables());
                usedVariables.addAll(expression.getUsedFieldsThroughLocalVariables());

                Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields =
                        expression.getInvokedMethodsThroughFields();
                for (AbstractVariable variable : invokedMethodsThroughFields.keySet()) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(variable);
                    for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                        thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                        processArgumentsOfInternalMethodInvocation(methodInvocationObject, variable);
                    }
                }
                for (MethodInvocationObject methodInvocationObject : expression.getInvokedMethodsThroughThisReference()) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                    processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
                }
                for (MethodInvocationObject methodInvocationObject : expression.getInvokedStaticMethods()) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                    processArgumentsOfInternalMethodInvocation(methodInvocationObject, null);
                }
                List<SuperMethodInvocationObject> superMethodInvocations = expression.getSuperMethodInvocations();
                for (SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
                    thrownExceptionTypes.addAll(superMethodInvocationObject.getThrownExceptions());
                }
                List<MethodInvocationObject> methodInvocations = expression.getMethodInvocations();
                for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                }
            }
        }
    }
}