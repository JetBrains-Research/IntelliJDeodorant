package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import java.util.*;

public class PDGObjectSliceUnionCollection {
    private final Map<BasicBlock, PDGObjectSliceUnion> objectSliceUnionMap;

    public PDGObjectSliceUnionCollection(PDG pdg, PlainVariable objectReference) {
        this.objectSliceUnionMap = new LinkedHashMap<>();
        Map<CompositeVariable, LinkedHashSet<PDGNode>> definedAttributeNodeCriteriaMap =
                pdg.getDefinedAttributesOfReference(objectReference);
        Map<CompositeVariable, Set<BasicBlock>> definedAttributeBasicBlockMap = new LinkedHashMap<>();
        for (CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
            Set<PDGNode> nodeCriteria = definedAttributeNodeCriteriaMap.get(compositeVariable);
            Map<PDGNode, Set<BasicBlock>> boundaryBlockMap = new LinkedHashMap<>();
            for (PDGNode nodeCriterion : nodeCriteria) {
                Set<BasicBlock> boundaryBlocks = pdg.boundaryBlocks(nodeCriterion);
                boundaryBlockMap.put(nodeCriterion, boundaryBlocks);
            }
            List<Set<BasicBlock>> basicBlockListPerNodeCriterion = new ArrayList<>(boundaryBlockMap.values());
            if (!basicBlockListPerNodeCriterion.isEmpty()) {
                Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<>(basicBlockListPerNodeCriterion.get(0));
                for (int i = 1; i < basicBlockListPerNodeCriterion.size(); i++) {
                    basicBlockIntersection.retainAll(basicBlockListPerNodeCriterion.get(i));
                }
                definedAttributeBasicBlockMap.put(compositeVariable, basicBlockIntersection);
            } else {
                definedAttributeBasicBlockMap.put(compositeVariable, new LinkedHashSet<>());
            }
        }
        List<Set<BasicBlock>> basicBlockListPerCompositeVariable = new ArrayList<>(definedAttributeBasicBlockMap.values());
        if (!basicBlockListPerCompositeVariable.isEmpty()) {
            Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<>(basicBlockListPerCompositeVariable.get(0));
            for (int i = 1; i < basicBlockListPerCompositeVariable.size(); i++) {
                basicBlockIntersection.retainAll(basicBlockListPerCompositeVariable.get(i));
            }
            for (BasicBlock basicBlock : basicBlockIntersection) {
                Set<PDGNode> allNodeCriteria = new LinkedHashSet<>();
                for (CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
                    Set<PDGNode> nodeCriteria = definedAttributeNodeCriteriaMap.get(compositeVariable);
                    allNodeCriteria.addAll(nodeCriteria);
                }
                PDGObjectSliceUnion objectSliceUnion = new PDGObjectSliceUnion(pdg, basicBlock, allNodeCriteria, objectReference);
                if (objectSliceUnion.satisfiesRules())
                    objectSliceUnionMap.put(basicBlock, objectSliceUnion);
            }
        }
    }

    public Collection<PDGObjectSliceUnion> getSliceUnions() {
        return objectSliceUnionMap.values();
    }
}
