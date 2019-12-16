package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.AbstractMethodDeclaration;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PDGSlice extends Graph {
    private PDG pdg;
    private AbstractMethodDeclaration method;
    private BasicBlock boundaryBlock;
    private PDGNode nodeCriterion;
    private AbstractVariable localVariableCriterion;
    private Set<PDGNode> sliceNodes;
    private Set<PDGNode> remainingNodes;
    private Set<AbstractVariable> passedParameters;
    private Set<PDGNode> indispensableNodes;
    private Set<PDGNode> removableNodes;
    private Set<AbstractVariable> returnedVariablesInOriginalMethod;
    private PsiFile psiFile;
    private int methodSize;

    PDGSlice(PDG pdg, BasicBlock boundaryBlock) {
        super();
        this.pdg = pdg;
        this.method = pdg.getMethod();
        this.psiFile = pdg.getPsiFile();
        this.methodSize = pdg.getTotalNumberOfStatements();
        this.returnedVariablesInOriginalMethod = pdg.getReturnedVariables();
        this.boundaryBlock = boundaryBlock;
        Set<PDGNode> regionNodes = pdg.blockBasedRegion(boundaryBlock);
        nodes.addAll(regionNodes);

        for (GraphEdge edge : pdg.edges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (nodes.contains(dependence.src) && nodes.contains(dependence.dst)) {
                if (dependence instanceof PDGDataDependence) {
                    PDGDataDependence dataDependence = (PDGDataDependence) dependence;
                    if (dataDependence.isLoopCarried()) {
                        PDGNode loopNode = dataDependence.getLoop().getPDGNode();
                        if (nodes.contains(loopNode))
                            edges.add(dataDependence);
                    } else
                        edges.add(dataDependence);
                } else if (dependence instanceof PDGAntiDependence) {
                    PDGAntiDependence antiDependence = (PDGAntiDependence) dependence;
                    if (antiDependence.isLoopCarried()) {
                        PDGNode loopNode = antiDependence.getLoop().getPDGNode();
                        if (nodes.contains(loopNode))
                            edges.add(antiDependence);
                    } else
                        edges.add(antiDependence);
                } else if (dependence instanceof PDGOutputDependence) {
                    PDGOutputDependence outputDependence = (PDGOutputDependence) dependence;
                    if (outputDependence.isLoopCarried()) {
                        PDGNode loopNode = outputDependence.getLoop().getPDGNode();
                        if (nodes.contains(loopNode))
                            edges.add(outputDependence);
                    } else
                        edges.add(outputDependence);
                } else
                    edges.add(dependence);
            }
        }
    }

    Set<PsiVariable> getVariableDeclarationsAndAccessedFieldsInMethod() {
        return pdg.getVariableDeclarationsAndAccessedFieldsInMethod();
    }

    public AbstractMethodDeclaration getMethod() {
        return method;
    }

    PsiFile getIFile() {
        return psiFile;
    }

    int getMethodSize() {
        return methodSize;
    }

    BasicBlock getBoundaryBlock() {
        return boundaryBlock;
    }

    PDGNode getExtractedMethodInvocationInsertionNode() {
        return ((TreeSet<PDGNode>) sliceNodes).first();
    }

    AbstractVariable getLocalVariableCriterion() {
        return localVariableCriterion;
    }

    Set<PDGNode> getSliceNodes() {
        return sliceNodes;
    }

    Set<AbstractVariable> getPassedParameters() {
        return passedParameters;
    }

    Set<PDGNode> getRemovableNodes() {
        return removableNodes;
    }

    boolean nodeBelongsToBlockBasedRegion(GraphNode node) {
        return nodes.contains(node);
    }

    boolean edgeBelongsToBlockBasedRegion(GraphEdge edge) {
        return edges.contains(edge);
    }

    PDGNode getDeclarationOfVariableCriterion() {
        PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
        for (PDGNode pdgNode : sliceNodes) {
            if (pdgNode.declaresLocalVariable(plainVariable))
                return pdgNode;
        }
        return null;
    }

    boolean declarationOfVariableCriterionBelongsToSliceNodes() {
        PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
        for (PDGNode pdgNode : sliceNodes) {
            if (pdgNode.declaresLocalVariable(plainVariable))
                return true;
        }
        return false;
    }

    boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
        PlainVariable plainVariable = localVariableCriterion.getInitialVariable();
        for (PDGNode pdgNode : removableNodes) {
            if (pdgNode.declaresLocalVariable(plainVariable))
                return true;
        }
        return false;
    }

    private boolean nodeCriterionIsDuplicated() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        return duplicatedNodes.contains(nodeCriterion);
    }

    private boolean sliceContainsBranchStatementWithoutInnermostLoop() {
        for (PDGNode node : sliceNodes) {
            CFGNode cfgNode = node.getCFGNode();
            if (cfgNode instanceof CFGBreakNode) {
                CFGBreakNode breakNode = (CFGBreakNode) cfgNode;
                CFGNode innerMostLoopNode = breakNode.getInnerMostLoopNode();
                if (innerMostLoopNode != null && !sliceNodes.contains(innerMostLoopNode.getPDGNode()))
                    return true;
            } else if (cfgNode instanceof CFGContinueNode) {
                CFGContinueNode continueNode = (CFGContinueNode) cfgNode;
                CFGNode innerMostLoopNode = continueNode.getInnerMostLoopNode();
                if (innerMostLoopNode != null && !sliceNodes.contains(innerMostLoopNode.getPDGNode()))
                    return true;
            }
        }
        return false;
    }

    private boolean sliceContainsReturnStatement() {
        for (PDGNode node : sliceNodes) {
            if (node.getCFGNode() instanceof CFGExitNode)
                return true;
        }
        return false;
    }

    private boolean declarationOfVariableCriterionIsDuplicated() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        for (PDGNode node : duplicatedNodes) {
            if (node.declaresLocalVariable(localVariableCriterion) && !(node instanceof PDGTryNode))
                return true;
        }
        return false;
    }

    private boolean returnStatementIsControlDependentOnSliceNode() {
        for (GraphNode node : pdg.nodes) {
            PDGNode pdgNode = (PDGNode) node;
            if (pdgNode.getCFGNode() instanceof CFGExitNode) {
                if (isControlDependentOnSliceNode(pdgNode))
                    return true;
                if (sliceNodes.contains(pdgNode))
                    return true;
            }
        }
        return false;
    }

    private boolean isControlDependentOnSliceNode(PDGNode node) {
        for (GraphEdge edge : node.incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                PDGControlDependence controlDependence = (PDGControlDependence) dependence;
                PDGNode srcPDGNode = (PDGNode) controlDependence.src;
                if (sliceNodes.contains(srcPDGNode))
                    return true;
                else
                    return isControlDependentOnSliceNode(srcPDGNode);
            }
        }
        return false;
    }

    private boolean nonDuplicatedSliceNodeAntiDependsOnNonRemovableNode() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        for (PDGNode sliceNode : sliceNodes) {
            if (!duplicatedNodes.contains(sliceNode)) {
                for (GraphEdge edge : sliceNode.incomingEdges) {
                    PDGDependence dependence = (PDGDependence) edge;
                    if (edges.contains(dependence) && dependence instanceof PDGAntiDependence) {
                        PDGAntiDependence antiDependence = (PDGAntiDependence) dependence;
                        PDGNode srcPDGNode = (PDGNode) antiDependence.src;
                        if (!removableNodes.contains(srcPDGNode) && !nodeDependsOnNonRemovableNode(srcPDGNode, antiDependence.getData()))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean nodeDependsOnNonRemovableNode(PDGNode node, AbstractVariable variable) {
        for (GraphEdge edge : node.incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (edges.contains(dependence) && dependence instanceof PDGDataDependence) {
                PDGDataDependence dataDependence = (PDGDataDependence) dependence;
                if (dataDependence.getData().equals(variable)) {
                    PDGNode srcPDGNode = (PDGNode) dataDependence.src;
                    if (!removableNodes.contains(srcPDGNode))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean nonDuplicatedSliceNodeOutputDependsOnNonRemovableNode() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        for (PDGNode sliceNode : sliceNodes) {
            if (!duplicatedNodes.contains(sliceNode)) {
                for (GraphEdge edge : sliceNode.incomingEdges) {
                    PDGDependence dependence = (PDGDependence) edge;
                    if (edges.contains(dependence) && dependence instanceof PDGOutputDependence) {
                        PDGOutputDependence outputDependence = (PDGOutputDependence) dependence;
                        PDGNode srcPDGNode = (PDGNode) outputDependence.src;
                        if (!removableNodes.contains(srcPDGNode))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean duplicatedSliceNodeWithClassInstantiationHasDependenceOnRemovableNode() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        for (PDGNode duplicatedNode : duplicatedNodes) {
            if (duplicatedNode.containsClassInstanceCreation()) {
                Map<PsiVariable, PsiNewExpression> classInstantiations = duplicatedNode.getClassInstantiations();
                for (PsiVariable variableDeclaration : classInstantiations.keySet()) {
                    for (GraphEdge edge : duplicatedNode.outgoingEdges) {
                        PDGDependence dependence = (PDGDependence) edge;
                        if (edges.contains(dependence) && dependence != null) {
                            PDGNode dstPDGNode = (PDGNode) dependence.dst;
                            if (removableNodes.contains(dstPDGNode)) {
                                if (dstPDGNode.changesStateOfReference(variableDeclaration)
                                        || dstPDGNode.assignsReference(variableDeclaration)
                                        || dstPDGNode.accessesReference(variableDeclaration))
                                    return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean nodeCritetionIsDeclarationOfVariableCriterion() {
        return nodeCriterion.declaresLocalVariable(localVariableCriterion);
    }

    private boolean variableCriterionIsReturnedVariableInOriginalMethod() {
        return returnedVariablesInOriginalMethod.contains(localVariableCriterion);
    }

    private boolean containsDuplicateNodeWithStateChangingMethodInvocation() {
        Set<PDGNode> duplicatedNodes = new LinkedHashSet<>(sliceNodes);
        duplicatedNodes.retainAll(indispensableNodes);
        for (PDGNode node : duplicatedNodes) {
            for (AbstractVariable stateChangingVariable : node.definedVariables) {
                if (stateChangingVariable instanceof CompositeVariable) {
                    PlainVariable plainVariable = stateChangingVariable.getInitialVariable();
                    if (!sliceContainsDeclaration(plainVariable))
                        return true;
                } else if (stateChangingVariable instanceof PlainVariable) {
                    PlainVariable plainVariable = stateChangingVariable.getInitialVariable();
                    if (plainVariable.isField())
                        return true;
                }
            }
        }
        return false;
    }

    private boolean sliceContainsDeclaration(AbstractVariable variableDeclaration) {
        for (PDGNode pdgNode : sliceNodes) {
            if (pdgNode.declaresLocalVariable(variableDeclaration))
                return true;
        }
        return false;
    }

    Set<PDGNode> computeSlice(PDGNode nodeCriterion, AbstractVariable localVariableCriterion) {
        Set<PDGNode> sliceNodes = new LinkedHashSet<>();
        if (nodeCriterion.definesLocalVariable(localVariableCriterion)) {
            sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
        } else if (nodeCriterion.usesLocalVariable(localVariableCriterion)) {
            Set<PDGNode> defNodes = getDefNodes(nodeCriterion, localVariableCriterion);
            for (PDGNode defNode : defNodes) {
                sliceNodes.addAll(traverseBackward(defNode, new LinkedHashSet<>()));
            }
            sliceNodes.addAll(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
        }
        return sliceNodes;
    }

    Set<PDGNode> computeSlice(PDGNode nodeCriterion) {
        return new LinkedHashSet<>(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
    }

    private Set<PDGNode> getDefNodes(PDGNode node, AbstractVariable localVariable) {
        Set<PDGNode> defNodes = new LinkedHashSet<>();
        for (GraphEdge edge : node.incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (edges.contains(dependence) && dependence instanceof PDGDataDependence) {
                PDGDataDependence dataDependence = (PDGDataDependence) dependence;
                if (dataDependence.getData().equals(localVariable)) {
                    PDGNode srcPDGNode = (PDGNode) dependence.src;
                    defNodes.add(srcPDGNode);
                }
            }
        }
        return defNodes;
    }

    private Set<PDGNode> traverseBackward(PDGNode node, Set<PDGNode> visitedNodes) {
        Set<PDGNode> sliceNodes = new LinkedHashSet<>();
        sliceNodes.add(node);
        visitedNodes.add(node);
        for (GraphEdge edge : node.incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (edges.contains(dependence) && !(dependence instanceof PDGAntiDependence)
                    && !(dependence instanceof PDGOutputDependence)) {
                PDGNode srcPDGNode = (PDGNode) dependence.src;
                if (!visitedNodes.contains(srcPDGNode))
                    sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes));
            }
        }
        return sliceNodes;
    }

    public String toString() {
        return "<" + localVariableCriterion + ", " + nodeCriterion.getId() + "> [B" + boundaryBlock.getId() + "]\n" +
                sliceNodes + "\npassed parameters: " + passedParameters + "\nindispensable nodes: " + indispensableNodes;
    }
}