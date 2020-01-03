package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

class CFGBranchSwitchNode extends CFGBranchConditionalNode {

    CFGBranchSwitchNode(AbstractStatement statement) {
        super(statement);
    }
}
