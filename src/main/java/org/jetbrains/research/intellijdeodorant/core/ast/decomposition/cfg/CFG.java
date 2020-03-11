package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.*;
import org.jetbrains.research.intellijdeodorant.core.ast.AbstractMethodDeclaration;

import java.util.*;

public class CFG extends Graph {
    private static final int PUSH_NEW_LIST = 0;
    private static final int JOIN_TOP_LIST = 1;
    private static final int PLACE_NEW_LIST_SECOND_FROM_TOP = 2;
    private static final int JOIN_SECOND_FROM_TOP_LIST = 3;
    private final AbstractMethodDeclaration method;
    private final Stack<List<CFGBranchConditionalNode>> unjoinedConditionalNodes;
    private final Map<CFGBranchSwitchNode, List<CFGNode>> switchBreakMap;
    private final Map<CFGBlockNode, List<CFGNode>> directlyNestedNodesInBlocks;
    private BasicBlockCFG basicBlockCFG;

    public CFG(AbstractMethodDeclaration method) {
        this.method = method;
        this.unjoinedConditionalNodes = new Stack<>();
        this.switchBreakMap = new LinkedHashMap<>();
        this.directlyNestedNodesInBlocks = new LinkedHashMap<>();
        MethodBodyObject methodBody = method.getMethodBody();
        if (methodBody != null) {
            CompositeStatementObject composite = methodBody.getCompositeStatement();
            process(new ArrayList<>(), composite);
            GraphNode.resetNodeNum();
            this.basicBlockCFG = new BasicBlockCFG(this);
        }
    }

    public AbstractMethodDeclaration getMethod() {
        return method;
    }

    BasicBlockCFG getBasicBlockCFG() {
        return basicBlockCFG;
    }

    List<BasicBlock> getBasicBlocks() {
        return basicBlockCFG.getBasicBlocks();
    }

    Map<CFGBlockNode, List<CFGNode>> getDirectlyNestedNodesInBlocks() {
        return directlyNestedNodesInBlocks;
    }

