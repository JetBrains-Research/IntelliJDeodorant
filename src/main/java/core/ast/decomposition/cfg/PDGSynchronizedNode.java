package core.ast.decomposition.cfg;

import core.ast.FieldObject;
import core.ast.VariableDeclarationObject;

import java.util.Set;

class PDGSynchronizedNode extends PDGBlockNode {
    PDGSynchronizedNode(CFGSynchronizedNode cfgSynchronizedNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
                        Set<FieldObject> fieldsAccessedInMethod) {
        super(cfgSynchronizedNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        this.controlParent = cfgSynchronizedNode.getControlParent();
        determineDefinedAndUsedVariables();
    }
}
