package core.ast.decomposition;

import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSynchronizedStatement;

public class SynchronizedStatementObject extends CompositeStatementObject {

    SynchronizedStatementObject(PsiStatement statement, AbstractMethodFragment parent) {
        super(statement, StatementType.SYNCHRONIZED, parent);
        AbstractExpression abstractExpression = new AbstractExpression(
                ((PsiSynchronizedStatement) statement).getLockExpression(), this);
        this.addExpression(abstractExpression);
    }

}
