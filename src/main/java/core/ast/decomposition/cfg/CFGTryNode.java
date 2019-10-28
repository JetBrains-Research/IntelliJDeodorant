package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;
import core.ast.decomposition.CatchClauseObject;
import core.ast.decomposition.TryStatementObject;

import java.util.ArrayList;
import java.util.List;

public class CFGTryNode extends CFGBlockNode {
    private List<String> handledExceptions;
    private boolean hasResources;

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

    public List<String> getHandledExceptions() {
        return handledExceptions;
    }

    boolean hasFinallyClauseClosingVariable(AbstractVariable variable) {
        return ((TryStatementObject) getStatement()).hasFinallyClauseClosingVariable(variable);
    }

    boolean hasCatchClause() {
        return ((TryStatementObject) getStatement()).hasCatchClause();
    }
}
