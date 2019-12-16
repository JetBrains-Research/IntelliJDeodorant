package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiSwitchLabelStatement;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;

class CFGSwitchCaseNode extends CFGNode {
    private final boolean isDefault;

    CFGSwitchCaseNode(AbstractStatement statement) {
        super(statement);
        PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) statement.getStatement();
        isDefault = switchCase.isDefaultCase();
    }

    boolean isDefault() {
        return isDefault;
    }
}
