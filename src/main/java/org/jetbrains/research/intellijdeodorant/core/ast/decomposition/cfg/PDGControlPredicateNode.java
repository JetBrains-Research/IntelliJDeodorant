package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractExpression;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.CompositeStatementObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PDGControlPredicateNode extends PDGNode {

    PDGControlPredicateNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                            Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        determineDefinedAndUsedVariables();
    }

    private void determineDefinedAndUsedVariables() {
        CFGNode cfgNode = getCFGNode();
        if (cfgNode.getStatement() instanceof CompositeStatementObject) {
            CompositeStatementObject composite = (CompositeStatementObject) cfgNode.getStatement();
            List<AbstractExpression> expressions = composite.getExpressions();
            for (AbstractExpression expression : expressions) {
                List<CreationObject> creations = expression.getCreations();
                for (CreationObject creation : creations) {
                    createdTypes.add(creation);
                    if (creation instanceof ClassInstanceCreationObject) {
                        ClassInstanceCreationObject classInstanceCreation = (ClassInstanceCreationObject) creation;
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
                    }
                }
                Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters =
                        expression.getInvokedMethodsThroughParameters();
                for (AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
                    for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                        thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
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
                    }
                }
                for (MethodInvocationObject methodInvocationObject : expression.getInvokedMethodsThroughThisReference()) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                }
                for (MethodInvocationObject methodInvocationObject : expression.getInvokedStaticMethods()) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
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
