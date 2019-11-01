package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

class CFGBranchSwitchNode extends CFGBranchConditionalNode {

    CFGBranchSwitchNode(AbstractStatement statement) {
        super(statement);
    }
}
