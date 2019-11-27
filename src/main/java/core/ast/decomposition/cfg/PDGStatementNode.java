package core.ast.decomposition.cfg;

import core.ast.*;
import core.ast.decomposition.StatementObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PDGStatementNode extends PDGNode {

    PDGStatementNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                     Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        determineDefinedAndUsedVariables();
    }

    private void determineDefinedAndUsedVariables() {
        CFGNode cfgNode = getCFGNode();
        if (cfgNode.getStatement() instanceof StatementObject) {
            StatementObject statement = (StatementObject) cfgNode.getStatement();
            thrownExceptionTypes.addAll(statement.getExceptionsInThrowStatements());
            List<CreationObject> creations = statement.getCreations();
            for (CreationObject creation : creations) {
                createdTypes.add(creation);
                if (creation instanceof ClassInstanceCreationObject) {
                    ClassInstanceCreationObject classInstanceCreation = (ClassInstanceCreationObject) creation;
                    thrownExceptionTypes.addAll(classInstanceCreation.getThrownExceptions());
                }
            }
            for (PlainVariable variable : statement.getDeclaredLocalVariables()) {
                declaredVariables.add(variable);
                definedVariables.add(variable);
            }
            definedVariables.addAll(statement.getDefinedLocalVariables());
            usedVariables.addAll(statement.getUsedLocalVariables());
            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables = statement.getInvokedMethodsThroughLocalVariables();
            for (AbstractVariable variable : invokedMethodsThroughLocalVariables.keySet()) {
                LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(variable);
                for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                }
            }
            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters = statement.getInvokedMethodsThroughParameters();
            for (AbstractVariable variable : invokedMethodsThroughParameters.keySet()) {
                LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(variable);
                for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                }
            }

            definedVariables.addAll(statement.getDefinedFieldsThroughThisReference());
            usedVariables.addAll(statement.getUsedFieldsThroughThisReference());
            definedVariables.addAll(statement.getDefinedFieldsThroughFields());
            usedVariables.addAll(statement.getUsedFieldsThroughFields());
            definedVariables.addAll(statement.getDefinedFieldsThroughParameters());
            usedVariables.addAll(statement.getUsedFieldsThroughParameters());
            definedVariables.addAll(statement.getDefinedFieldsThroughLocalVariables());
            usedVariables.addAll(statement.getUsedFieldsThroughLocalVariables());

            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields = statement.getInvokedMethodsThroughFields();
            for (AbstractVariable variable : invokedMethodsThroughFields.keySet()) {
                LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(variable);
                for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                    thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
                }
            }
            for (MethodInvocationObject methodInvocationObject : statement.getInvokedMethodsThroughThisReference()) {
                thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
            }
            for (MethodInvocationObject methodInvocationObject : statement.getInvokedStaticMethods()) {
                thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
            }
            List<SuperMethodInvocationObject> superMethodInvocations = statement.getSuperMethodInvocations();
            for (SuperMethodInvocationObject superMethodInvocationObject : superMethodInvocations) {
                thrownExceptionTypes.addAll(superMethodInvocationObject.getThrownExceptions());
            }
            List<MethodInvocationObject> methodInvocations = statement.getMethodInvocations();
            for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                thrownExceptionTypes.addAll(methodInvocationObject.getThrownExceptions());
            }
        }
    }
}
