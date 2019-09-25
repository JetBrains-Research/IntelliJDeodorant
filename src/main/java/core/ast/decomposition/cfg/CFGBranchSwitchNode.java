package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

public class CFGBranchSwitchNode extends CFGBranchConditionalNode {

    public CFGBranchSwitchNode(AbstractStatement statement) {
        super(statement);
    }
}
