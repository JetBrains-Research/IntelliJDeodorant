package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.FieldObject;
import org.jetbrains.research.intellijdeodorant.core.ast.VariableDeclarationObject;

import java.util.Set;

class PDGExitNode extends PDGStatementNode {
    private AbstractVariable returnedVariable;

    PDGExitNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        if (cfgNode instanceof CFGExitNode) {
            CFGExitNode exitNode = (CFGExitNode) cfgNode;
            returnedVariable = exitNode.getReturnedVariable();
        }
    }

    AbstractVariable getReturnedVariable() {
        return returnedVariable;
    }
}
