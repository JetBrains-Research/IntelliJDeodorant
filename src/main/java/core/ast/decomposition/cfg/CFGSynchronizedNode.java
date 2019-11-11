package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

class CFGSynchronizedNode extends CFGBlockNode {
    CFGSynchronizedNode(AbstractStatement statement) {
        super(statement);
    }
}
