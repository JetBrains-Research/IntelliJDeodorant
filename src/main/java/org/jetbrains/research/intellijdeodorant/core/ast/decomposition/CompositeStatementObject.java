package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;

import java.util.*;

/*
 * CompositeStatementObject represents the following AST Statement subclasses:
 * 1.	Block
 * 2.	DoStatement
 * 3.	EnhancedForStatement
 * 4.	ForStatement
 * 5.	IfStatement
 * 6.	LabeledStatement
 * 7.	SwitchStatement
 * 8.	SynchronizedStatement
 * 9.	TryStatement
 * 10.	WhileStatement
 */

public class CompositeStatementObject extends AbstractStatement {

    private final List<AbstractStatement> statementList;
    private final List<AbstractExpression> expressionList;

    public CompositeStatementObject(PsiElement statement, StatementType type, AbstractMethodFragment parent) {
        super(statement, type, parent);
        this.statementList = new ArrayList<>();
        this.expressionList = new ArrayList<>();
    }

    public void addStatement(AbstractStatement statement) {
        statementList.add(statement);
    }

    public List<AbstractStatement> getStatements() {
        return statementList;
    }

    public void addExpression(AbstractExpression expression) {
        expressionList.add(expression);
    }

    public List<AbstractExpression> getExpressions() {
        return expressionList;
    }

