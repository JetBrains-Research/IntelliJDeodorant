package core.ast.decomposition;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.AnonymousClassDeclarationObject;
import core.ast.ConstructorInvocationObject;
import core.ast.CreationObject;
import core.ast.FieldInstructionObject;
import core.ast.LiteralObject;
import core.ast.LocalVariableDeclarationObject;
import core.ast.LocalVariableInstructionObject;
import core.ast.MethodInvocationObject;
import core.ast.SuperFieldInstructionObject;
import core.ast.SuperMethodInvocationObject;
import core.ast.decomposition.cfg.AbstractVariable;
import core.ast.util.StatementExtractor;
import refactoring.TypeCheckElimination;

import java.util.*;

public class MethodBodyObject {

    private final CompositeStatementObject compositeStatement;

    public MethodBodyObject(PsiCodeBlock methodBody) {
        this.compositeStatement = new CompositeStatementObject(methodBody, StatementType.BLOCK, null);
        PsiStatement[] statements = methodBody.getStatements();
        for (PsiStatement statement : statements) {
            processStatement(compositeStatement, statement);
        }
    }

    public CompositeStatementObject getCompositeStatement() {
        return compositeStatement;
    }

    public List<TypeCheckElimination> generateTypeCheckEliminations() {
        List<TypeCheckElimination> typeCheckEliminations = new ArrayList<>();
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiStatement> switchStatements = statementExtractor.getSwitchStatements(compositeStatement.getStatement());
        List<CompositeStatementObject> switchCompositeStatements = compositeStatement.getSwitchStatements();
        for (PsiStatement statement : switchStatements) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
            typeCheckElimination.setTypeCheckCodeFragment(switchStatement);
            PsiStatement[] statements = switchStatement.getBody().getStatements();
            boolean isDefaultCase = false;
            Set<PsiExpression> switchCaseExpressions = new LinkedHashSet<>();
            for (PsiStatement statement2 : statements) {
                if (statement2 instanceof PsiSwitchLabelStatement) {
                    PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) statement2;
                    isDefaultCase = switchCase.isDefaultCase();
                    if (!isDefaultCase) {
                        switchCaseExpressions.addAll(Arrays.asList(switchCase.getCaseValues().getExpressions()));
                    }
                } else {
                    if (statement2 instanceof PsiBlockStatement) {
                        PsiBlockStatement block = (PsiBlockStatement) statement2;
                        PsiStatement[] statementsInBlock = block.getCodeBlock().getStatements();
                        for (PsiStatement statementInBlock : statementsInBlock) {
                            if (!(statementInBlock instanceof PsiBreakStatement)) {
                                for (PsiExpression expression : switchCaseExpressions) {
                                    typeCheckElimination.addTypeCheck(expression, statementInBlock);
                                }
                                if (isDefaultCase) {
                                    typeCheckElimination.addDefaultCaseStatement(statementInBlock);
                                }
                            }
                        }
                        List<PsiStatement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
                        if (branchingStatements.size() > 0) {
                            for (PsiExpression expression : switchCaseExpressions) {
                                if (!typeCheckElimination.containsTypeCheckExpression(expression)) {
                                    typeCheckElimination.addEmptyTypeCheck(expression);
                                }
                            }
                            switchCaseExpressions.clear();
                        }
                    } else {
                        if (!(statement2 instanceof PsiBreakStatement)) {
                            for (PsiExpression expression : switchCaseExpressions) {
                                typeCheckElimination.addTypeCheck(expression, statement2);
                            }
                            if (isDefaultCase) {
                                typeCheckElimination.addDefaultCaseStatement(statement2);
                            }
                        }
                        List<PsiStatement> branchingStatements = statementExtractor.getBranchingStatements(statement2);
                        if (statement2 instanceof PsiBreakStatement || statement2 instanceof PsiReturnStatement || branchingStatements.size() > 0) {
                            for (PsiExpression expression : switchCaseExpressions) {
                                if (!typeCheckElimination.containsTypeCheckExpression(expression))
                                    typeCheckElimination.addEmptyTypeCheck(expression);
                            }
                            switchCaseExpressions.clear();
                        }
                    }
                }
            }
            typeCheckEliminations.add(typeCheckElimination);
            for (CompositeStatementObject composite : switchCompositeStatements) {
                if (composite.getStatement().toString().equals(switchStatement.toString())) { // TODO: not sure if it will work
                    typeCheckElimination.setTypeCheckCompositeStatement(composite);
                    break;
                }
            }
        }

        List<PsiStatement> ifStatements = statementExtractor.getIfStatements(compositeStatement.getStatement());
        List<CompositeStatementObject> ifCompositeStatements = compositeStatement.getIfStatements();
        TypeCheckElimination typeCheckElimination = new TypeCheckElimination();
        int i = 0;
        for (PsiStatement statement : ifStatements) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            PsiExpression ifExpression = ifStatement.getCondition();
            PsiStatement thenStatement = ifStatement.getThenBranch();
            if (thenStatement instanceof PsiBlockStatement) {
                PsiBlockStatement block = (PsiBlockStatement) thenStatement;
                PsiStatement[] statements = block.getCodeBlock().getStatements();
                for (PsiStatement statementInBlock : statements) {
                    typeCheckElimination.addTypeCheck(ifExpression, statementInBlock);
                }
            } else {
                typeCheckElimination.addTypeCheck(ifExpression, thenStatement);
            }
            PsiStatement elseStatement = ifStatement.getElseBranch();
            if (elseStatement != null) {
                if (elseStatement instanceof PsiBlockStatement) {
                    PsiBlockStatement block = (PsiBlockStatement) elseStatement;
                    PsiStatement[] statements = block.getCodeBlock().getStatements();
                    for (PsiStatement statementInBlock : statements) {
                        typeCheckElimination.addDefaultCaseStatement(statementInBlock);
                    }
                } else if (!(elseStatement instanceof PsiIfStatement)) {
                    typeCheckElimination.addDefaultCaseStatement(elseStatement);
                }
            }
            if (ifStatements.size() - 1 > i) { // TODO: what is happening here???
                PsiIfStatement nextIfStatement = (PsiIfStatement) ifStatements.get(i + 1);
                if (!ifStatement.getParent().equals(nextIfStatement)) {
                    typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
                    typeCheckEliminations.add(typeCheckElimination);
                    for (CompositeStatementObject composite : ifCompositeStatements) {
                        if (composite.getStatement().toString().equals(ifStatement.toString())) {
                            typeCheckElimination.setTypeCheckCompositeStatement(composite);
                            break;
                        }
                    }
                    typeCheckElimination = new TypeCheckElimination();
                }
            } else {
                typeCheckElimination.setTypeCheckCodeFragment(ifStatement);
                typeCheckEliminations.add(typeCheckElimination);
                for (CompositeStatementObject composite : ifCompositeStatements) {
                    if (composite.getStatement().toString().equals(ifStatement.toString())) {
                        typeCheckElimination.setTypeCheckCompositeStatement(composite);
                        break;
                    }
                }
            }
            i++;
        }
        return typeCheckEliminations;
    }

    public List<FieldInstructionObject> getFieldInstructions() {
        return compositeStatement.getFieldInstructions();
    }

    public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
        return compositeStatement.getSuperFieldInstructions();
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
        return compositeStatement.getLocalVariableDeclarations();
    }

    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
        return compositeStatement.getLocalVariableInstructions();
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        return compositeStatement.getMethodInvocations();
    }

    public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
        return compositeStatement.getSuperMethodInvocations();
    }

    public List<ConstructorInvocationObject> getConstructorInvocations() {
        return compositeStatement.getConstructorInvocations();
    }

    public List<CreationObject> getCreations() {
        return compositeStatement.getCreations();
    }

    public List<LiteralObject> getLiterals() {
        return compositeStatement.getLiterals();
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return compositeStatement.getAnonymousClassDeclarations();
    }

    public Set<String> getExceptionsInThrowStatements() {
        return compositeStatement.getExceptionsInThrowStatements();
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
        return compositeStatement.containsMethodInvocation(methodInvocation);
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
        return compositeStatement.containsFieldInstruction(fieldInstruction);
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        return compositeStatement.containsSuperMethodInvocation(superMethodInvocation);
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
        return compositeStatement.getInvokedMethodsThroughFields();
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
        return compositeStatement.getInvokedMethodsThroughParameters();
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields() {
        return compositeStatement.getNonDistinctInvokedMethodsThroughFields();
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters() {
        return compositeStatement.getNonDistinctInvokedMethodsThroughParameters();
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
        return compositeStatement.getInvokedMethodsThroughLocalVariables();
    }

    public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
        return compositeStatement.getInvokedMethodsThroughThisReference();
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference() {
        return compositeStatement.getNonDistinctInvokedMethodsThroughThisReference();
    }

    public Set<MethodInvocationObject> getInvokedStaticMethods() {
        return compositeStatement.getInvokedStaticMethods();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughFields() {
        return compositeStatement.getDefinedFieldsThroughFields();
    }

    public Set<AbstractVariable> getUsedFieldsThroughFields() {
        return compositeStatement.getUsedFieldsThroughFields();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
        return compositeStatement.getNonDistinctDefinedFieldsThroughFields();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
        return compositeStatement.getNonDistinctUsedFieldsThroughFields();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
        return compositeStatement.getDefinedFieldsThroughParameters();
    }

    public Set<AbstractVariable> getUsedFieldsThroughParameters() {
        return compositeStatement.getUsedFieldsThroughParameters();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
        return compositeStatement.getNonDistinctDefinedFieldsThroughParameters();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
        return compositeStatement.getNonDistinctUsedFieldsThroughParameters();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
        return compositeStatement.getDefinedFieldsThroughLocalVariables();
    }

    public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
        return compositeStatement.getUsedFieldsThroughLocalVariables();
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
        return compositeStatement.getDefinedFieldsThroughThisReference();
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference() {
        return compositeStatement.getNonDistinctDefinedFieldsThroughThisReference();
    }

    public Set<PlainVariable> getUsedFieldsThroughThisReference() {
        return compositeStatement.getUsedFieldsThroughThisReference();
    }

    public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference() {
        return compositeStatement.getNonDistinctUsedFieldsThroughThisReference();
    }

    public Set<PlainVariable> getDeclaredLocalVariables() {
        return compositeStatement.getDeclaredLocalVariables();
    }

    public Set<PlainVariable> getDefinedLocalVariables() {
        return compositeStatement.getDefinedLocalVariables();
    }

    public Set<PlainVariable> getUsedLocalVariables() {
        return compositeStatement.getUsedLocalVariables();
    }

    public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
        return compositeStatement.getParametersPassedAsArgumentsInMethodInvocations();
    }

    public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations() {
        return compositeStatement.getParametersPassedAsArgumentsInSuperMethodInvocations();
    }

    public Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> getParametersPassedAsArgumentsInConstructorInvocations() {
        return compositeStatement.getParametersPassedAsArgumentsInConstructorInvocations();
    }

    public boolean containsSuperMethodInvocation() {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(compositeStatement.getStatement());
        return !superMethodInvocations.isEmpty();
    }

    public boolean containsSuperFieldAccess() {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> superFieldAccesses = expressionExtractor.getSuperFieldAccesses(compositeStatement.getStatement());
        return !superFieldAccesses.isEmpty();
    }

    private void processStatement(CompositeStatementObject parent, PsiStatement statement) {
        if (statement instanceof PsiCodeBlock) {
            PsiCodeBlock codeBlock = (PsiCodeBlock) statement;
            PsiStatement[] statements = codeBlock.getStatements();
            CompositeStatementObject child = new CompositeStatementObject(codeBlock, StatementType.BLOCK, parent);
            parent.addStatement(child);
            for (PsiStatement psiStatement : statements) {
                processStatement(child, psiStatement);
            }
        } else if (statement instanceof PsiBlockStatement) {
            PsiBlockStatement blockStatement = (PsiBlockStatement) statement;
            PsiCodeBlock psiCodeBlock = blockStatement.getCodeBlock();
            PsiStatement[] blockStatements = psiCodeBlock.getStatements();
            CompositeStatementObject child = new CompositeStatementObject(blockStatement, StatementType.BLOCK, parent);
            parent.addStatement(child);
            for (PsiStatement psiStatement : blockStatements) {
                processStatement(child, psiStatement);
            }
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(ifStatement, StatementType.IF, parent);
            AbstractExpression abstractExpression = new AbstractExpression(ifStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            parent.addStatement(child);
            processStatement(child, ifStatement.getThenBranch());
            if (ifStatement.getElseBranch() != null) {
                processStatement(child, ifStatement.getElseBranch());
            }
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(forStatement, StatementType.FOR, parent);
            parent.addStatement(child);
            processStatement(child, forStatement.getBody());
            processStatement(child, forStatement.getInitialization());
            processStatement(child, forStatement.getUpdate());
        } else if (statement instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(enhancedForStatement, StatementType.ENHANCED_FOR, parent);
            PsiParameter variableDeclaration = enhancedForStatement.getIterationParameter();
            AbstractExpression variableDeclarationName = new AbstractExpression(variableDeclaration.getInitializer(), child);
            child.addExpression(variableDeclarationName);
            if (variableDeclaration.getInitializer() != null) {
                AbstractExpression variableDeclarationInitializer = new AbstractExpression(variableDeclaration.getInitializer(), child);
                child.addExpression(variableDeclarationInitializer);
            }
            AbstractExpression abstractExpression = new AbstractExpression(enhancedForStatement.getIteratedValue(), child);
            child.addExpression(abstractExpression);
            parent.addStatement(child);
            processStatement(child, enhancedForStatement.getBody());
        } else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(whileStatement, StatementType.WHILE, parent);
            AbstractExpression abstractExpression = new AbstractExpression(whileStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            parent.addStatement(child);
            processStatement(child, whileStatement.getBody());
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(doStatement, StatementType.DO, parent);
            AbstractExpression abstractExpression = new AbstractExpression(doStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            parent.addStatement(child);
            processStatement(child, doStatement.getBody());
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            StatementObject child = new StatementObject(expressionStatement, StatementType.EXPRESSION, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(switchStatement, StatementType.SWITCH, parent);
            AbstractExpression abstractExpression = new AbstractExpression(switchStatement.getExpression(), child);
            child.addExpression(abstractExpression);
            parent.addStatement(child);
            if (switchStatement.getBody() != null) {
                PsiStatement[] switchStatements = switchStatement.getBody().getStatements();
                for (PsiStatement psiStatement : switchStatements)
                    processStatement(child, psiStatement);
            }
        } else if (statement instanceof PsiAssertStatement) {
            PsiAssertStatement assertStatement = (PsiAssertStatement) statement;
            StatementObject child = new StatementObject(assertStatement, StatementType.ASSERT, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(labeledStatement, StatementType.LABELED, parent);
            parent.addStatement(child);
            processStatement(child, labeledStatement.getStatement());
        } else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            StatementObject child = new StatementObject(returnStatement, StatementType.RETURN, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            SynchronizedStatementObject child = new SynchronizedStatementObject(synchronizedStatement, parent);
            parent.addStatement(child);
            if (synchronizedStatement.getBody() != null) {
                PsiStatement[] statements = synchronizedStatement.getBody().getStatements();
                for (PsiStatement psiStatement : statements) {
                    processStatement(child, psiStatement);
                }
            }
        } else if (statement instanceof PsiThrowStatement) {
            PsiThrowStatement throwStatement = (PsiThrowStatement) statement;
            StatementObject child = new StatementObject(throwStatement, StatementType.THROW, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            TryStatementObject child = new TryStatementObject(tryStatement, parent);

            PsiResourceList resources = tryStatement.getResourceList();
            if (resources != null)
                for (PsiResourceListElement resource : resources) {
                    if (resource instanceof PsiResourceExpression) {
                        PsiResourceExpression expression = (PsiResourceExpression) resource;
                        AbstractExpression variableDeclarationExpression = new AbstractExpression(expression.getExpression(), child);
                        child.addExpression(variableDeclarationExpression);
                    }
                }
            parent.addStatement(child);

            if (tryStatement.getTryBlock() != null) {
                PsiStatement[] tryStatements = tryStatement.getTryBlock().getStatements();
                for (PsiStatement psiStatement : tryStatements) {
                    processStatement(child, psiStatement);
                }
            }

            PsiCatchSection[] catchSections = tryStatement.getCatchSections();
            for (PsiCatchSection section : catchSections) {
                CatchClauseObject catchClauseObject = new CatchClauseObject();
                PsiCodeBlock catchClauseBody = section.getCatchBlock();
                CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(catchClauseBody, StatementType.BLOCK, null);
                List<PsiType> caughtExceptionTypes = section.getPreciseCatchTypes();
                for (PsiType type : caughtExceptionTypes) {
                    if (type instanceof PsiDisjunctionType) {
                        PsiDisjunctionType disjunctionType = (PsiDisjunctionType) type;
                        List<PsiType> disjunctions = disjunctionType.getDisjunctions();
                        for (PsiType psiType : disjunctions) {
                            catchClauseObject.addExceptionType(psiType.getCanonicalText());
                        }
                    }
                    catchClauseObject.addExceptionType(type.getCanonicalText());
                }

                PsiParameter[] catchBlockParameters = tryStatement.getCatchBlockParameters();
                for (PsiParameter psiParameter : catchBlockParameters) {
                    if (psiParameter.getInitializer() != null) {
                        AbstractExpression variableDeclarationInitializer = new AbstractExpression(psiParameter.getInitializer(), child);
                        catchClauseObject.addExpression(variableDeclarationInitializer);
                    }
                }

                if (catchClauseBody != null) {
                    PsiStatement[] blockStatements = catchClauseBody.getStatements();
                    for (PsiStatement blockStatement : blockStatements) {
                        processStatement(catchClauseStatementObject, blockStatement);
                    }
                }
                catchClauseObject.setBody(catchClauseStatementObject);
                child.addCatchClause(catchClauseObject);
            }

            PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(finallyBlock, StatementType.BLOCK, null);
                PsiStatement[] blockStatements = finallyBlock.getStatements();
                for (PsiStatement blockStatement : blockStatements) {
                    processStatement(finallyClauseStatementObject, blockStatement);
                }
                child.setFinallyClause(finallyClauseStatementObject);
            }
        } else if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
            StatementObject child = new StatementObject(variableDeclarationStatement, StatementType.VARIABLE_DECLARATION, parent);
            parent.addStatement(child);
            Collection<PsiMethodCallExpression> methodCallExpressions =
                    PsiTreeUtil.findChildrenOfType(variableDeclarationStatement, PsiMethodCallExpression.class);
            for (PsiMethodCallExpression elem : methodCallExpressions) {
                AbstractExpression abstractExpression = new AbstractExpression(elem);
                parent.addExpression(abstractExpression);
            }
        } else if (statement instanceof PsiBreakStatement) {
            PsiBreakStatement breakStatement = (PsiBreakStatement) statement;
            StatementObject child = new StatementObject(breakStatement, StatementType.BREAK, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiContinueStatement) {
            PsiContinueStatement continueStatement = (PsiContinueStatement) statement;
            StatementObject child = new StatementObject(continueStatement, StatementType.CONTINUE, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiEmptyStatement) {
            PsiEmptyStatement emptyStatement = (PsiEmptyStatement) statement;
            StatementObject child = new StatementObject(emptyStatement, StatementType.EMPTY, parent);
            parent.addStatement(child);
        } else if (statement instanceof PsiSwitchLabelStatement) {
            PsiSwitchLabelStatement switchStatement = (PsiSwitchLabelStatement) statement;
            StatementObject child = new StatementObject(switchStatement, StatementType.SWITCH_CASE, parent);
            parent.addStatement(child);
        }
    }

    public List<TryStatementObject> getTryStatements() {
        return compositeStatement.getTryStatements();
    }

    public List<String> stringRepresentation() {
        return compositeStatement.stringRepresentation();
    }
}
