package gr.uom.java.ast.decomposition;

import java.util.List;

import com.intellij.psi.PsiExpression;
import gr.uom.java.ast.ASTInformation;
import gr.uom.java.ast.ASTInformationGenerator;
import gr.uom.java.ast.util.ExpressionExtractor;

public class AbstractExpression extends AbstractMethodFragment {

    private ASTInformation expression;

    public AbstractExpression(PsiExpression expression) {
        super(null);
        this.expression = ASTInformationGenerator.generateASTInformation(expression);
        processExpression(expression);
    }

    public AbstractExpression(PsiExpression expression, AbstractMethodFragment parent) {
        super(parent);
        this.expression = ASTInformationGenerator.generateASTInformation(expression);
        processExpression(expression);
    }

    private void processExpression(PsiExpression expression) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(expression);
        List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(expression);
        processVariables(expressionExtractor.getVariableInstructions(expression), assignments, postfixExpressions, prefixExpressions);
        processMethodInvocations(expressionExtractor.getMethodInvocations(expression));
        processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(expression));
        processArrayCreations(expressionExtractor.getArrayCreations(expression));
        processLiterals(expressionExtractor.getLiterals(expression));
    }

    public PsiExpression getExpression() {
        return (PsiExpression) this.expression.recoverASTNode();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((expression == null) ? 0 : expression.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractExpression other = (AbstractExpression) obj;
        if (expression == null) {
            return other.expression == null;
        } else return expression.equals(other.expression);
    }

    public String toString() {
        return getExpression().toString();
    }
}