    public List<PsiField> getFieldInstructionsInExpressions() {
        List<PsiField> fieldInstructions = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            fieldInstructions.addAll(expression.getFieldInstructions());
        }
        return fieldInstructions;
    }

    public List<SuperFieldInstructionObject> getSuperFieldInstructionsInExpressions() {
        List<SuperFieldInstructionObject> superFieldInstructions = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            superFieldInstructions.addAll(expression.getSuperFieldInstructions());
        }
        return superFieldInstructions;
    }

    public List<PsiVariable> getLocalVariableDeclarationsInExpressions() {
        List<PsiVariable> localVariableDeclarations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            localVariableDeclarations.addAll(expression.getLocalVariableDeclarations());
        }
        return localVariableDeclarations;
    }

    public List<PsiVariable> getLocalVariableInstructionsInExpressions() {
        List<PsiVariable> localVariableInstructions = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            localVariableInstructions.addAll(expression.getLocalVariableInstructions());
        }
        return localVariableInstructions;
    }

    public List<MethodInvocationObject> getMethodInvocationsInExpressions() {
        List<MethodInvocationObject> methodInvocations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            methodInvocations.addAll(expression.getMethodInvocations());
        }
        return methodInvocations;
    }

    public List<SuperMethodInvocationObject> getSuperMethodInvocationsInExpressions() {
        List<SuperMethodInvocationObject> superMethodInvocations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            superMethodInvocations.addAll(expression.getSuperMethodInvocations());
        }
        return superMethodInvocations;
    }

    public List<CreationObject> getCreationsInExpressions() {
        List<CreationObject> creations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            creations.addAll(expression.getCreations());
        }
        return creations;
    }

    public List<ArrayCreationObject> getArrayCreationsInExpressions() {
        List<ArrayCreationObject> creations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            creations.addAll(expression.getArrayCreations());
        }
        return creations;
    }

    public List<ClassInstanceCreationObject> getClassInstanceCreationsInExpressions() {
        List<ClassInstanceCreationObject> creations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            creations.addAll(expression.getClassInstanceCreations());
        }
        return creations;
    }

    public List<LiteralObject> getLiteralsInExpressions() {
        List<LiteralObject> literals = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            literals.addAll(expression.getLiterals());
        }
        return literals;
    }

    public List<PsiAnonymousClass> getAnonymousClassDeclarationsInExpressions() {
        List<PsiAnonymousClass> anonymousClassDeclarations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            anonymousClassDeclarations.addAll(expression.getAnonymousClassDeclarations());
        }
        return anonymousClassDeclarations;
    }

    public Set<MethodInvocationObject> getInvokedStaticMethodsInExpressions() {
        Set<MethodInvocationObject> staticMethodInvocations = new LinkedHashSet<>();
        for (AbstractExpression expression : expressionList) {
            staticMethodInvocations.addAll(expression.getInvokedStaticMethods());
        }
        return staticMethodInvocations;
    }

    public Set<PlainVariable> getUsedFieldsThroughThisReferenceInExpressions() {
        Set<PlainVariable> usedFieldsThroughThisReference = new LinkedHashSet<>();
        for (AbstractExpression expression : expressionList) {
            usedFieldsThroughThisReference.addAll(expression.getUsedFieldsThroughThisReference());
        }
        return usedFieldsThroughThisReference;
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReferenceInExpressions() {
        Set<PlainVariable> definedFieldsThroughThisReference = new LinkedHashSet<>();
        for (AbstractExpression expression : expressionList) {
            definedFieldsThroughThisReference.addAll(expression.getDefinedFieldsThroughThisReference());
        }
        return definedFieldsThroughThisReference;
    }

    public Set<MethodInvocationObject> getInvokedMethodsThroughThisReferenceInExpressions() {
        Set<MethodInvocationObject> invokedMethodsThroughThisReference = new LinkedHashSet<>();
        for (AbstractExpression expression : expressionList) {
            invokedMethodsThroughThisReference.addAll(expression.getInvokedMethodsThroughThisReference());
        }
        return invokedMethodsThroughThisReference;
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReferenceInExpressions() {
        List<MethodInvocationObject> invokedMethodsThroughThisReference = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            invokedMethodsThroughThisReference.addAll(expression.getNonDistinctInvokedMethodsThroughThisReference());
        }
        return invokedMethodsThroughThisReference;
    }

    public List<MethodInvocationObject> getNonDistinctInvokedStaticMethodsInExpressions() {
        List<MethodInvocationObject> staticMethodInvocations = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            staticMethodInvocations.addAll(expression.getNonDistinctInvokedStaticMethods());
        }
        return staticMethodInvocations;
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReferenceInExpressions() {
        List<PlainVariable> nonDistinctDefinedFieldsThroughThisReference = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            nonDistinctDefinedFieldsThroughThisReference.addAll(expression.getNonDistinctDefinedFieldsThroughThisReference());
        }
        return nonDistinctDefinedFieldsThroughThisReference;
    }

    public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReferenceInExpressions() {
        List<PlainVariable> nonDistinctUsedFieldsThroughThisReference = new ArrayList<>();
        for (AbstractExpression expression : expressionList) {
            nonDistinctUsedFieldsThroughThisReference.addAll(expression.getNonDistinctUsedFieldsThroughThisReference());
        }
        return nonDistinctUsedFieldsThroughThisReference;
    }

    public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocationsInExpressions() {
        Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations =
                new LinkedHashMap<>();
        for (AbstractExpression expression : expressionList) {
            parametersPassedAsArgumentsInMethodInvocations.putAll(expression.getParametersPassedAsArgumentsInMethodInvocations());
        }
        return parametersPassedAsArgumentsInMethodInvocations;
    }

    public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocationsInExpressions() {
        Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> parametersPassedAsArgumentsInSuperMethodInvocations =
                new LinkedHashMap<>();
        for (AbstractExpression expression : expressionList) {
            parametersPassedAsArgumentsInSuperMethodInvocations.putAll(expression.getParametersPassedAsArgumentsInSuperMethodInvocations());
        }
        return parametersPassedAsArgumentsInSuperMethodInvocations;
    }

    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        for (AbstractStatement statement : statementList) {
            stringRepresentation.addAll(statement.stringRepresentation());
        }
        return stringRepresentation;
    }

    public List<CompositeStatementObject> getIfStatements() {
        List<CompositeStatementObject> ifStatements = new ArrayList<>();
        if (this.getType().equals(StatementType.IF))
            ifStatements.add(this);
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                ifStatements.addAll(composite.getIfStatements());
            }
        }
        return ifStatements;
    }

    public List<CompositeStatementObject> getSwitchStatements() {
        List<CompositeStatementObject> switchStatements = new ArrayList<>();
        if (this.getType().equals(StatementType.SWITCH))
            switchStatements.add(this);
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                switchStatements.addAll(composite.getSwitchStatements());
            }
        }
        return switchStatements;
    }

    public List<TryStatementObject> getTryStatements() {
        List<TryStatementObject> tryStatements = new ArrayList<>();
        if (this.getType().equals(StatementType.TRY))
            tryStatements.add((TryStatementObject) this);
        for (AbstractStatement statement : statementList) {
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) statement;
                tryStatements.addAll(composite.getTryStatements());
            }
        }
        return tryStatements;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getType().toString());
        if (expressionList.size() > 0) {
            sb.append("(");
            for (int i = 0; i < expressionList.size() - 1; i++) {
                sb.append(expressionList.get(i).toString()).append("; ");
            }
            sb.append(expressionList.get(expressionList.size() - 1).toString());
            sb.append(")");
        }
        sb.append("\n");
        return sb.toString();
    }
}
