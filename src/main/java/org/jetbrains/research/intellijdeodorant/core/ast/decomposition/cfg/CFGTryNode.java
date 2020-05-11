package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.CatchClauseObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.TryStatementObject;

import java.util.ArrayList;
import java.util.List;

class CFGTryNode extends CFGBlockNode {
    private final List<String> handledExceptions;
    private final boolean hasResources;

    CFGTryNode(AbstractStatement statement) {
        super(statement);
        this.handledExceptions = new ArrayList<>();
        TryStatementObject tryStatement = (TryStatementObject) statement;
        this.hasResources = tryStatement.hasResources();
        for (CatchClauseObject catchClause : tryStatement.getCatchClauses()) {
            handledExceptions.addAll(catchClause.getExceptionTypes());
        }
    }

    boolean hasResources() {
        return hasResources;
    }

    boolean hasFinallyClauseClosingVariable(AbstractVariable variable) {
        return ((TryStatementObject) getStatement()).hasFinallyClauseClosingVariable(variable);
    }

}
