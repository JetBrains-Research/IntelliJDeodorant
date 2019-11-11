package core.ast.decomposition;

import com.intellij.psi.*;
import core.ast.util.ExpressionExtractor;

import java.util.ArrayList;
import java.util.List;

/*
 * StatementObject represents the following AST Statement subclasses:
 * 1.	ExpressionStatement
 * 2.	VariableDeclarationStatement
 * 3.	ConstructorInvocation
 * 4.	SuperConstructorInvocation
 * 5.	ReturnStatement
 * 6.	AssertStatement
 * 7.	BreakStatement
 * 8.	ContinueStatement
 * 9.	SwitchCase
 * 10.	EmptyStatement
 * 11.	ThrowStatement
 */

public class StatementObject extends AbstractStatement {

    public StatementObject(PsiStatement statement, StatementType type, AbstractMethodFragment parent) {
        super(statement, type, parent);

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
        List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
        processVariables(expressionExtractor.getVariableInstructions(statement), assignments, postfixExpressions, prefixExpressions);
        processLocalVariableDeclaration(statement);
        processMethodInvocations(expressionExtractor.getMethodInvocations(statement));
        processClassInstanceCreations(expressionExtractor.getClassInstanceCreations(statement));
        processArrayCreations(expressionExtractor.getArrayCreations(statement));
        processLiterals(expressionExtractor.getLiterals(statement));
        if (statement instanceof PsiThrowStatement) {
            processThrowStatement((PsiThrowStatement) statement);
        }
    }

    public String toString() {
        return getStatement().toString();
    }

    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        return stringRepresentation;
    }
}