    private List<CFGNode> process(List<CFGNode> previousNodes, CompositeStatementObject composite) {
        if (composite instanceof TryStatementObject) {
            CFGTryNode tryNode = new CFGTryNode(composite);
            directlyNestedNodeInBlock(tryNode);
            findBlockNodeControlParent(tryNode);
            directlyNestedNodesInBlocks.put(tryNode, new ArrayList<>());
            AbstractStatement firstStatement = composite.getStatements().get(0);
            if (firstStatement instanceof CompositeStatementObject) {
                composite = (CompositeStatementObject) firstStatement;
            }
        } else if (composite instanceof SynchronizedStatementObject) {
            CFGSynchronizedNode synchronizedNode = new CFGSynchronizedNode(composite);
            directlyNestedNodeInBlock(synchronizedNode);
            findBlockNodeControlParent(synchronizedNode);
            directlyNestedNodesInBlocks.put(synchronizedNode, new ArrayList<>());
            AbstractStatement firstStatement = composite.getStatements().get(0);
            if (firstStatement instanceof CompositeStatementObject) {
                composite = (CompositeStatementObject) firstStatement;
            }
        }
        int i = 0;
        for (AbstractStatement abstractStatement : composite.getStatements()) {
            if (abstractStatement instanceof StatementObject) {
                StatementObject statement = (StatementObject) abstractStatement;
                previousNodes = processNonCompositeStatement(previousNodes, statement, composite);
            } else if (abstractStatement instanceof CompositeStatementObject) {
                CompositeStatementObject compositeStatement = (CompositeStatementObject) abstractStatement;
                if (compositeStatement.getStatement() instanceof PsiBlockStatement) {
                    previousNodes = process(previousNodes, compositeStatement);
                } else if (compositeStatement.getStatement() instanceof PsiLabeledStatement) {
                    List<AbstractStatement> nestedStatements = compositeStatement.getStatements();
                    if (!nestedStatements.isEmpty()) {
                        AbstractStatement firstStatement = nestedStatements.get(0);
                        if (firstStatement instanceof CompositeStatementObject) {
                            CompositeStatementObject compositeStatement2 = (CompositeStatementObject) firstStatement;
                            if (compositeStatement2.getStatement() instanceof PsiBlockStatement) {
                                previousNodes = process(previousNodes, compositeStatement2);
                            } else if (compositeStatement2 instanceof SynchronizedStatementObject) {
                                previousNodes = processSynchronizedStatement(previousNodes, compositeStatement2);
                            } else if (compositeStatement2 instanceof TryStatementObject) {
                                previousNodes = processTryStatement(previousNodes, compositeStatement2);
                            } else if (isLoop(compositeStatement2)) {
                                previousNodes = processLoopStatement(previousNodes, compositeStatement2);
                            } else if (compositeStatement2.getStatement() instanceof PsiDoWhileStatement) {
                                previousNodes = processDoStatement(previousNodes, compositeStatement2);
                            } else if (compositeStatement2.getStatement() instanceof PsiSwitchStatement) {
                                int action = getAction(composite, i, compositeStatement2);
                                previousNodes = processSwitchStatement(previousNodes, compositeStatement2, action);
                            } else if (compositeStatement2.getStatement() instanceof PsiIfStatement) {
                                int action = getAction(composite, i, compositeStatement2);
                                previousNodes = processIfStatement(previousNodes, compositeStatement2, action);
                            }
                        } else if (firstStatement instanceof StatementObject) {
                            previousNodes = processNonCompositeStatement(previousNodes, (StatementObject) firstStatement, composite);
                        }
                    }
                } else if (compositeStatement instanceof SynchronizedStatementObject) {
                    previousNodes = processSynchronizedStatement(previousNodes, compositeStatement);
                } else if (compositeStatement instanceof TryStatementObject) {
                    previousNodes = processTryStatement(previousNodes, compositeStatement);
                } else if (isLoop(compositeStatement)) {
                    previousNodes = processLoopStatement(previousNodes, compositeStatement);
                } else if (compositeStatement.getStatement() instanceof PsiDoWhileStatement) {
                    previousNodes = processDoStatement(previousNodes, compositeStatement);
                } else if (compositeStatement.getStatement() instanceof PsiSwitchStatement) {
                    int action = getAction(composite, i, compositeStatement);
                    previousNodes = processSwitchStatement(previousNodes, compositeStatement, action);
                } else if (compositeStatement.getStatement() instanceof PsiIfStatement) {
                    int action = getAction(composite, i, compositeStatement);
                    previousNodes = processIfStatement(previousNodes, compositeStatement, action);
                }
            }
            i++;
        }
        return previousNodes;
    }

