package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

class CFGSynchronizedNode extends CFGBlockNode {
    CFGSynchronizedNode(AbstractStatement statement) {
        super(statement);
    }
}
