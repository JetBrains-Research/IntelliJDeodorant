package org.jetbrains.research.intellijdeodorant.core.ast.util;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatementExtractor {

    public StatementExtractor() {

    }

    private StatementInstanceChecker instanceChecker;

    public List<PsiStatement> getConstructorInvocations(PsiStatement statement) {
        instanceChecker = new InstanceOfConstructorInvocation();
        return getStatements(statement);
    }

    public List<PsiStatement> getVariableDeclarationStatements(PsiElement statement) {
        instanceChecker = new InstanceOfVariableDeclarationStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getVariableDeclarationStatements(PsiStatement[] statement) {
        instanceChecker = new InstanceOfVariableDeclarationStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getBranchingStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfBranchingStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getTryStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfTryStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getSwitchStatements(PsiElement statement) {
        instanceChecker = new InstanceOfSwitchStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getIfStatements(PsiElement statement) {
        instanceChecker = new InstanceOfIfStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getReturnStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfReturnStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getReturnStatements(PsiStatement[] statement) {
        instanceChecker = new InstanceOfReturnStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getBreakStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfBreakStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getContinueStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfContinueStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getEnhancedForStatements(PsiElement statement) {
        instanceChecker = new InstanceOfEnhancedForStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getForStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfForStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getWhileStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfWhileStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getDoStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfDoStatement();
        return getStatements(statement);
    }

    public List<PsiStatement> getTypeDeclarationStatements(PsiStatement statement) {
        instanceChecker = new InstanceOfTypeDeclarationStatement();
        return getStatements(statement);
    }

    private List<PsiStatement> getStatements(PsiStatement[] statements) {
        List<PsiStatement> result = new ArrayList<>();
        for (PsiStatement statement : statements) {
            result.addAll(getStatements(statement));
        }

        return result;
    }

    private List<PsiStatement> getStatements(PsiElement statement) {
        List<PsiStatement> statementList = new ArrayList<>();
        if (statement instanceof PsiCodeBlock) {
            PsiCodeBlock block = (PsiCodeBlock) statement;
            PsiStatement[] statements = block.getStatements();
            for (PsiStatement psiStatement : statements) {
                statementList.addAll(getStatements(psiStatement));
            }
        } else if (statement instanceof PsiBlockStatement) {
            PsiBlockStatement block = (PsiBlockStatement) statement;
            PsiStatement[] statements = block.getCodeBlock().getStatements();
            for (PsiStatement psiStatement : statements) {
                statementList.addAll(getStatements(psiStatement));
            }
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            statementList.addAll(getStatements(ifStatement.getThenBranch()));
            if (ifStatement.getElseBranch() != null) {
                statementList.addAll(getStatements(ifStatement.getElseBranch()));
            }
            if (instanceChecker.instanceOf(ifStatement))
                statementList.add(ifStatement);
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            statementList.addAll(getStatements(forStatement.getBody()));
            if (instanceChecker.instanceOf(forStatement))
                statementList.add(forStatement);
        } else if (statement instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) statement;
            statementList.addAll(getStatements(enhancedForStatement.getBody()));
            if (instanceChecker.instanceOf(enhancedForStatement))
                statementList.add(enhancedForStatement);
        } else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            statementList.addAll(getStatements(whileStatement.getBody()));
            if (instanceChecker.instanceOf(whileStatement))
                statementList.add(whileStatement);
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            statementList.addAll(getStatements(doStatement.getBody()));
            if (instanceChecker.instanceOf(doStatement))
                statementList.add(doStatement);
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            if (switchStatement.getBody() != null) {
                PsiStatement[] statements = switchStatement.getBody().getStatements();
                for (PsiStatement psiStatement : statements)
                    statementList.addAll(getStatements(psiStatement));
            }
            if (instanceChecker.instanceOf(switchStatement))
                statementList.add(switchStatement);
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            statementList.addAll(getStatements(labeledStatement.getStatement()));
        } else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            if (instanceChecker.instanceOf(returnStatement))
                statementList.add(returnStatement);
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            statementList.addAll(getStatements(synchronizedStatement.getBody()));
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            PsiCodeBlock tryBlock = tryStatement.getTryBlock();
            if (tryBlock != null) {
                PsiStatement[] tryStatements = tryBlock.getStatements();
                for (PsiStatement psiTryStatement : tryStatements) {
                    statementList.addAll(getStatements(psiTryStatement));
                }
            }
            PsiCodeBlock[] catchClauses = tryStatement.getCatchBlocks();
            for (PsiCodeBlock catchClause : catchClauses) {
                Arrays.asList(catchClause.getStatements()).forEach(s -> statementList.addAll(getStatements(s)));
            }
            PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null)
                Arrays.asList(finallyBlock.getStatements()).forEach(s -> statementList.addAll(getStatements(s)));
            if (instanceChecker.instanceOf(tryStatement))
                statementList.add(tryStatement);
        } else if (statement instanceof PsiBreakStatement) {
            PsiBreakStatement breakStatement = (PsiBreakStatement) statement;
            if (instanceChecker.instanceOf(breakStatement))
                statementList.add(breakStatement);
        } else if (statement instanceof PsiContinueStatement) {
            PsiContinueStatement continueStatement = (PsiContinueStatement) statement;
            if (instanceChecker.instanceOf(continueStatement))
                statementList.add(continueStatement);
        } else if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement typeDeclarationStatement = (PsiDeclarationStatement) statement;
            if (instanceChecker.instanceOf(typeDeclarationStatement))
                statementList.add(typeDeclarationStatement);
        }
        return statementList;
    }

    public int getTotalNumberOfStatements(PsiStatement statement) {
        int statementCounter = 0;
        if (statement instanceof PsiBlockStatement) {
            PsiBlockStatement block = (PsiBlockStatement) statement;
            PsiStatement[] statements = block.getCodeBlock().getStatements();
            for (PsiStatement psiStatement : statements) {
                statementCounter += getTotalNumberOfStatements(psiStatement);
            }
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(ifStatement.getThenBranch());
            if (ifStatement.getElseBranch() != null) {
                statementCounter += getTotalNumberOfStatements(ifStatement.getElseBranch());
            }
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(forStatement.getBody());
        } else if (statement instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(enhancedForStatement.getBody());
        } else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(whileStatement.getBody());
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(doStatement.getBody());
        } else if (statement instanceof PsiExpressionStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            statementCounter += getNumberOfStatementsInsideCodeBlock(switchStatement.getBody());
            statementCounter += 1;
        } else if (statement instanceof PsiAssertStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            statementCounter += 1;
            statementCounter += getTotalNumberOfStatements(labeledStatement.getStatement());
        } else if (statement instanceof PsiReturnStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            statementCounter += 1;
            statementCounter += getNumberOfStatementsInsideCodeBlock(synchronizedStatement.getBody());
        } else if (statement instanceof PsiThrowStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            statementCounter += 1;
            statementCounter += getNumberOfStatementsInsideCodeBlock(tryStatement.getTryBlock());
            PsiCodeBlock[] catchClauses = tryStatement.getCatchBlocks();
            for (PsiCodeBlock catchClause : catchClauses) {
                statementCounter += getNumberOfStatementsInsideCodeBlock(catchClause);
            }
            statementCounter += getNumberOfStatementsInsideCodeBlock(tryStatement.getFinallyBlock());
        } else if (statement instanceof PsiDeclarationStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiBreakStatement) {
            statementCounter += 1;
        } else if (statement instanceof PsiContinueStatement) {
            statementCounter += 1;
        }
        return statementCounter;
    }

    private int getNumberOfStatementsInsideCodeBlock(PsiCodeBlock block) {
        int statementCounter = 0;
        if (block != null) {
            for (PsiStatement psiStatement : block.getStatements()) {
                statementCounter += getTotalNumberOfStatements(psiStatement);
            }
        }
        return statementCounter;
    }
}
