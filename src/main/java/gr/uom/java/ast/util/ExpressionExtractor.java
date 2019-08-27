package gr.uom.java.ast.util;

import java.util.*;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

public class ExpressionExtractor {
    ExpressionInstanceChecker instanceChecker;

    // returns a List of SimpleName objects
    public List<PsiExpression> getVariableInstructions(PsiStatement statement) {
        instanceChecker = new InstanceOfSimpleName();
        return getExpressions(statement);
    }

    // returns a List of SimpleName objects
    public List<PsiExpression> getVariableInstructions(PsiExpression expression) {
        instanceChecker = new InstanceOfSimpleName();
        return getExpressions(expression);
    }

    // returns a List of MethodInvocation and SuperMethodInvocation objects
    public List<PsiExpression> getMethodInvocations(PsiStatement statement) {
        instanceChecker = new InstanceOfMethodInvocation();
        return getExpressions(statement);
    }

    // returns a List of SuperMethodInvocation objects
    public List<PsiExpression> getSuperMethodInvocations(PsiStatement statement) {
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
    public List<PsiExpression> getSuperFieldAccesses(PsiStatement statement) {
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

    // returns a List of InstanceofExpression objects
    public List<PsiExpression> getInstanceofExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfInstanceofExpression();
        return getExpressions(statement);
    }

    // returns a List of InstanceofExpression objects
    public List<PsiExpression> getInstanceofExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfInstanceofExpression();
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
    public List<PsiExpression> getPrefixExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfPrefixExpression();
        return getExpressions(expression);
    }

    // returns a List of VariableDeclarationExpression objects
    public List<PsiExpression> getVariableDeclarationExpressions(PsiStatement statement) {
        instanceChecker = new InstanceOfVariableDeclarationExpression();
        return getExpressions(statement);
    }

