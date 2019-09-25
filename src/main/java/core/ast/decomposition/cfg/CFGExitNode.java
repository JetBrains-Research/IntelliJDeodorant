package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

import java.util.ArrayList;
import java.util.List;

public class CFGExitNode extends CFGNode {
    private PlainVariable returnedVariable;

    public CFGExitNode(AbstractStatement statement) {
        super(statement);
        List<PlainVariable> usedVariables = new ArrayList<PlainVariable>(statement.getUsedLocalVariables());
        List<PlainVariable> usedFields = new ArrayList<PlainVariable>(statement.getUsedFieldsThroughThisReference());
        if (usedVariables.size() == 1 && usedFields.size() == 0) {
            returnedVariable = usedVariables.get(0);
        }
        if (usedVariables.size() == 0 && usedFields.size() == 1) {
            returnedVariable = usedFields.get(0);
        }
    }

    public PlainVariable getReturnedVariable() {
        return returnedVariable;
    }
}
