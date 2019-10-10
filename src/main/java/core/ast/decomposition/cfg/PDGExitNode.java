package core.ast.decomposition.cfg;

import core.ast.FieldObject;
import core.ast.VariableDeclarationObject;

import java.util.Set;

public class PDGExitNode extends PDGStatementNode {
    private AbstractVariable returnedVariable;

    public PDGExitNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                       Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        if (cfgNode instanceof CFGExitNode) {
            CFGExitNode exitNode = (CFGExitNode) cfgNode;
            returnedVariable = exitNode.getReturnedVariable();
        }
    }

    public AbstractVariable getReturnedVariable() {
        return returnedVariable;
    }
}