    // returns a List of VariableDeclarationExpression objects
    public List<PsiExpression> getVariableDeclarationExpressions(PsiExpression expression) {
        instanceChecker = new InstanceOfVariableDeclarationExpression();
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

    private List<PsiExpression> getExpressions(PsiStatement statement) {
        List<PsiExpression> expressionList = new ArrayList<PsiExpression>();
        if (statement instanceof PsiCodeBlock) {
            PsiCodeBlock block = (PsiCodeBlock) statement;
            List<PsiStatement> blockStatements = Arrays.asList(block.getStatements());
            for (PsiStatement blockStatement : blockStatements)
                expressionList.addAll(getExpressions(blockStatement));
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            PsiExpression expression = ifStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(ifStatement.getThenBranch()));
            if (ifStatement.getElseBranch() != null) {
                expressionList.addAll(getExpressions(ifStatement.getElseBranch()));
            }
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            PsiStatement initializers = forStatement.getInitialization();
            expressionList.addAll(getExpressions(initializers));
            PsiExpression expression = forStatement.getCondition();
            if (expression != null)
                expressionList.addAll(getExpressions(expression));
            PsiStatement updaters = forStatement.getUpdate();
            expressionList.addAll(getExpressions(updaters));
            expressionList.addAll(getExpressions(forStatement.getBody()));
        }
		else if(statement instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement)statement;
			PsiExpression expression = enhancedForStatement.getIteratedValue();
			expressionList.addAll(getExpressions(expression));
			expressionList.addAll(getExpressions(enhancedForStatement.getBody()));
		}
        else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            PsiExpression expression = whileStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(whileStatement.getBody()));
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            PsiExpression expression = doStatement.getCondition();
            expressionList.addAll(getExpressions(expression));
            expressionList.addAll(getExpressions(doStatement.getBody()));
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            expressionList.addAll(getExpressions(expression));
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            PsiExpression expression = switchStatement.getExpression();
            expressionList.addAll(getExpressions(expression));
            List<PsiStatement> switchStatements = Arrays.asList(switchStatement.getBody().getStatements());
            for (PsiStatement switchStatement2 : switchStatements)
                expressionList.addAll(getExpressions(switchStatement2));
        }
        else if (statement instanceof PsiAssertStatement) {
            PsiAssertStatement assertStatement = (PsiAssertStatement) statement;
            PsiExpression expression = assertStatement.getAssertCondition();
            expressionList.addAll(getExpressions(expression));
            PsiExpression message = assertStatement.getAssertDescription();
            if (message != null)
                expressionList.addAll(getExpressions(message));
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            // expressionList.addAll(getExpressions(labeledStatement.getLabelIdentifier()));
            expressionList.addAll(getExpressions(labeledStatement.getStatement()));
        } else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            PsiExpression expression = returnStatement.getReturnValue();
            expressionList.addAll(getExpressions(expression));
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            PsiExpression expression = synchronizedStatement.getLockExpression();
            expressionList.addAll(getExpressions(expression));
            List<PsiStatement> statements = Arrays.asList(synchronizedStatement.getBody().getStatements());
            statements.forEach(s -> expressionList.addAll(getExpressions(s)));
        } else if (statement instanceof PsiThrowStatement) {
            PsiThrowStatement throwStatement = (PsiThrowStatement) statement;
            PsiExpression expression = throwStatement.getException();
            expressionList.addAll(getExpressions(expression));
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            List<PsiStatement> tryStatements = Arrays.asList(tryStatement.getTryBlock().getStatements());
            tryStatements.forEach(s -> expressionList.addAll(getExpressions(s)));
            List<PsiCodeBlock> catchClauses = Arrays.asList(tryStatement.getCatchBlocks());
            for (PsiCodeBlock catchClause : catchClauses) {
                List<PsiStatement> statements = Arrays.asList(catchClause.getStatements());
                statements.forEach(s -> expressionList.addAll(getExpressions(s)));
            }
            if (tryStatement.getFinallyBlock() != null) {
                List<PsiStatement> finallyBlock = Arrays.asList(tryStatement.getFinallyBlock().getStatements());
                finallyBlock.forEach(f -> expressionList.addAll(getExpressions(f)));
            }
        } else if (statement instanceof PsiConstructorCall) {
            PsiConstructorCall constructorInvocation = (PsiConstructorCall) statement;
            List<PsiExpression> arguments = Arrays.asList(constructorInvocation.getArgumentList().getExpressions());
            for (PsiExpression argument : arguments)
                expressionList.addAll(getExpressions(argument));
        } else if (statement instanceof PsiSuperExpression) {
            PsiSuperExpression superInvocation = (PsiSuperExpression) statement;
            expressionList.add(superInvocation);
        }
        else if (statement instanceof PsiBreakStatement) {
            PsiBreakStatement breakStatement = (PsiBreakStatement) statement;
            if (breakStatement.getLabelExpression() != null)
                expressionList.addAll(getExpressions(breakStatement.getLabelExpression()));
        } else if (statement instanceof PsiContinueStatement) {
            PsiContinueStatement continueStatement = (PsiContinueStatement) statement;
            expressionList.addAll(getExpressions(continueStatement));
        }

        return expressionList;
    }

    private List<PsiExpression> getExpressions(PsiExpression expression) {
        List<PsiExpression> expressionList = new ArrayList<>();
        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
            expressionList.addAll(getExpressions(methodInvocation.getMethodExpression()));
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
            if (classInstanceCreation.getClassReference() != null)
                expressionList.add(classInstanceCreation);
            List<PsiExpression> arguments = Arrays.asList(classInstanceCreation.getArgumentList().getExpressions());
            for (PsiExpression argument : arguments)
                expressionList.addAll(getExpressions(argument));
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
            List<PsiExpression> extendedOperands = Arrays.asList(infixExpression.getOperands());
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
        } else if (expression instanceof PsiArrayAccessExpression) {
            PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression;
            expressionList.addAll(getExpressions(arrayAccess.getArrayExpression()));
            expressionList.addAll(getExpressions(arrayAccess.getIndexExpression()));
            if (instanceChecker.instanceOf(arrayAccess))
                expressionList.add(arrayAccess);
        } else if (expression instanceof PsiArrayInitializerExpression) {
            PsiArrayInitializerExpression arrayInitializer = (PsiArrayInitializerExpression) expression;
            List<PsiExpression> expressions = Arrays.asList(arrayInitializer.getInitializers());
            for (PsiExpression arrayInitializerExpression : expressions)
                expressionList.addAll(getExpressions(arrayInitializerExpression));
            if (instanceChecker.instanceOf(arrayInitializer))
                expressionList.add(arrayInitializer);
        } else if (expression instanceof PsiReferenceExpression) {
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            if (instanceChecker.instanceOf(simpleName))
                expressionList.add(simpleName);
        } else if (expression instanceof PsiQualifiedExpression) {
            PsiQualifiedExpression qualifiedName = (PsiQualifiedExpression) expression;
            expressionList.addAll(getExpressions(qualifiedName));
            if (instanceChecker.instanceOf(qualifiedName))
                expressionList.add(qualifiedName);
        } else if (expression instanceof PsiLiteralExpression) {
            PsiLiteralExpression typeLiteral = (PsiLiteralExpression) expression;
            if (instanceChecker.instanceOf(typeLiteral))
                expressionList.add(typeLiteral);
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
                    List<PsiStatement> statements = Arrays.asList(body.getStatements());
                    for (PsiStatement statement : statements) {
                        expressionList.addAll(getExpressions(statement));
                    }
                }
            }
        }
        return expressionList;
    }
}
