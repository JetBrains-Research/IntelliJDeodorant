package gr.uom.java.ast.util;

import com.intellij.psi.*;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.CompositeVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodDeclarationUtility {

    public static PsiMethodCallExpression isDelegate(PsiMethod methodDeclaration) {
        PsiElement parentClass = (PsiElement) methodDeclaration.getParent();
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        if (methodBody != null) {
            List<PsiStatement> statements = Arrays.asList(methodBody.getStatements());
            if (statements.size() == 1) {
                PsiStatement statement = statements.get(0);
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
                    PsiReferenceExpression methodInvocationExpression = methodInvocation.getMethodExpression();
                    if (methodInvocationExpression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression previousChainedMethodInvocation = (PsiMethodCallExpression) methodInvocationExpression;
                        List<PsiMethod> parentClassMethods = new ArrayList<PsiMethod>();
/*	    				if(parentClass instanceof PsiClass) {
	    					PsiMethod[] parentClassMethodArray = ((PsiClass)parentClass).getMethods();
							parentClassMethods.addAll(Arrays.asList(parentClassMethodArray));
	    				}
	    				else */
                        if (parentClass instanceof PsiClass) {
                            PsiClass enumDeclaration = (PsiClass) parentClass;
                            List<PsiMethod> bodyDeclarations = Arrays.asList(enumDeclaration.getMethods());
                            for (PsiMethod bodyDeclaration : bodyDeclarations) {
                                if (bodyDeclaration != null) {
                                    parentClassMethods.add((PsiMethod) bodyDeclaration);
                                }
                            }
                        }
                        boolean isDelegationChain = false;
                        boolean foundInParentClass = false;
                        for (PsiMethod parentClassMethod : parentClassMethods) {
                            if (parentClassMethod.getReference().equals(previousChainedMethodInvocation.getReference())) {
                                foundInParentClass = true;
                                PsiElement getterField = isGetter(parentClassMethod);
                                if (getterField == null)
                                    isDelegationChain = true;
                                break;
                            }
                        }
                        if (!isDelegationChain && foundInParentClass) {
                            return methodInvocation;
                        }
                    } else if (methodInvocationExpression.getOriginalElement() instanceof PsiField) {
                        PsiField fieldAccess = (PsiField) methodInvocationExpression;
                        //		IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
                        if (fieldAccess.getContainingClass().equals(parentClass)/* ||
	    						parentClass.resolveBinding().isSubTypeCompatible(variableBinding.getDeclaringClass())*/) {
                            return methodInvocation;
                        }
                    } else {
                        PsiReferenceExpression simpleName = methodInvocationExpression;
                        PsiElement binding = simpleName.resolve();
                        if (binding != null) {
                            PsiReference variableBinding = binding.getReference();
                            if (variableBinding instanceof PsiField) {
                                return methodInvocation;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static PsiElement isGetter(PsiMethod methodDeclaration) {
        PsiCodeBlock methodBody = methodDeclaration.getBody();
        List<PsiParameter> parameters = Arrays.asList(methodDeclaration.getParameterList().getParameters());
        if (methodBody != null) {
            List<PsiStatement> statements = Arrays.asList(methodBody.getStatements());
            if (statements.size() == 1 && parameters.size() == 0) {
                PsiStatement statement = statements.get(0);
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    PsiExpression returnStatementExpression = returnStatement.getReturnValue();
                    if (returnStatementExpression != null) {
                        return returnStatementExpression;
                    }
/*	    			else if(returnStatementExpression instanceof FieldAccess) {
	    				FieldAccess fieldAccess = (FieldAccess)returnStatementExpression;
	    				return fieldAccess.getName();
	    			}*/
                }
            }
        }
        return null;
    }

    public static PsiReferenceExpression isSetter(PsiMethod methodDeclaration) {
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
                            PsiReferenceExpression rightHandSideSimpleName = (PsiReferenceExpression) rightHandSide;
                            PsiExpression leftHandSide = assignment.getLExpression();
                            if (leftHandSide instanceof PsiReferenceExpression) {
                                return (PsiReferenceExpression) leftHandSide;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static AbstractVariable createVariable(PsiElement simpleName, AbstractVariable rightPart) {
        AbstractVariable currentVariable = null;
        if (rightPart == null)
            currentVariable = new PlainVariable(simpleName);
        else
            currentVariable = new CompositeVariable(simpleName, rightPart);
            
/*
            if(simpleName.getParent() instanceof QualifiedName) {
                    return createVariable(simpleName, currentVariable);
            }*/
        /*  else */
        if (simpleName.getParent() instanceof PsiReference) {
            PsiReference fieldAccess = (PsiReference) simpleName.getParent();
            //   PsiExpression fieldAccessExpression = fieldAccess.getExpression();
            if (fieldAccess instanceof PsiReferenceExpression) {
                PsiReferenceExpression fieldAccess2 = (PsiReferenceExpression) fieldAccess;
                return createVariable(fieldAccess2.getElement(), currentVariable);
            } else if (simpleName.getParent() instanceof PsiThisExpression) {
                return currentVariable;
            }
        } else {
            return currentVariable;
        }
        return null;
    }

    public static AbstractVariable processMethodInvocationExpression(PsiExpression expression) {
        if (expression instanceof PsiReferenceExpression) {
                PsiElement qualifiedName = ((PsiReferenceExpression) expression).resolve();
                return createVariable(qualifiedName, null);
/*                return createVariable(qualifiedName, null);
            } else if (expression.getOriginalElement() instanceof PsiField
                    || ((expression instanceof PsiReferenceExpression) && ((((PsiReferenceExpression)expression).resolve()) instanceof PsiField))) {
                //FieldAccess fieldAccess = (FieldAccess)expression;
                return createVariable(expression.getOriginalElement(), null);
            } else {
                return createVariable(((PsiReferenceExpression)expression).resolve(), null);
            }*/

        } else return null;
    }

/*	public static PsiElement getRightMostSimpleName(PsiExpression expression) {
		PsiElement simpleName = null;
		if(expression != null) {
			simpleName = expression;
		}
		else if(expression instanceof PsiReferenceExpression) {
			QualifiedName leftHandSideQualifiedName = (QualifiedName)expression;
			simpleName = leftHandSideQualifiedName.getName();
		}
		else if(expression instanceof PsiClassObjectAccessExpression) {
			FieldAccess leftHandSideFieldAccess = (FieldAccess)expression;
			simpleName = leftHandSideFieldAccess.getName();
		}
		else if(expression instanceof PsiArrayAccessExpression) {
			ArrayAccess leftHandSideArrayAccess = (ArrayAccess)expression;
			Expression array = leftHandSideArrayAccess.getArray();
			if(array instanceof PsiElement) {
				simpleName = (PsiElement)array;
			}
			else if(array instanceof QualifiedName) {
				QualifiedName arrayQualifiedName = (QualifiedName)array;
				simpleName = arrayQualifiedName.getName();
			}
			else if(array instanceof FieldAccess) {
				FieldAccess arrayFieldAccess = (FieldAccess)array;
				simpleName = arrayFieldAccess.getName();
			}
		}
		return simpleName;
	}*/
}
