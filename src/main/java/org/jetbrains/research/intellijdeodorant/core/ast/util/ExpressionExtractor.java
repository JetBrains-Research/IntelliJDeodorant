package org.jetbrains.research.intellijdeodorant.core.ast.util;

import java.util.*;

import com.intellij.psi.*;

public class ExpressionExtractor {

    private ExpressionInstanceChecker instanceChecker;

    // returns a List of Variable objects
    public List<PsiExpression> getVariableInstructions(PsiStatement statement) {
        instanceChecker = new InstanceOfVariable();
        return getExpressions(statement);
    }

    // returns a List of Variable objects
    public List<PsiExpression> getVariableInstructions(PsiStatement[] statement) {
        instanceChecker = new InstanceOfVariable();
        return getExpressions(statement);
    }

    // returns a List of Variable objects
    public List<PsiExpression> getVariableInstructions(PsiExpression expression) {
        instanceChecker = new InstanceOfVariable();
        return getExpressions(expression);
    }

    // returns a List of MethodInvocation and SuperMethodInvocation objects
    public List<PsiExpression> getMethodInvocations(PsiStatement statement) {
        instanceChecker = new InstanceOfMethodInvocation();
        return getExpressions(statement);
    }

    // returns a List of SuperMethodInvocation objects
    public List<PsiExpression> getSuperMethodInvocations(PsiElement statement) {
        instanceChecker = new InstanceOfSuperMethodInvocation();
        return getExpressions(statement);
    }

    // returns a List of MethodInvocation and SuperMethodInvocation objects
    public List<PsiExpression> getMethodInvocations(PsiExpression expression) {
        instanceChecker = new InstanceOfMethodInvocation();
        return getExpressions(expression);
    }

    // returns a List of SuperMethodInvocation objects
    public List<PsiExpression> getSuperMethodInvocations(PsiExpression expression) {
        instanceChecker = new InstanceOfSuperMethodInvocation();
        return getExpressions(expression);
    }

    // returns a List of FieldAccess objects
    public List<PsiExpression> getFieldAccesses(PsiStatement statement) {
        instanceChecker = new InstanceOfFieldAccess();
        return getExpressions(statement);
    }

    // returns a List of SuperFieldAccess objects
    public List<PsiExpression> getSuperFieldAccesses(PsiElement statement) {
        instanceChecker = new InstanceOfSuperFieldAccess();
        return getExpressions(statement);
    }

    // returns a List of FieldAccess objects
    public List<PsiExpression> getFieldAccesses(PsiExpression expression) {
        instanceChecker = new InstanceOfFieldAccess();
        return getExpressions(expression);
    }

    // returns a List of SuperFieldAccess objects
    public List<PsiExpression> getSuperFieldAccesses(PsiExpression expression) {
        instanceChecker = new InstanceOfSuperFieldAccess();
        return getExpressions(expression);
    }

    // returns a List of ClassInstanceCreation objects
    public List<PsiExpression> getClassInstanceCreations(PsiStatement statement) {
        instanceChecker = new InstanceOfClassInstanceCreation();
        return getExpressions(statement);
    }

    // returns a List of ClassInstanceCreation objects
    public List<PsiExpression> getClassInstanceCreations(PsiStatement[] statement) {
        instanceChecker = new InstanceOfClassInstanceCreation();
        return getExpressions(statement);
    }


    // returns a List of ClassInstanceCreation objects
    public List<PsiExpression> getClassInstanceCreations(PsiExpression expression) {
        instanceChecker = new InstanceOfClassInstanceCreation();
        return getExpressions(expression);
    }

    // returns a List of ArrayCreation objects
    public List<PsiExpression> getArrayCreations(PsiStatement statement) {
        instanceChecker = new InstanceOfArrayCreation();
        return getExpressions(statement);
    }

    // returns a List of ArrayCreation objects
    public List<PsiExpression> getArrayCreations(PsiExpression expression) {
        instanceChecker = new InstanceOfArrayCreation();
        return getExpressions(expression);
    }

    // returns a List of ArrayAccess objects
    public List<PsiExpression> getArrayAccesses(PsiStatement statement) {
        instanceChecker = new InstanceOfArrayAccess();
        return getExpressions(statement);
    }

    // returns a List of ArrayAccess objects
    public List<PsiExpression> getArrayAccesses(PsiExpression expression) {
        instanceChecker = new InstanceOfArrayAccess();
        return getExpressions(expression);
    }

