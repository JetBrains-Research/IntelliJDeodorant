package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.*;

import java.util.List;
import java.util.ListIterator;

class MyMethodBody {

    private final MyCompositeStatement compositeStatement;
    private final MethodBodyObject methodBodyObject;

    public MyMethodBody(MethodBodyObject methodBody) {
        this.methodBodyObject = methodBody;
        CompositeStatementObject compositeStatementObject = methodBody.getCompositeStatement();
        this.compositeStatement = new MyCompositeStatement(compositeStatementObject);

        List<AbstractStatement> statements = compositeStatementObject.getStatements();
        for (AbstractStatement statement : statements) {
            processStatement(compositeStatement, statement);
        }
    }

    private void processStatement(MyCompositeStatement parent, AbstractStatement statement) {
        if (statement instanceof StatementObject) {
            MyStatement child = new MyStatement(statement);
            parent.addStatement(child);
        } else if (statement instanceof CompositeStatementObject) {
            MyCompositeStatement child = new MyCompositeStatement(statement);
            parent.addStatement(child);
            CompositeStatementObject compositeStatementObject = (CompositeStatementObject) statement;
            List<AbstractExpression> expressions = compositeStatementObject.getExpressions();
            for (AbstractExpression expression : expressions) {
                MyAbstractExpression myAbstractExpression = new MyAbstractExpression(expression);
                child.addExpression(myAbstractExpression);
            }
            List<AbstractStatement> statements = compositeStatementObject.getStatements();
            for (AbstractStatement statement2 : statements) {
                processStatement(child, statement2);
            }
        }
    }

    public int getNumberOfAttributeInstructions() {
        return this.compositeStatement.getNumberOfAttributeInstructions();
    }

    public int getNumberOfMethodInvocations() {
        return this.compositeStatement.getNumberOfMethodInvocations();
    }

    public ListIterator<MyMethodInvocation> getMethodInvocationIterator() {
        return this.compositeStatement.getMethodInvocationIterator();
    }

    public ListIterator<MyAttributeInstruction> getAttributeInstructionIterator() {
        return this.compositeStatement.getAttributeInstructionIterator();
    }

    public void setAttributeInstructionReference(MyAttributeInstruction myAttributeInstruction, boolean reference) {
        this.compositeStatement.setAttributeInstructionReference(myAttributeInstruction, reference);
    }

    public MyAbstractStatement getAbstractStatement(AbstractStatement statement) {
        return this.compositeStatement.getAbstractStatement(statement);
    }

}
