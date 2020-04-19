package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;

import java.util.Set;

class PDGExitNode extends PDGStatementNode {
    private AbstractVariable returnedVariable;

    PDGExitNode(CFGNode cfgNode, Set<PsiVariable> variableDeclarationsInMethod,
                Set<PsiField> fieldsAccessedInMethod) {
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
