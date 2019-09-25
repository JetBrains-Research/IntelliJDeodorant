package core.ast.decomposition.cfg;

import core.ast.decomposition.AbstractStatement;

public class CFGSynchronizedNode extends CFGBlockNode {
    public CFGSynchronizedNode(AbstractStatement statement) {
        super(statement);
    }
}
