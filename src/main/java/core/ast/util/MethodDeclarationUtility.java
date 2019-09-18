package core.ast.util;

import com.intellij.psi.*;
import core.ast.decomposition.cfg.CompositeVariable;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.decomposition.cfg.AbstractVariable;

import java.util.Arrays;
import java.util.List;

public class MethodDeclarationUtility {

    public static PsiElement isGetter(PsiMethod methodDeclaration) {
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        List<PsiParameter> parameters = Arrays.asList(methodDeclaration.getParameterList().getParameters());
        if (methodBody != null) {
            List<PsiStatement> statements = Arrays.asList(methodBody.getStatements());
            if (statements.size() == 1 && parameters.size() == 0) {
                PsiStatement statement = statements.get(0);
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    return returnStatement.getReturnValue();
                }
            }
        }
        return null;
    }

    public static PsiElement isSetter(PsiMethod methodDeclaration) {
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        List<PsiParameter> parameters = Arrays.asList(methodDeclaration.getParameterList().getParameters());
        if (methodBody != null) {
            List<PsiStatement> statements = Arrays.asList(methodBody.getStatements());
            if (statements.size() == 1 && parameters.size() == 1) {
                PsiStatement statement = statements.get(0);
                if (statement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
                    PsiExpression expressionStatementExpression = expressionStatement.getExpression();
                    if (expressionStatementExpression instanceof PsiAssignmentExpression) {
                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) expressionStatementExpression;
                        PsiExpression rightHandSide = assignment.getRExpression();
                        if (rightHandSide instanceof PsiReferenceExpression) {
                            PsiReferenceExpression rightHandSideReference = (PsiReferenceExpression) rightHandSide;
                            PsiElement resolvedElement = rightHandSideReference.resolve();
                            if (resolvedElement != null && resolvedElement.equals(parameters.get(0).getOriginalElement())) {
                                PsiExpression leftHandSide = assignment.getLExpression();
                                if (leftHandSide instanceof PsiReferenceExpression) {
                                    return ((PsiReferenceExpression) leftHandSide).resolve();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static AbstractVariable createVariable(PsiVariable psiVariable, AbstractVariable rightPart) {
        AbstractVariable currentVariable;
        if (rightPart == null)
            currentVariable = new PlainVariable(psiVariable);
        else
            currentVariable = new CompositeVariable(psiVariable, rightPart);

        if (psiVariable.getParent() instanceof PsiReferenceExpression) {
            PsiReferenceExpression fieldAccess = (PsiReferenceExpression) psiVariable.getParent();
            if (fieldAccess.resolve() instanceof PsiReferenceExpression) {
                PsiElement resolvedReference = fieldAccess.resolve();
                if (resolvedReference instanceof PsiVariable) {
                    return createVariable((PsiVariable) resolvedReference, currentVariable);
                }
            } else if (psiVariable.getParent() instanceof PsiThisExpression) {
                return currentVariable;
            }
        } else {
            return currentVariable;
        }
        return null;
    }

    public static AbstractVariable processMethodInvocationExpression(PsiExpression expression) {
        AbstractVariable resultVariable = null;
        if (expression instanceof PsiReferenceExpression) {
            PsiElement resolvedReference = expression.getFirstChild();
            if (resolvedReference instanceof PsiReferenceExpression) {
                PsiElement resolvedElement = ((PsiReferenceExpression) resolvedReference).resolve();
                if (resolvedElement instanceof PsiVariable) {
                    resultVariable = createVariable((PsiVariable) resolvedElement, null);
                }
            }
        }
        return resultVariable;
    }

}
