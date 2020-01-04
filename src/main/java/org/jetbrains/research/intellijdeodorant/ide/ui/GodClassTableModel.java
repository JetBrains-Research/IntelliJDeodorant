package org.jetbrains.research.intellijdeodorant.ide.ui;

import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractedConcept;
import org.jetbrains.research.intellijdeodorant.ide.ui.abstractrefactorings.ExtractClassRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.ui.abstractrefactorings.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.abstractrefactorings.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup;
import org.jetbrains.research.intellijdeodorant.ide.ui.abstractrefactorings.RefactoringType.AbstractCandidateRefactoringGroup;

import java.util.ArrayList;
import java.util.List;

public class GodClassTableModel extends AbstractTreeTableModel {
    public GodClassTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(candidateRefactoringGroups, columnNames, new ExtractClassRefactoringType());
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractExtractClassCandidateRefactoringGroup =
                    (AbstractExtractClassCandidateRefactoringGroup) o;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractExtractClassCandidateRefactoringGroup.getCandidateRefactoringGroup();

            if (index == 0) {
                return group.getSource();
            } else {
                return "";
            }
        } else
        if (o instanceof AbstractExtractClassCandidateRefactoring) {
            AbstractExtractClassCandidateRefactoring abstractCandidateRefactoring = (AbstractExtractClassCandidateRefactoring) o;
            ExtractClassCandidateRefactoring candidateRefactoring = (ExtractClassCandidateRefactoring) abstractCandidateRefactoring.getCandidateRefactoring();
            switch (index) {
                case 0:
                    return "";
                case 1:
                    return candidateRefactoring.getSourceEntity();
                case 2:
                    return candidateRefactoring.getExtractedFieldFragments().size();
            }
        }

        return "";
    }


    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractGroup = (AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return group.getExtractedConcepts().size();
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children.size();
        } else {
            return super.getChildCount(parent);
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractGroup = (AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return new ExtractedConceptAndChildren(group.getExtractedConcepts().get(index));
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children.get(index);
        } else {
            return super.getChild(parent, index);
        }
    }

    //todo comment
    private static class ExtractedConceptAndChildren {
        private ExtractedConcept extractedConcept;
        private List<AbstractExtractClassCandidateRefactoring> children;

        private ExtractedConceptAndChildren(ExtractedConcept extractedConcept) {
            this.extractedConcept = extractedConcept;
            ExtractClassCandidateRefactoring[] refactorings = extractedConcept.getConceptClusters().toArray(new ExtractClassCandidateRefactoring[0]);
            children = new ArrayList<>();
            for (ExtractClassCandidateRefactoring refactoring : refactorings) {
                children.add(new AbstractExtractClassCandidateRefactoring(refactoring));
            }
        }

        @Override
        public String toString() {
            return extractedConcept.toString();
        }

        @Override
        public int hashCode() {
            return extractedConcept.hashCode();
        }
    }
}