    // returns a List of ThisExpression objects
    public List<PsiExpression> getThisExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfThisExpression();
        return getExpressions(statement);
    }

    // returns a List of ThisExpression objects
    public List<PsiExpression> getThisExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfThisExpression();
        return getExpressions(expression);
    }

    // returns a List of TypeLiteral objects
    public List<PsiExpression> getTypeLiterals(PsiStatement statement) {
        instanceChecker = new InstanceOfTypeLiteral();
        return getExpressions(statement);
    }

    // returns a List of Type, String, Boolean, Character, Number, and Null Literal objects
    public List<PsiExpression> getLiterals(PsiStatement statement) {
        instanceChecker = new InstanceOfLiteral();
        return getExpressions(statement);
    }

    // returns a List of Type, String, Boolean, Character, Number, and Null Literal objects
    public List<PsiExpression> getLiterals(PsiExpression expression) {
        instanceChecker = new InstanceOfLiteral();
        return getExpressions(expression);
    }

    // returns a List of CastExpression objects
    public List<PsiExpression> getCastExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfCastExpression();
        return getExpressions(statement);
    }

    // returns a List of InfixExpression objects
    public List<PsiExpression> getInfixExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfInfixExpression();
        return getExpressions(statement);
    }

    // returns a List of InfixExpression objects
    public List<PsiExpression> getInfixExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfInfixExpression();
        return getExpressions(expression);
    }

    public List<PsiExpression> getInstanceOfExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfInstanceOfExpression();
        return getExpressions(statement);
    }

    public List<PsiExpression> getInstanceOfExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfInstanceOfExpression();
        return getExpressions(expression);
    }

    // returns a List of Assignment objects
    public List<PsiExpression> getAssignments(PsiStatement statement) {
        instanceChecker = new InstanceOfAssignment();
        return getExpressions(statement);
    }

    // returns a List of Assignment objects
    public List<PsiExpression> getAssignments(PsiExpression expression) {
        instanceChecker = new InstanceOfAssignment();
        return getExpressions(expression);
    }

    // returns a List of PostfixExpression objects
    public List<PsiExpression> getPostfixExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfPostfixExpression();
        return getExpressions(statement);
    }

    // returns a List of PostfixExpression objects
    public List<PsiExpression> getPostfixExpressions(PsiStatement[] statement) {
        instanceChecker = new InstanceOfPostfixExpression();
        return getExpressions(statement);
    }

    // returns a List of PostfixExpression objects
    public List<PsiExpression> getPostfixExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfPostfixExpression();
        return getExpressions(expression);
    }

    // returns a List of PrefixExpression objects
    public List<PsiExpression> getPrefixExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfPrefixExpression();
        return getExpressions(statement);
    }

    // returns a List of PrefixExpression objects
    public List<PsiExpression> getPrefixExpressions(PsiStatement[] statement) {
        instanceChecker = new InstanceOfPrefixExpression();
        return getExpressions(statement);
    }

    // returns a List of PrefixExpression objects
    public List<PsiExpression> getPrefixExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfPrefixExpression();
        return getExpressions(expression);
    }

    // returns a List of ConditionalExpression objects
    public List<PsiExpression> getConditionalExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfConditionalExpression();
        return getExpressions(statement);
    }

    // returns a List of VariableDeclarationExpression objects
    public List<PsiExpression> getConditionalExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfConditionalExpression();
        return getExpressions(expression);
    }

    // returns a List of variable updaters (i.e. Assignment, PrefixExpression, or PostfixExpression objects)
    public List<PsiExpression> getVariableModifiers(PsiStatement statement) {
        instanceChecker = new InstanceOfVariableModifier();
        return getExpressions(statement);
    }

    // returns a List of variable updaters (i.e. Assignment, PrefixExpression, or PostfixExpression objects)
    public List<PsiExpression> getVariableModifiers(PsiExpression expression) {
        instanceChecker = new InstanceOfVariableModifier();
        return getExpressions(expression);
    }

    private List<PsiExpression> getExpressions(PsiElement[] elements) {
        List<PsiExpression> result = new ArrayList<>();
        for (PsiElement element : elements) {
            result.addAll(getExpressions(element));
        }

        return result;
    }

    private List<PsiExpression> getExpressions(PsiElement element) {
        List<PsiExpression> expressionList = new ArrayList<>();
        if (element instanceof PsiCodeBlock) {
            PsiCodeBlock psiCodeBlock = (PsiCodeBlock) element;
            PsiStatement[] blockStatements = psiCodeBlock.getStatements();
            for (PsiStatement blockStatement : blockStatements)
                expressionList.addAll(getExpressions(blockStatement));
        } else if (element instanceof PsiBlockStatement) {
            PsiBlockStatement psiBlockStatement = (PsiBlockStatement) element;
            PsiCodeBlock psiCodeBlock = psiBlockStatement.getCodeBlock();
            PsiStatement[] blockStatements = psiCodeBlock.getStatements();
            for (PsiStatement blockStatement : blockStatements)
                expressionList.addAll(getExpressions(blockStatement));
        } else if (element instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) element;
            PsiExpression expression = ifStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(ifStatement.getThenBranch()));
            if (ifStatement.getElseBranch() != null) {
                expressionList.addAll(getExpressions(ifStatement.getElseBranch()));
            }
        } else if (element instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) element;
            PsiStatement initializer = forStatement.getInitialization();
            expressionList.addAll(getExpressions(initializer));
            PsiExpression expression = forStatement.getCondition();
            if (expression != null)
                expressionList.addAll(getExpressions(expression));
            PsiStatement updater = forStatement.getUpdate();
            expressionList.addAll(getExpressions(updater));
            expressionList.addAll(getExpressions(forStatement.getBody()));
        } else if (element instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) element;
            PsiExpression expression = enhancedForStatement.getIteratedValue();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(enhancedForStatement.getBody()));
            expressionList.addAll(getExpressions(enhancedForStatement.getIterationParameter().getInitializer()));
        } else if (element instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) element;
            PsiExpression expression = whileStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(whileStatement.getBody()));
        } else if (element instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) element;
            PsiExpression expression = doStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(doStatement.getBody()));
        } else if (element instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) element;
            PsiExpression expression = expressionStatement.getExpression();
            expressionList.addAll(getExpressions(expression));
        } else if (element instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) element;
            PsiExpression expression = switchStatement.getExpression();
            expressionList.addAll(getExpressions(expression));
            PsiCodeBlock statementBody = switchStatement.getBody();
            if (statementBody != null) {
                PsiStatement[] statements = statementBody.getStatements();
                for (PsiStatement psiStatement : statements)
                    expressionList.addAll(getExpressions(psiStatement));
            }
        } else if (element instanceof PsiAssertStatement) {
            PsiAssertStatement assertStatement = (PsiAssertStatement) element;
            PsiExpression expression = assertStatement.getAssertCondition();
            expressionList.addAll(getExpressions(expression));
            PsiExpression message = assertStatement.getAssertDescription();
            if (message != null)
                expressionList.addAll(getExpressions(message));
        } else if (element instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) element;
            expressionList.addAll(getExpressions(labeledStatement.getStatement()));
        } else if (element instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) element;
            PsiExpression expression = returnStatement.getReturnValue();
            expressionList.addAll(getExpressions(expression));
        } else if (element instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) element;
            PsiExpression expression = synchronizedStatement.getLockExpression();
            expressionList.addAll(getExpressions(expression));
            PsiCodeBlock statementBody = synchronizedStatement.getBody();
            if (statementBody != null) {
                PsiStatement[] statements = statementBody.getStatements();
                for (PsiStatement psiStatement : statements) {
                    expressionList.addAll(getExpressions(psiStatement));
                }
            }
        } else if (element instanceof PsiThrowStatement) {
            PsiThrowStatement throwStatement = (PsiThrowStatement) element;
            PsiExpression expression = throwStatement.getException();
            expressionList.addAll(getExpressions(expression));
        } else if (element instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) element;
            PsiCodeBlock tryBody = tryStatement.getTryBlock();
            if (tryBody != null) {
                PsiStatement[] tryStatements = tryBody.getStatements();
                for (PsiStatement psiStatement : tryStatements) {
                    expressionList.addAll(getExpressions(psiStatement));
                }
            }
            PsiCodeBlock[] catchClauses = tryStatement.getCatchBlocks();
            for (PsiCodeBlock catchClause : catchClauses) {
                List<PsiStatement> statements = Arrays.asList(catchClause.getStatements());
                statements.forEach(s -> expressionList.addAll(getExpressions(s)));
            }
            if (tryStatement.getFinallyBlock() != null) {
                List<PsiStatement> finallyBlock = Arrays.asList(tryStatement.getFinallyBlock().getStatements());
                finallyBlock.forEach(f -> expressionList.addAll(getExpressions(f)));
            }
        } else if (element instanceof PsiConstructorCall) {
            PsiConstructorCall constructorInvocation = (PsiConstructorCall) element;
            PsiExpressionList arguments = constructorInvocation.getArgumentList();
            if (arguments != null) {
                PsiExpression[] psiExpressions = arguments.getExpressions();
                for (PsiExpression argument : psiExpressions)
                    expressionList.addAll(getExpressions(argument));
            }
        } else if (element instanceof PsiSuperExpression) {
            PsiSuperExpression superInvocation = (PsiSuperExpression) element;
            expressionList.add(superInvocation);
        } else if (element instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) element;
            PsiElement[] declaredElements = variableDeclarationStatement.getDeclaredElements();
            for (PsiElement psiElement : declaredElements) {
                if (psiElement instanceof PsiVariable) {
                    expressionList.addAll(getExpressions(((PsiVariable) psiElement).getInitializer()));
                }
            }
        } else if (element instanceof PsiExpressionListStatement) {
            PsiExpressionListStatement listStatement = (PsiExpressionListStatement) element;
            for (PsiExpression psiExpression : listStatement.getExpressionList().getExpressions()) {
                expressionList.addAll(getExpressions(psiExpression));
            }
        }
        return expressionList;
    }

    private List<PsiExpression> getExpressions(PsiExpression expression) {
        List<PsiExpression> expressionList = new ArrayList<>();
        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
            expressionList.addAll(getExpressions(methodInvocation.getMethodExpression()));
            expressionList.addAll(getExpressions(methodInvocation.getMethodExpression().getQualifierExpression()));
            PsiExpressionList arguments = methodInvocation.getArgumentList();
            for (PsiExpression argument : arguments.getExpressions())
                expressionList.addAll(getExpressions(argument));
            if (instanceChecker.instanceOf(methodInvocation))
                expressionList.add(methodInvocation);
        } else if (expression instanceof PsiAssignmentExpression) {
            PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
            expressionList.addAll(getExpressions(assignment.getLExpression()));
            expressionList.addAll(getExpressions(assignment.getRExpression()));
            if (instanceChecker.instanceOf(assignment))
                expressionList.add(assignment);
        } else if (expression instanceof PsiTypeCastExpression) {
            PsiTypeCastExpression castExpression = (PsiTypeCastExpression) expression;
            expressionList.addAll(getExpressions(castExpression.getOperand()));
            if (instanceChecker.instanceOf(castExpression))
                expressionList.add(castExpression);
        } else if (expression instanceof PsiNewExpression) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
            if (classInstanceCreation.getQualifier() != null)
                expressionList.addAll(getExpressions(classInstanceCreation.getQualifier()));
            if (classInstanceCreation.getArgumentList() != null) {
                PsiExpression[] arguments = classInstanceCreation.getArgumentList().getExpressions();
                for (PsiExpression argument : arguments)
                    expressionList.addAll(getExpressions(argument));
            }
            if (instanceChecker.instanceOf(classInstanceCreation))
                expressionList.add(classInstanceCreation);
            PsiAnonymousClass anonymousClassDeclaration = classInstanceCreation.getAnonymousClass();
            if (anonymousClassDeclaration != null) {
                expressionList.addAll(getExpressions(anonymousClassDeclaration));
            }
        } else if (expression instanceof PsiConditionalExpression) {
            PsiConditionalExpression conditionalExpression = (PsiConditionalExpression) expression;
            expressionList.addAll(getExpressions(conditionalExpression.getCondition()));
            expressionList.addAll(getExpressions(conditionalExpression.getThenExpression()));
            expressionList.addAll(getExpressions(conditionalExpression.getElseExpression()));
            if (instanceChecker.instanceOf(conditionalExpression))
                expressionList.add(conditionalExpression);
        } else if (expression instanceof PsiBinaryExpression) {
            PsiBinaryExpression infixExpression = (PsiBinaryExpression) expression;
            expressionList.addAll(getExpressions(infixExpression.getLOperand()));
            expressionList.addAll(getExpressions(infixExpression.getROperand()));
            PsiExpression[] extendedOperands = infixExpression.getOperands();
            for (PsiExpression operand : extendedOperands)
                expressionList.addAll(getExpressions(operand));
            if (instanceChecker.instanceOf(infixExpression))
                expressionList.add(infixExpression);
        } else if (expression instanceof PsiInstanceOfExpression) {
            PsiInstanceOfExpression instanceofExpression = (PsiInstanceOfExpression) expression;
            expressionList.addAll(getExpressions(instanceofExpression.getOperand()));
            if (instanceChecker.instanceOf(instanceofExpression))
                expressionList.add(instanceofExpression);
        } else if (expression instanceof PsiParenthesizedExpression) {
            PsiParenthesizedExpression parenthesizedExpression = (PsiParenthesizedExpression) expression;
            expressionList.addAll(getExpressions(parenthesizedExpression.getExpression()));
            if (instanceChecker.instanceOf(parenthesizedExpression))
                expressionList.add(parenthesizedExpression);
        } else if (expression instanceof PsiPostfixExpression) {
            PsiPostfixExpression postfixExpression = (PsiPostfixExpression) expression;
            expressionList.addAll(getExpressions(postfixExpression.getOperand()));
            if (instanceChecker.instanceOf(postfixExpression))
                expressionList.add(postfixExpression);
        } else if (expression instanceof PsiPrefixExpression) {
            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) expression;
            expressionList.addAll(getExpressions(prefixExpression.getOperand()));
            if (instanceChecker.instanceOf(prefixExpression))
                expressionList.add(prefixExpression);
        } else if (expression instanceof PsiSuperExpression) {
            PsiSuperExpression superInvocation = (PsiSuperExpression) expression;
            if (instanceChecker.instanceOf(superInvocation))
                expressionList.add(superInvocation);
        } else if (expression instanceof PsiArrayAccessExpression) {
            PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression;
            expressionList.addAll(getExpressions(arrayAccess.getArrayExpression()));
            expressionList.addAll(getExpressions(arrayAccess.getIndexExpression()));
            if (instanceChecker.instanceOf(arrayAccess))
                expressionList.add(arrayAccess);
        } else if (expression instanceof PsiArrayInitializerExpression) {
            PsiArrayInitializerExpression arrayInitializer = (PsiArrayInitializerExpression) expression;
            PsiExpression[] expressions = arrayInitializer.getInitializers();
            for (PsiExpression arrayInitializerExpression : expressions)
                expressionList.addAll(getExpressions(arrayInitializerExpression));
            if (instanceChecker.instanceOf(arrayInitializer))
                expressionList.add(arrayInitializer);
        } else if (expression instanceof PsiReferenceExpression) {
            //TODO plugin does not find field accesses such as FIELD.toString()
            // probably because it finds PsiReferenceExpression FIELD.toString which is not a field access.
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            expressionList.addAll(getExpressions(simpleName.getQualifierExpression()));
            if (instanceChecker.instanceOf(simpleName))
                expressionList.add(simpleName);
        } else if (expression instanceof PsiQualifiedExpression) {
            PsiQualifiedExpression qualifiedName = (PsiQualifiedExpression) expression;
            if (instanceChecker.instanceOf(qualifiedName))
                expressionList.add(qualifiedName);
        } else if (expression instanceof PsiLiteralExpression) {
            PsiLiteralExpression typeLiteral = (PsiLiteralExpression) expression;
            if (instanceChecker.instanceOf(typeLiteral))
                expressionList.add(typeLiteral);
        } else if (expression instanceof PsiPolyadicExpression) {
            PsiPolyadicExpression polyadicExpression = (PsiPolyadicExpression) expression;
            PsiExpression[] psiExpressions = polyadicExpression.getOperands();
            for (PsiExpression psiExpression : psiExpressions) {
                expressionList.addAll(getExpressions(psiExpression));
            }
            if (instanceChecker.instanceOf(polyadicExpression))
                expressionList.add(polyadicExpression);
        } else if (expression instanceof PsiLambdaExpression) {
            PsiLambdaExpression lambdaExpression = (PsiLambdaExpression) expression;
            if (lambdaExpression.getBody() instanceof PsiExpression) {
                expressionList.addAll(getExpressions(lambdaExpression.getBody()));
            } else if (lambdaExpression.getBody() instanceof PsiCodeBlock) {
                expressionList.addAll(getExpressions(lambdaExpression.getBody()));
            }
            if (instanceChecker.instanceOf(lambdaExpression))
                expressionList.add(lambdaExpression);
        }
        return expressionList;
    }

    private List<PsiExpression> getExpressions(PsiAnonymousClass anonymousClassDeclaration) {
        List<PsiExpression> expressionList = new ArrayList<>();
        PsiMethod[] bodyDeclarations = anonymousClassDeclaration.getMethods();
        for (PsiMethod bodyDeclaration : bodyDeclarations) {
            if (bodyDeclaration != null) {
                PsiCodeBlock body = bodyDeclaration.getBody();
                if (body != null) {
                    PsiStatement[] statements = body.getStatements();
                    for (PsiStatement statement : statements) {
                        expressionList.addAll(getExpressions(statement));
                    }
                }
            }
        }
        return expressionList;
    }
}