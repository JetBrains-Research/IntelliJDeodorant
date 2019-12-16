package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import java.util.ArrayList;
import java.util.List;

public class CatchClauseObject {

    private CompositeStatementObject body;
    private final List<AbstractExpression> expressionList;
    private final List<String> exceptionTypes;
    private TryStatementObject parent;

    public CatchClauseObject() {
        this.expressionList = new ArrayList<>();
        this.exceptionTypes = new ArrayList<>();
        this.parent = null;
    }

    public void setParent(TryStatementObject parent) {
        this.parent = parent;
    }

    public TryStatementObject getParent() {
        return parent;
    }

    public void setBody(CompositeStatementObject body) {
        this.body = body;
    }

    public CompositeStatementObject getBody() {
        return body;
    }

    public void addExpression(AbstractExpression expression) {
        expressionList.add(expression);
    }

    public List<AbstractExpression> getExpressions() {
        return expressionList;
    }

    public List<String> getExceptionTypes() {
        return exceptionTypes;
    }

    public void addExceptionType(String exceptionType) {
        this.exceptionTypes.add(exceptionType);
    }

    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        stringRepresentation.addAll(body.stringRepresentation());
        return stringRepresentation;
    }

    public List<CompositeStatementObject> getIfStatements() {
        return new ArrayList<>(body.getIfStatements());
    }

    public List<CompositeStatementObject> getSwitchStatements() {
        return new ArrayList<>(body.getSwitchStatements());
    }

    public List<TryStatementObject> getTryStatements() {
        return new ArrayList<>(body.getTryStatements());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("catch");
        if (expressionList.size() > 0) {
            sb.append("(");
            if (!exceptionTypes.isEmpty()) {
                for (int i = 0; i < exceptionTypes.size() - 1; i++)
                    sb.append(exceptionTypes.get(i)).append(" |");
                sb.append(exceptionTypes.get(exceptionTypes.size() - 1)).append(" ");
            }
            for (AbstractExpression expression : expressionList)
                sb.append(expression.toString());
            sb.append(")");
        }
        return sb.toString();
    }
}
