package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.PsiElement;

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
