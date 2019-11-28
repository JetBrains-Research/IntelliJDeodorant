package ui;

import core.distance.ExtractClassCandidateGroup;
import refactoring.MyExtractClassRefactoring;
import ui.abstractrefactorings.AbstractCandidateRefactoringGroup;

import java.util.List;

public class GodClassTableModel extends AbstractTreeTableModel {
    public GodClassTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(candidateRefactoringGroups, columnNames);
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (getRefactoringType().instanceOfCandidateRefactoring(o)) {
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) ((AbstractCandidateRefactoringGroup) o).getCandidateRefactoringGroup();
            switch(index){
                case 0:
                    return group.toString();
                case 2:
                    return Integer.toString(group.getCandidates().size());
                case 3:
                    return group.getSource();
            }
        }

        if (getRefactoringType().instanceOfRefactoring(o)) {
            MyExtractClassRefactoring extractClassRefactoring = (MyExtractClassRefactoring) o;
            switch(index) {
                case 0:
                    return extractClassRefactoring.toString();
                case 1:
                    return "Replace Conditional with Polymorphism";
                case 3:
                    return extractClassRefactoring.getExtractedTypeName();
            }
        }

        return "";
    }
}
