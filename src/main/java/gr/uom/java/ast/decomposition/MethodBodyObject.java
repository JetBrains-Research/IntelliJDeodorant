package gr.uom.java.ast.decomposition;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import gr.uom.java.ast.AnonymousClassDeclarationObject;
import gr.uom.java.ast.ConstructorInvocationObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.*;

public class MethodBodyObject {

    private CompositeStatementObject compositeStatement;

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

    public Set<PsiExpression> getExceptionsInThrowStatements() {
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
        if (!superFieldAccesses.isEmpty())
            return true;
        else
            return false;
    }

    private void processStatement(CompositeStatementObject parent, PsiStatement statement) {
        if (statement instanceof PsiCodeBlock) {
            PsiCodeBlock block = (PsiCodeBlock) statement;
            List<PsiStatement> blockStatements = Arrays.asList(block.getStatements());
            CompositeStatementObject child = new CompositeStatementObject(block.getStatements()[0], StatementType.BLOCK, parent);
            parent.addStatement(child);
            for (PsiStatement blockStatement : blockStatements) {
                processStatement(child, blockStatement);
            }
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(ifStatement, StatementType.IF, parent);
            AbstractExpression abstractExpression = new AbstractExpression(ifStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            //processExpression(child, ifStatement.getExpression());
            parent.addStatement(child);
            processStatement(child, ifStatement.getThenBranch());
            if (ifStatement.getElseBranch() != null) {
                processStatement(child, ifStatement.getElseBranch());
            }
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(forStatement, StatementType.FOR, parent);
            PsiExpression initializers = forStatement.getCondition();
            //for (PsiStatement initializer : initializers) {
            AbstractExpression abstractExpression0 = new AbstractExpression(initializers, child);
            child.addExpression(abstractExpression0);
            //processExpression(child, initializer);
            //}
            PsiExpression expression = forStatement.getCondition();
            if (expression != null) {
                AbstractExpression abstractExpression = new AbstractExpression(expression, child);
                child.addExpression(abstractExpression);
                //processExpression(child, expression);
            }
            PsiExpression updaters = forStatement.getCondition();

            AbstractExpression abstractExpression = new AbstractExpression(updaters, child);
            child.addExpression(abstractExpression);
            //processExpression(child, updater);

            parent.addStatement(child);
            processStatement(child, forStatement.getBody());
        } else if (statement instanceof PsiForeachStatement) {
            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(enhancedForStatement, StatementType.ENHANCED_FOR, parent);
            PsiParameter variableDeclaration = enhancedForStatement.getIterationParameter();
            AbstractExpression variableDeclarationName = new AbstractExpression(variableDeclaration.getInitializer(), child);
            child.addExpression(variableDeclarationName);
            //processExpression(child, variableDeclaration.getName());
            if (variableDeclaration.getInitializer() != null) {
                AbstractExpression variableDeclarationInitializer = new AbstractExpression(variableDeclaration.getInitializer(), child);
                child.addExpression(variableDeclarationInitializer);
                //processExpression(child, variableDeclaration.getInitializer());
            }
            AbstractExpression abstractExpression = new AbstractExpression(enhancedForStatement.getIteratedValue(), child);
            child.addExpression(abstractExpression);
            //processExpression(child, enhancedForStatement.getExpression());
            parent.addStatement(child);
            processStatement(child, enhancedForStatement.getBody());
        } else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(whileStatement, StatementType.WHILE, parent);
            AbstractExpression abstractExpression = new AbstractExpression(whileStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            //processExpression(child, whileStatement.getExpression());
            parent.addStatement(child);
            processStatement(child, whileStatement.getBody());
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(doStatement, StatementType.DO, parent);
            AbstractExpression abstractExpression = new AbstractExpression(doStatement.getCondition(), child);
            child.addExpression(abstractExpression);
            //processExpression(child, doStatement.getExpression());
            parent.addStatement(child);
            processStatement(child, doStatement.getBody());
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            StatementObject child = new StatementObject(expressionStatement, StatementType.EXPRESSION, parent);
            //processExpression(child, expressionStatement.getExpression());
            parent.addStatement(child);
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(switchStatement, StatementType.SWITCH, parent);
            AbstractExpression abstractExpression = new AbstractExpression(switchStatement.getExpression(), child);
            child.addExpression(abstractExpression);
            //processExpression(child, switchStatement.getExpression());
            parent.addStatement(child);
            List<PsiStatement> switchStatements = Arrays.asList(switchStatement.getBody().getStatements());
            for (PsiStatement switchStatement2 : switchStatements)
                processStatement(child, switchStatement2);
        }/* else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchCase = (PsiSwitchStatement) statement;
            StatementObject child = new StatementObject(switchCase, StatementType.SWITCH_CASE, parent);
			*//*if(switchCase.getExpression() != null)
				processExpression(child, switchCase.getExpression());*//*
            parent.addStatement(child);
        } */ else if (statement instanceof PsiAssertStatement) {
            PsiAssertStatement assertStatement = (PsiAssertStatement) statement;
            StatementObject child = new StatementObject(assertStatement, StatementType.ASSERT, parent);
			/*processExpression(child, assertStatement.getExpression());
			Expression message = assertStatement.getMessage();
			if(message != null)
				processExpression(child, message);*/
            parent.addStatement(child);
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(labeledStatement, StatementType.LABELED, parent);
			/*if(labeledStatement.getLabel() != null)
				processExpression(child, labeledStatement.getLabel());*/
            parent.addStatement(child);
            processStatement(child, labeledStatement.getStatement());
        } else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            StatementObject child = new StatementObject(returnStatement, StatementType.RETURN, parent);
            //processExpression(child, returnStatement.getExpression());
            parent.addStatement(child);
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            SynchronizedStatementObject child = new SynchronizedStatementObject(synchronizedStatement, parent);
            //processExpression(child, synchronizedStatement.getExpression());
            parent.addStatement(child);
            if (synchronizedStatement.getBody() != null) {
                PsiStatement[] statements = synchronizedStatement.getBody().getStatements();
                for (PsiStatement statement1 : statements) {
                    processStatement(child, statement1);
                }
            }
        } else if (statement instanceof PsiThrowStatement) {
            PsiThrowStatement throwStatement = (PsiThrowStatement) statement;
            StatementObject child = new StatementObject(throwStatement, StatementType.THROW, parent);
            //processExpression(child, throwStatement.getExpression());
            parent.addStatement(child);
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            TryStatementObject child = new TryStatementObject(tryStatement, parent);
            parent.addStatement(child);
           // processStatement(child, tryStatement);
            List<PsiCodeBlock> catchClauses = Arrays.asList(tryStatement.getCatchBlocks());
            for (PsiCodeBlock catchClause : catchClauses) {
                CatchClauseObject catchClauseObject = new CatchClauseObject();
                PsiStatement[] catchClauseBody = catchClause.getStatements();
                for (PsiStatement psiStatement : catchClauseBody) {
                    CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(psiStatement, StatementType.BLOCK, null);
                }
            }
            PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(finallyBlock.getStatements()[0], StatementType.BLOCK, null);
                List<PsiStatement> blockStatements = Arrays.asList(finallyBlock.getStatements());
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
        }
    }

    public List<TryStatementObject> getTryStatements() {
        return compositeStatement.getTryStatements();
    }

    public List<String> stringRepresentation() {
        return compositeStatement.stringRepresentation();
    }
}
