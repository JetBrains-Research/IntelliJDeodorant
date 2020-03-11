package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.CompositeVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodDeclarationUtility {

    public static PsiMethodCallExpression isDelegate(PsiMethod methodDeclaration) {
        PsiClass parentClass = methodDeclaration.getContainingClass();
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        if (methodBody != null) {
            PsiStatement[] statements = methodBody.getStatements();
            if (statements.length == 1) {
                PsiStatement statement = statements[0];

                PsiMethodCallExpression methodInvocation = null;
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    if (returnStatement.getReturnValue() instanceof PsiMethodCallExpression) {
                        methodInvocation = (PsiMethodCallExpression) returnStatement.getReturnValue();
                    }
                } else if (statement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
                    if (expressionStatement.getExpression() instanceof PsiMethodCallExpression) {
                        methodInvocation = (PsiMethodCallExpression) expressionStatement.getExpression();
                    }
                }

                if (methodInvocation != null) {
                    PsiExpression methodInvocationExpression = methodInvocation.getMethodExpression().getQualifierExpression();
                    if (methodInvocationExpression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression previousChainedMethodInvocation = (PsiMethodCallExpression) methodInvocationExpression;
                        List<PsiMethod> parentClassMethods = new ArrayList<>();
                        if (parentClass.isEnum() || !parentClass.isAnnotationType()) {
                            PsiMethod[] parentClassMethodArray = parentClass.getMethods();
                            parentClassMethods.addAll(Arrays.asList(parentClassMethodArray));
                        }
                        boolean isDelegationChain = false;
                        boolean foundInParentClass = false;
                        for (PsiMethod parentClassMethod : parentClassMethods) {
                            if (parentClassMethod.equals(previousChainedMethodInvocation.resolveMethod())) {
                                foundInParentClass = true;
                                PsiExpression getterField = isGetter(parentClassMethod);
                                if (getterField == null) {
                                    isDelegationChain = true;
                                }
                                break;
                            }
                        }
                        if (!isDelegationChain && foundInParentClass) {
                            return methodInvocation;
                        }
                    } else if (methodInvocationExpression instanceof PsiReferenceExpression) {
                        PsiReferenceExpression simpleName = (PsiReferenceExpression) methodInvocationExpression;
                        PsiElement binding = simpleName.resolve();
                        if (binding instanceof PsiVariable) {
                            PsiVariable variableBinding = (PsiVariable) binding;
                            if (variableBinding instanceof PsiField || variableBinding instanceof PsiParameter) {
                                return methodInvocation;
                            }
                        }
                    } else if (methodInvocationExpression instanceof PsiThisExpression) {
                        return methodInvocation;
                    } else if (methodInvocationExpression == null) {
                        return methodInvocation;
                    }
                }
            }
        }
        return null;
    }

    // returns reference expression (see the difference between isSetter)
    public static PsiExpression isGetter(PsiMethod methodDeclaration) {
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        List<PsiParameter> parameters = Arrays.asList(methodDeclaration.getParameterList().getParameters());
        if (methodBody != null) {
            List<PsiStatement> statements = Arrays.asList(methodBody.getStatements());
            if (statements.size() == 1 && parameters.size() == 0) {
                PsiStatement statement = statements.get(0);
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    if (returnStatement.getReturnValue() instanceof PsiReferenceExpression) {
                        return returnStatement.getReturnValue();
                    }
                }
            }
        }
        return null;
    }

    // returns resolved reference (see the difference between isGetter)
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
            PsiElement variable = ((PsiReferenceExpression) expression).resolve();
            if (variable instanceof PsiVariable) {
                resultVariable = createVariable((PsiVariable) variable, null);
            }
        }
        return resultVariable;
    }

}
