package core.ast.decomposition.cfg;

import com.intellij.psi.PsiSwitchStatement;
import core.ast.decomposition.AbstractStatement;

public class CFGSwitchCaseNode extends CFGNode {
    private boolean isDefault;

    public CFGSwitchCaseNode(AbstractStatement statement) {
        super(statement);
        PsiSwitchStatement switchCase = (PsiSwitchStatement) statement.getStatement();
        // TODO: how can we identify this case?
/*        if (switchCase)
            isDefault = true;
        else
            isDefault = false;*/
    }

    public boolean isDefault() {
        return isDefault;
    }
}