    private List<CFGNode> processSynchronizedStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement) {
        CFGSynchronizedNode synchronizedNode = new CFGSynchronizedNode(compositeStatement);
        directlyNestedNodeInBlock(synchronizedNode);
        findBlockNodeControlParent(synchronizedNode);
        nodes.add(synchronizedNode);
        directlyNestedNodesInBlocks.put(synchronizedNode, new ArrayList<>());
        createTopDownFlow(previousNodes, synchronizedNode);
        ArrayList<CFGNode> currentNodes = new ArrayList<>();
        currentNodes.add(synchronizedNode);
        previousNodes = currentNodes;
        AbstractStatement firstStatement = compositeStatement.getStatements().get(0);
        if (firstStatement instanceof CompositeStatementObject) {
            previousNodes = process(previousNodes, (CompositeStatementObject) firstStatement);
        }
        return previousNodes;
    }

    private List<CFGNode> processTryStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement) {
        TryStatementObject tryStatement = (TryStatementObject) compositeStatement;
        if (!tryStatement.hasResources()) {
            //if a try node does not have resources, it is treated as a block and is omitted
            CFGTryNode tryNode = new CFGTryNode(compositeStatement);
            //nodes.add(tryNode);
            directlyNestedNodeInBlock(tryNode);
            findBlockNodeControlParent(tryNode);
            directlyNestedNodesInBlocks.put(tryNode, new ArrayList<>());
            AbstractStatement firstStatement = compositeStatement.getStatements().get(0);
            if (firstStatement instanceof CompositeStatementObject) {
                previousNodes = process(previousNodes, (CompositeStatementObject) firstStatement);
            }
        } else {
            //if a try node has resources, it is treated as a non-composite node
            CFGTryNode tryNode = new CFGTryNode(compositeStatement);
            directlyNestedNodeInBlock(tryNode);
            findBlockNodeControlParent(tryNode);
            nodes.add(tryNode);
            directlyNestedNodesInBlocks.put(tryNode, new ArrayList<>());
            createTopDownFlow(previousNodes, tryNode);
            ArrayList<CFGNode> currentNodes = new ArrayList<>();
            currentNodes.add(tryNode);
            previousNodes = currentNodes;
            AbstractStatement firstStatement = compositeStatement.getStatements().get(0);
            if (firstStatement instanceof CompositeStatementObject) {
                previousNodes = process(previousNodes, (CompositeStatementObject) firstStatement);
            }
        }
        return previousNodes;
    }

    private void findBlockNodeControlParent(CFGBlockNode blockNode) {
        for (GraphNode node : nodes) {
            CFGNode cfgNode = (CFGNode) node;
            if (cfgNode.getStatement() instanceof CompositeStatementObject) {
                CompositeStatementObject composite = (CompositeStatementObject) cfgNode.getStatement();
                if (directlyNestedNode(blockNode, composite)) {
                    blockNode.setControlParent(cfgNode);
                    break;
                }
            }
        }
        for (CFGBlockNode blockNode2 : directlyNestedNodesInBlocks.keySet()) {
            List<CFGNode> nestedNodes = directlyNestedNodesInBlocks.get(blockNode2);
            if (nestedNodes.contains(blockNode)) {
                blockNode.setControlParent(blockNode2);
                break;
            }
        }
    }

    private List<CFGNode> processDoStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement) {
        List<CFGNode> tmpNodes = previousNodes;
        previousNodes = process(previousNodes, compositeStatement);
        CFGBranchNode currentNode = new CFGBranchDoLoopNode(compositeStatement);
        nodes.add(currentNode);
        directlyNestedNodeInBlock(currentNode);
        createTopDownFlow(previousNodes, currentNode);
        CFGNode topNode = getCommonNextNode(tmpNodes);
        if (topNode == null)
            topNode = (CFGNode) nodes.toArray()[0];
        Flow flow = new Flow(currentNode, topNode);
        flow.setTrueControlFlow(true);
        flow.setLoopbackFlow(true);
        edges.add(flow);
        ArrayList<CFGNode> currentNodes = new ArrayList<>();
        currentNodes.add(currentNode);
        previousNodes = currentNodes;
        return previousNodes;
    }

    private List<CFGNode> processLoopStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement) {
        CFGBranchNode currentNode = new CFGBranchLoopNode(compositeStatement);
        nodes.add(currentNode);
        directlyNestedNodeInBlock(currentNode);
        createTopDownFlow(previousNodes, currentNode);
        previousNodes = new ArrayList<>();
        ArrayList<CFGNode> currentNodes = new ArrayList<>();
        currentNodes.add(currentNode);
        previousNodes.addAll(process(currentNodes, compositeStatement));
        for (CFGNode previousNode : previousNodes) {
            Flow flow = new Flow(previousNode, currentNode);
            if (previousNode instanceof CFGBranchNode) {
                if (previousNode.equals(currentNode))
                    flow.setTrueControlFlow(true);
                else
                    flow.setFalseControlFlow(true);
            }
            flow.setLoopbackFlow(true);
            edges.add(flow);
        }
        if (previousNodes.size() > 1) {
            List<CFGBranchConditionalNode> conditionalNodes = unjoinedConditionalNodes.pop();
            for (CFGBranchConditionalNode conditionalNode : conditionalNodes) {
                conditionalNode.setJoinNode(currentNode);
            }
        }
        previousNodes = currentNodes;
        return previousNodes;
    }

    private int getAction(CompositeStatementObject parentComposite, int i, CompositeStatementObject childComposite) {
        int action = PUSH_NEW_LIST;
        List<AbstractStatement> statements = new ArrayList<>(parentComposite.getStatements());
        CompositeStatementObject parent = (CompositeStatementObject) statements.get(0).getParent();
        boolean isBlockWithoutCompositeParent = isBlockWithoutCompositeParent(parent);
        if (parent.getStatement() instanceof PsiBlockStatement)
            parent = (CompositeStatementObject) parent.getParent();
        int position = i;
        while ((parent instanceof TryStatementObject || parent instanceof SynchronizedStatementObject)) {
            CompositeStatementObject tryStatement = parent;
            CompositeStatementObject tryStatementParent = (CompositeStatementObject) tryStatement.getParent();
            List<AbstractStatement> tryParentStatements = new ArrayList<>(tryStatementParent.getStatements());
            if (tryStatementParent.getStatement() instanceof PsiBlockStatement)
                tryStatementParent = (CompositeStatementObject) tryStatementParent.getParent();
            int positionOfTryStatementInParent = 0;
            int j = 0;
            for (AbstractStatement statement : tryParentStatements) {
                if (statement.equals(tryStatement)) {
                    positionOfTryStatementInParent = j;
                    break;
                }
                j++;
            }
            if (tryStatement instanceof TryStatementObject) {
                TryStatementObject tempTry = (TryStatementObject) tryStatement;
                if (tempTry.hasResources()) {
                    tryParentStatements.addAll(positionOfTryStatementInParent + 1, statements);
                } else {
                    tryParentStatements.remove(tryStatement);
                    tryParentStatements.addAll(positionOfTryStatementInParent, statements);
                }
            } else {
                tryParentStatements.addAll(positionOfTryStatementInParent + 1, statements);
            }
            statements = tryParentStatements;
            parent = tryStatementParent;
            if (tryStatement instanceof TryStatementObject) {
                TryStatementObject tempTry = (TryStatementObject) tryStatement;
                if (tempTry.hasResources())
                    position = positionOfTryStatementInParent + position + 1;
                else
                    position = positionOfTryStatementInParent + position;
            } else {
                position = positionOfTryStatementInParent + position + 1;
            }
        }
        if (parent != null && parent.getStatement() instanceof PsiSwitchStatement &&
                parentComposite.getStatement() instanceof PsiBlockStatement) {
            List<AbstractStatement> switchStatements = new ArrayList<>(parent.getStatements());
            int positionOfBlockInParentSwitch = 0;
            int j = 0;
            for (AbstractStatement statement : switchStatements) {
                if (statement.equals(parentComposite)) {
                    positionOfBlockInParentSwitch = j;
                    break;
                }
                j++;
            }
            switchStatements.remove(parentComposite);
            switchStatements.addAll(positionOfBlockInParentSwitch, statements);
            statements = switchStatements;
            position = positionOfBlockInParentSwitch + position;
        }
        if (parent != null && isBlockWithoutCompositeParent) {
            List<AbstractStatement> blockStatements = new ArrayList<>(parent.getStatements());
            int positionOfBlockInParent = 0;
            int j = 0;
            for (AbstractStatement statement : blockStatements) {
                if (statement.equals(parentComposite)) {
                    positionOfBlockInParent = j;
                    break;
                }
                j++;
            }
            blockStatements.remove(parentComposite);
            blockStatements.addAll(positionOfBlockInParent, statements);
            statements = blockStatements;
            position = positionOfBlockInParent + position;
        }
        if (statements.size() == 1) {
            action = JOIN_TOP_LIST;
            if (parent != null) {
                if (isLoop(parent) || parent.getStatement() instanceof PsiDoWhileStatement)
                    action = PUSH_NEW_LIST;
            }
        } else if (statements.size() > 1) {
            AbstractStatement previousStatement = null;
            if (position >= 1)
                previousStatement = statements.get(position - 1);
            int j = 0;
            while (previousStatement instanceof TryStatementObject && !((TryStatementObject) previousStatement).hasResources()) {
                CompositeStatementObject tryStatement = (CompositeStatementObject) previousStatement;
                AbstractStatement firstStatement = tryStatement.getStatements().get(0);
                if (firstStatement instanceof CompositeStatementObject) {
                    CompositeStatementObject tryBlock = (CompositeStatementObject) firstStatement;
                    List<AbstractStatement> tryBlockStatements = tryBlock.getStatements();
                    if (tryBlockStatements.size() > 0) {
                        //previous statement is the last statement of this try block
                        previousStatement = tryBlockStatements.get(tryBlockStatements.size() - 1);
                    } else {
                        //try block is empty and previous statement is the statement before this try block
                        if (position >= 2 + j)
                            previousStatement = statements.get(position - 2 - j);
                        else
                            previousStatement = null;
                    }
                } else {
                    previousStatement = null;
                }
                j++;
            }
            while (previousStatement != null && isBlockWithoutCompositeParent(previousStatement)) {
                CompositeStatementObject block = (CompositeStatementObject) previousStatement;
                List<AbstractStatement> blockStatements = block.getStatements();
                if (blockStatements.size() > 0) {
                    //previous statement is the last statement of this block
                    previousStatement = blockStatements.get(blockStatements.size() - 1);
                }
            }
            if (statements.get(statements.size() - 1).equals(childComposite)) {
                //current if statement is the last statement of the composite statement
                if (previousStatement != null && (previousStatement.getStatement() instanceof PsiIfStatement
                        || previousStatement.getStatement() instanceof PsiSwitchStatement)) {
                    action = JOIN_SECOND_FROM_TOP_LIST;
                    if (parent != null && (isLoop(parent) || parent.getStatement() instanceof PsiDoWhileStatement))
                        action = PLACE_NEW_LIST_SECOND_FROM_TOP;
                } else {
                    action = JOIN_TOP_LIST;
                    if (parent != null && (isLoop(parent) || parent.getStatement() instanceof PsiDoWhileStatement))
                        action = PUSH_NEW_LIST;
                }
            } else {
                if (previousStatement != null && (previousStatement.getStatement() instanceof PsiIfStatement
                        || previousStatement.getStatement() instanceof PsiSwitchStatement))
                    action = PLACE_NEW_LIST_SECOND_FROM_TOP;
                else {
                    action = PUSH_NEW_LIST;
                }
            }
        }
        return action;
    }

    private boolean isBlockWithoutCompositeParent(AbstractStatement statement) {
        boolean isBlock = false;
        if (statement.getStatement() instanceof PsiBlockStatement) {
            isBlock = true;
        }
        boolean parentIsBlock = false;
        AbstractStatement parent = (AbstractStatement) statement.getParent();
        if (parent != null && parent.getStatement() instanceof PsiBlockStatement) {
            parentIsBlock = true;
        }
        return isBlock && parentIsBlock;
    }

    private List<CFGNode> processNonCompositeStatement(List<CFGNode> previousNodes, StatementObject statement,
                                                       CompositeStatementObject composite) {
        //special handling of break, continue, return
        CFGNode currentNode = createNonCompositeNode(statement);
        nodes.add(currentNode);
        if ((currentNode instanceof CFGBreakNode || currentNode instanceof CFGExitNode) &&
                composite.getStatement() instanceof PsiSwitchStatement && directlyNestedNode(currentNode, composite)) {
            CFGBranchSwitchNode switchNode = getMostRecentSwitchNode();
            if (switchBreakMap.containsKey(switchNode)) {
                List<CFGNode> breakList = switchBreakMap.get(switchNode);
                breakList.add(currentNode);
            } else {
                List<CFGNode> breakList = new ArrayList<>();
                breakList.add(currentNode);
                switchBreakMap.put(switchNode, breakList);
            }
            createTopDownFlow(previousNodes, currentNode);
        } else if (currentNode instanceof CFGSwitchCaseNode) {
            CFGSwitchCaseNode switchCase = (CFGSwitchCaseNode) currentNode;
            if (previousNodesContainBreakOrReturn(previousNodes, composite)) {
                CFGBranchSwitchNode switchNode = getMostRecentSwitchNode();
                if (switchNode != null) {
                    Flow flow = new Flow(switchNode, currentNode);
                    if (switchCase.isDefault())
                        flow.setFalseControlFlow(true);
                    else
                        flow.setTrueControlFlow(true);
                    edges.add(flow);
                }
            } else
                createTopDownFlow(previousNodes, currentNode);
        } else
            createTopDownFlow(previousNodes, currentNode);
        ArrayList<CFGNode> currentNodes = new ArrayList<>();
        currentNodes.add(currentNode);
        previousNodes = currentNodes;
        return previousNodes;
    }

    private CFGNode createNonCompositeNode(StatementObject statement) {
        CFGNode currentNode;
        PsiElement astStatement = statement.getStatement();
        if (astStatement instanceof PsiReturnStatement)
            currentNode = new CFGExitNode(statement);
        else if (astStatement instanceof PsiSwitchLabelStatement)
            currentNode = new CFGSwitchCaseNode(statement);
        else if (astStatement instanceof PsiBreakStatement)
            currentNode = new CFGBreakNode(statement);
        else if (astStatement instanceof PsiContinueStatement)
            currentNode = new CFGContinueNode(statement);
        else if (astStatement instanceof PsiThrowStatement)
            currentNode = new CFGThrowNode(statement);
        else
            currentNode = new CFGNode(statement);
        directlyNestedNodeInBlock(currentNode);
        return currentNode;
    }

    private boolean previousNodesContainBreakOrReturn(List<CFGNode> previousNodes, CompositeStatementObject composite) {
        for (CFGNode previousNode : previousNodes) {
            PsiStatement statement = previousNode.getASTStatement();
            if ((statement instanceof PsiBreakStatement || statement instanceof PsiReturnStatement) &&
                    directlyNestedNode(previousNode, composite))
                return true;
        }
        return false;
    }

    private boolean directlyNestedNode(CFGNode node, CompositeStatementObject composite) {
        for (AbstractStatement statement : composite.getStatements()) {
            if (statement.equals(node.getStatement()))
                return true;
            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject composite2 = (CompositeStatementObject) statement;
                PsiElement astComposite2 = composite2.getStatement();
                if (astComposite2 instanceof PsiBlockStatement) {
                    if (directlyNestedNode(node, composite2))
                        return true;
                }
            }
        }
        return false;
    }

    private void directlyNestedNodeInBlock(CFGNode node) {
        for (CFGBlockNode blockNode : directlyNestedNodesInBlocks.keySet()) {
            if (blockNode.getStatement() instanceof CompositeStatementObject
                    && directlyNestedNode(node, (CompositeStatementObject) blockNode.getStatement())) {
                List<CFGNode> directlyNestedNodes = directlyNestedNodesInBlocks.get(blockNode);
                directlyNestedNodes.add(node);
                break;
            }
        }
    }

    private List<CFGNode> processSwitchStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement, int action) {
        CFGBranchSwitchNode currentNode = new CFGBranchSwitchNode(compositeStatement);
        handleAction(currentNode, action);
        nodes.add(currentNode);
        directlyNestedNodeInBlock(currentNode);
        createTopDownFlow(previousNodes, currentNode);
        previousNodes = new ArrayList<>();
        ArrayList<CFGNode> currentNodes = new ArrayList<>();
        currentNodes.add(currentNode);
        previousNodes.addAll(process(currentNodes, compositeStatement));
        List<CFGNode> breakList = switchBreakMap.get(currentNode);
        if (breakList != null) {
            for (CFGNode node : breakList) {
                if (!previousNodes.contains(node))
                    previousNodes.add(node);
            }
        }
        if (currentNode.getFalseControlFlow() == null)
            previousNodes.add(currentNode);
        return previousNodes;
    }

    private List<CFGNode> processIfStatement(List<CFGNode> previousNodes, CompositeStatementObject compositeStatement, int action) {
        CFGBranchIfNode currentNode = new CFGBranchIfNode(compositeStatement);
        handleAction(currentNode, action);
        nodes.add(currentNode);
        directlyNestedNodeInBlock(currentNode);
        createTopDownFlow(previousNodes, currentNode);
        previousNodes = new ArrayList<>();
        List<AbstractStatement> ifStatementList = compositeStatement.getStatements();
        AbstractStatement thenClause = ifStatementList.get(0);
        if (thenClause instanceof StatementObject) {
            StatementObject thenClauseStatement = (StatementObject) thenClause;
            CFGNode thenClauseNode = createNonCompositeNode(thenClauseStatement);
            nodes.add(thenClauseNode);
            ArrayList<CFGNode> currentNodes = new ArrayList<>();
            currentNodes.add(currentNode);
            createTopDownFlow(currentNodes, thenClauseNode);
            previousNodes.add(thenClauseNode);
        } else if (thenClause instanceof CompositeStatementObject) {
            CompositeStatementObject thenClauseCompositeStatement = (CompositeStatementObject) thenClause;
            ArrayList<CFGNode> currentNodes = new ArrayList<>();
            currentNodes.add(currentNode);
            if (thenClauseCompositeStatement.getStatement() instanceof PsiIfStatement)
                previousNodes.addAll(processIfStatement(currentNodes, thenClauseCompositeStatement, JOIN_TOP_LIST));
            else if (thenClauseCompositeStatement.getStatement() instanceof PsiSwitchStatement)
                previousNodes.addAll(processSwitchStatement(currentNodes, thenClauseCompositeStatement, JOIN_TOP_LIST));
            else if (isLoop(thenClauseCompositeStatement))
                previousNodes.addAll(processLoopStatement(currentNodes, thenClauseCompositeStatement));
            else if (thenClauseCompositeStatement.getStatement() instanceof PsiDoWhileStatement)
                previousNodes.addAll(processDoStatement(currentNodes, thenClauseCompositeStatement));
            else
                previousNodes.addAll(process(currentNodes, thenClauseCompositeStatement));
        }
        if (ifStatementList.size() == 2) {
            AbstractStatement elseClause = ifStatementList.get(1);
            if (elseClause instanceof StatementObject) {
                StatementObject elseClauseStatement = (StatementObject) elseClause;
                CFGNode elseClauseNode = createNonCompositeNode(elseClauseStatement);
                nodes.add(elseClauseNode);
                ArrayList<CFGNode> currentNodes = new ArrayList<>();
                currentNodes.add(currentNode);
                createTopDownFlow(currentNodes, elseClauseNode);
                previousNodes.add(elseClauseNode);
            } else if (elseClause instanceof CompositeStatementObject) {
                CompositeStatementObject elseClauseCompositeStatement = (CompositeStatementObject) elseClause;
                ArrayList<CFGNode> currentNodes = new ArrayList<>();
                currentNodes.add(currentNode);
                if (elseClauseCompositeStatement.getStatement() instanceof PsiIfStatement)
                    previousNodes.addAll(processIfStatement(currentNodes, elseClauseCompositeStatement, JOIN_TOP_LIST));
                else if (elseClauseCompositeStatement.getStatement() instanceof PsiSwitchStatement)
                    previousNodes.addAll(processSwitchStatement(currentNodes, elseClauseCompositeStatement, JOIN_TOP_LIST));
                else if (isLoop(elseClauseCompositeStatement))
                    previousNodes.addAll(processLoopStatement(currentNodes, elseClauseCompositeStatement));
                else if (elseClauseCompositeStatement.getStatement() instanceof PsiDoWhileStatement)
                    previousNodes.addAll(processDoStatement(currentNodes, elseClauseCompositeStatement));
                else
                    previousNodes.addAll(process(currentNodes, elseClauseCompositeStatement));
            }
        } else {
            previousNodes.add(currentNode);
        }
        return previousNodes;
    }

    private void handleAction(CFGBranchConditionalNode currentNode, int action) {
        if (action == JOIN_TOP_LIST && !unjoinedConditionalNodes.empty()) {
            List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.peek();
            topList.add(currentNode);
        } else if (action == JOIN_SECOND_FROM_TOP_LIST) {
            if (unjoinedConditionalNodes.size() > 1) {
                List<CFGBranchConditionalNode> list = unjoinedConditionalNodes.elementAt(unjoinedConditionalNodes.size() - 2);
                list.add(currentNode);
            } else {
                List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.pop();
                List<CFGBranchConditionalNode> list = new ArrayList<>();
                list.add(currentNode);
                unjoinedConditionalNodes.push(list);
                unjoinedConditionalNodes.push(topList);
            }
        } else if (action == PLACE_NEW_LIST_SECOND_FROM_TOP && !unjoinedConditionalNodes.empty()) {
            List<CFGBranchConditionalNode> topList = unjoinedConditionalNodes.pop();
            List<CFGBranchConditionalNode> list = new ArrayList<>();
            list.add(currentNode);
            unjoinedConditionalNodes.push(list);
            unjoinedConditionalNodes.push(topList);
        } else {
            List<CFGBranchConditionalNode> list = new ArrayList<>();
            list.add(currentNode);
            unjoinedConditionalNodes.push(list);
        }
    }

    private void createTopDownFlow(List<CFGNode> previousNodes, CFGNode currentNode) {
        for (CFGNode previousNode : previousNodes) {
            Flow flow = new Flow(previousNode, currentNode);
            int numberOfImmediateBlocks = getNumberOfImmediateBlocks(currentNode);
            if (previousNode instanceof CFGBranchNode) {
                if (currentNode.getId() == previousNode.getId() + 1 + numberOfImmediateBlocks &&
                        !(previousNode instanceof CFGBranchDoLoopNode))
                    flow.setTrueControlFlow(true);
                else
                    flow.setFalseControlFlow(true);
            }
            edges.add(flow);
        }
        if (previousNodes.size() > 1) {
            List<CFGBranchConditionalNode> conditionalNodes = unjoinedConditionalNodes.pop();
            for (CFGBranchConditionalNode conditionalNode : conditionalNodes) {
                conditionalNode.setJoinNode(currentNode);
            }
        }
    }

    private int getNumberOfImmediateBlocks(CFGNode node) {
        for (CFGBlockNode tryNode : directlyNestedNodesInBlocks.keySet()) {
            List<CFGNode> directlyNestedNodes = directlyNestedNodesInBlocks.get(tryNode);
            if (directlyNestedNodes.contains(node))
                return 1 + getNumberOfImmediateBlocks(tryNode);
        }
        return 0;
    }

    private boolean isLoop(CompositeStatementObject compositeStatement) {
        return compositeStatement.getStatement() instanceof PsiWhileStatement
                || compositeStatement.getStatement() instanceof PsiForStatement
                || compositeStatement.getStatement() instanceof PsiForeachStatement;
    }

    private CFGNode getCommonNextNode(List<CFGNode> nodes) {
        HashMap<CFGNode, Integer> nextNodeCounterMap = new HashMap<>();
        for (CFGNode node : nodes) {
            for (GraphEdge edge : node.outgoingEdges) {
                CFGNode nextNode = (CFGNode) edge.dst;
                if (nextNodeCounterMap.containsKey(nextNode))
                    nextNodeCounterMap.put(nextNode, nextNodeCounterMap.get(nextNode) + 1);
                else
                    nextNodeCounterMap.put(nextNode, 1);
            }
        }
        for (CFGNode key : nextNodeCounterMap.keySet()) {
            if (nextNodeCounterMap.get(key) == nodes.size())
                return key;
        }
        return null;
    }

    private CFGBranchSwitchNode getMostRecentSwitchNode() {
        for (int i = unjoinedConditionalNodes.size() - 1; i >= 0; i--) {
            List<CFGBranchConditionalNode> unjoinedConditionalNodeList = unjoinedConditionalNodes.get(i);
            for (int j = unjoinedConditionalNodeList.size() - 1; j >= 0; j--) {
                CFGBranchConditionalNode conditionalNode = unjoinedConditionalNodeList.get(j);
                if (conditionalNode instanceof CFGBranchSwitchNode) {
                    return (CFGBranchSwitchNode) conditionalNode;
                }
            }
        }
        return null;
    }
}
