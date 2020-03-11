package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.PsiStatement;
import com.intellij.util.SmartList;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodInvocationObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class TryStatementObject extends CompositeStatementObject {
    private final SmartList<CatchClauseObject> catchClauses;
    private CompositeStatementObject finallyClause;

    public TryStatementObject(PsiStatement statement, AbstractMethodFragment parent) {
        super(statement, StatementType.TRY, parent);
        this.catchClauses = new SmartList<>();
    }

    public List<AbstractStatement> getStatementsInsideTryBlock() {
        CompositeStatementObject tryBlock = (CompositeStatementObject) getStatements().get(0);
        return tryBlock.getStatements();
    }

    public boolean hasResources() {
        return !super.getExpressions().isEmpty();
    }

    public void addCatchClause(CatchClauseObject catchClause) {
        catchClauses.add(catchClause);
        catchClause.setParent(this);
    }

    public List<CatchClauseObject> getCatchClauses() {
        return catchClauses;
    }

    public void setFinallyClause(CompositeStatementObject finallyClause) {
        this.finallyClause = finallyClause;
    }

    public CompositeStatementObject getFinallyClause() {
        return finallyClause;
    }

    public List<AbstractExpression> getExpressions() {
        SmartList<AbstractExpression> expressions = new SmartList<>(super.getExpressions());
        for (CatchClauseObject catchClause : catchClauses) {
            expressions.addAll(catchClause.getExpressions());
        }
        return expressions;
    }

    public List<String> stringRepresentation() {
        SmartList<String> stringRepresentation = new SmartList<>(super.stringRepresentation());
        for (CatchClauseObject catchClause : catchClauses) {
            stringRepresentation.addAll(catchClause.stringRepresentation());
        }
        if (finallyClause != null) {
            stringRepresentation.addAll(finallyClause.stringRepresentation());
        }
        return stringRepresentation;
    }

    public List<CompositeStatementObject> getIfStatements() {
        List<CompositeStatementObject> ifStatements = new ArrayList<>(super.getIfStatements());
        for (CatchClauseObject catchClause : catchClauses) {
            ifStatements.addAll(catchClause.getIfStatements());
        }
        if (finallyClause != null) {
            ifStatements.addAll(finallyClause.getIfStatements());
        }
        return ifStatements;
    }

    public List<CompositeStatementObject> getSwitchStatements() {
        List<CompositeStatementObject> switchStatements = new ArrayList<>(super.getSwitchStatements());
        for (CatchClauseObject catchClause : catchClauses) {
            switchStatements.addAll(catchClause.getSwitchStatements());
        }
        if (finallyClause != null) {
            switchStatements.addAll(finallyClause.getSwitchStatements());
        }
        return switchStatements;
    }

    public List<TryStatementObject> getTryStatements() {
        SmartList<TryStatementObject> tryStatements = new SmartList<>(super.getTryStatements());
        for (CatchClauseObject catchClause : catchClauses) {
            tryStatements.addAll(catchClause.getTryStatements());
        }
        if (finallyClause != null) {
            tryStatements.addAll(finallyClause.getTryStatements());
        }
        return tryStatements;
    }

    public boolean hasFinallyClauseClosingVariable(AbstractVariable variable) {
        if (finallyClause != null) {
            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables =
                    finallyClause.getInvokedMethodsThroughLocalVariables();
            for (AbstractVariable key : invokedMethodsThroughLocalVariables.keySet()) {
                if (key.equals(variable)) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughLocalVariables.get(key);
                    for (MethodInvocationObject methodInvocation : methodInvocations) {
                        if (methodInvocation.getMethodName().equals("close")) {
                            return true;
                        }
                    }
                }
            }
            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters =
                    finallyClause.getInvokedMethodsThroughParameters();
            for (AbstractVariable key : invokedMethodsThroughParameters.keySet()) {
                if (key.equals(variable)) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughParameters.get(key);
                    for (MethodInvocationObject methodInvocation : methodInvocations) {
                        if (methodInvocation.getMethodName().equals("close")) {
                            return true;
                        }
                    }
                }
            }
            Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields =
                    finallyClause.getInvokedMethodsThroughFields();
            for (AbstractVariable key : invokedMethodsThroughFields.keySet()) {
                if (key.equals(variable)) {
                    LinkedHashSet<MethodInvocationObject> methodInvocations = invokedMethodsThroughFields.get(key);
                    for (MethodInvocationObject methodInvocation : methodInvocations) {
                        if (methodInvocation.getMethodName().equals("close")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean hasCatchClause() {
        return !catchClauses.isEmpty();
    }
}
