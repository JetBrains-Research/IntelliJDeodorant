package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiFile;
import org.jetbrains.research.intellijdeodorant.core.ast.AbstractMethodDeclaration;

import java.util.LinkedHashSet;
import java.util.Set;

public class PDGSlice extends Graph {
    private final PDG pdg;
    private final AbstractMethodDeclaration method;
    private final BasicBlock boundaryBlock;
    private PDGNode nodeCriterion;
    private AbstractVariable localVariableCriterion;
    private Set<PDGNode> sliceNodes;
    private Set<PDGNode> remainingNodes;
    private Set<AbstractVariable> passedParameters;
    private Set<PDGNode> indispensableNodes;
    private Set<PDGNode> removableNodes;
    private final Set<AbstractVariable> returnedVariablesInOriginalMethod;
    private final PsiFile psiFile;
    private final int methodSize;

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

    public AbstractMethodDeclaration getMethod() {
        return method;
    }

    boolean nodeBelongsToBlockBasedRegion(GraphNode node) {
        return nodes.contains(node);
    }

    boolean edgeBelongsToBlockBasedRegion(GraphEdge edge) {
        return edges.contains(edge);
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