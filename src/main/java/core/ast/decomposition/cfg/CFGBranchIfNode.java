package core.ast.decomposition.cfg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiLabeledStatement;
import com.intellij.psi.PsiSynchronizedStatement;
import com.intellij.psi.PsiTryStatement;
import core.ast.decomposition.AbstractStatement;
import core.ast.decomposition.CompositeStatementObject;

class CFGBranchIfNode extends CFGBranchConditionalNode {

    CFGBranchIfNode(AbstractStatement statement) {
        super(statement);
    }

    Set<CFGNode> getImmediatelyNestedNodesInTrueControlFlow() {
        Set<CFGNode> nestedNodes = new LinkedHashSet<>();
        AbstractStatement abstractStatement = getStatement();
        if (abstractStatement instanceof CompositeStatementObject) {
            Set<AbstractStatement> nestedStatements = new LinkedHashSet<>();
            CompositeStatementObject composite = (CompositeStatementObject) abstractStatement;
            List<AbstractStatement> statements = composite.getStatements();
            AbstractStatement trueControlFlowStatement = statements.get(0);
            if (trueControlFlowStatement.getStatement() instanceof PsiBlockStatement) {
                CompositeStatementObject blockStatement = (CompositeStatementObject) trueControlFlowStatement;
                processBlockStatement(nestedStatements, blockStatement);
            } else if (trueControlFlowStatement.getStatement() instanceof PsiLabeledStatement
                    || trueControlFlowStatement.getStatement() instanceof PsiSynchronizedStatement) {
                CompositeStatementObject labeledStatement = (CompositeStatementObject) trueControlFlowStatement;
                processLabeledStatement(nestedStatements, labeledStatement);
            } else if (trueControlFlowStatement instanceof PsiTryStatement) {
                CompositeStatementObject tryStatement = (CompositeStatementObject) trueControlFlowStatement;
                processTryStatement(nestedStatements, tryStatement);
            } else
                nestedStatements.add(trueControlFlowStatement);
            List<BasicBlock> nestedBasicBlocks = getNestedBasicBlocks();
            nestedBasicBlocks.add(0, getBasicBlock());
            for (BasicBlock nestedBlock : nestedBasicBlocks) {
                List<CFGNode> nodes = nestedBlock.getAllNodes();
                for (CFGNode node : nodes) {
                    if (nestedStatements.contains(node.getStatement())) {
                        nestedNodes.add(node);
                    }
                }
            }
        }
        return nestedNodes;
    }

    Set<CFGNode> getImmediatelyNestedNodesInFalseControlFlow() {
        Set<CFGNode> nestedNodes = new LinkedHashSet<>();
        AbstractStatement abstractStatement = getStatement();
        if (abstractStatement instanceof CompositeStatementObject) {
            Set<AbstractStatement> nestedStatements = new LinkedHashSet<>();
            CompositeStatementObject composite = (CompositeStatementObject) abstractStatement;
            List<AbstractStatement> statements = composite.getStatements();
            if (statements.size() == 2) {
                AbstractStatement falseControlFlowStatement = statements.get(1);
                if (falseControlFlowStatement.getStatement() instanceof PsiBlockStatement) {
                    CompositeStatementObject blockStatement = (CompositeStatementObject) falseControlFlowStatement;
                    processBlockStatement(nestedStatements, blockStatement);
                } else if (falseControlFlowStatement.getStatement() instanceof PsiLabeledStatement
                        || falseControlFlowStatement.getStatement() instanceof PsiSynchronizedStatement) {
                    CompositeStatementObject labeledStatement = (CompositeStatementObject) falseControlFlowStatement;
                    processLabeledStatement(nestedStatements, labeledStatement);
                } else if (falseControlFlowStatement instanceof PsiTryStatement) {
                    CompositeStatementObject tryStatement = (CompositeStatementObject) falseControlFlowStatement;
                    processTryStatement(nestedStatements, tryStatement);
                } else
                    nestedStatements.add(falseControlFlowStatement);
                List<BasicBlock> nestedBasicBlocks = getNestedBasicBlocks();
                nestedBasicBlocks.add(0, getBasicBlock());
                for (BasicBlock nestedBlock : nestedBasicBlocks) {
                    List<CFGNode> nodes = nestedBlock.getAllNodes();
                    for (CFGNode node : nodes) {
                        if (nestedStatements.contains(node.getStatement())) {
                            nestedNodes.add(node);
                        }
                    }
                }
            }
        }
        return nestedNodes;
    }
}
