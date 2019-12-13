package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.FieldObject;
import org.jetbrains.research.intellijdeodorant.core.ast.VariableDeclarationObject;

import java.util.Set;

class PDGSynchronizedNode extends PDGBlockNode {
    PDGSynchronizedNode(CFGSynchronizedNode cfgSynchronizedNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                        Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgSynchronizedNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        this.controlParent = cfgSynchronizedNode.getControlParent();
        determineDefinedAndUsedVariables();
    }
}
