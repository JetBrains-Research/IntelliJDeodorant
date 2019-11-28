package ui.abstractrefactorings;

import core.distance.ExtractClassCandidateGroup;
import core.distance.ExtractClassCandidateRefactoring;

import java.util.ArrayList;
import java.util.List;

public class AbstractCandidateRefactoringGroup {
    private AbstractRefactoringType refactoringType;
    private List<AbstractCandidateRefactoring> candidates = new ArrayList<>();
    private Object candidateRefactoringGroup;

    public AbstractCandidateRefactoringGroup(AbstractRefactoringType refactoringType, Object candidateRefactoringGroup) {
        this.refactoringType = refactoringType;

        this.candidateRefactoringGroup = candidateRefactoringGroup;
        List<?> notAbstractCandidates = new ArrayList<>();

        switch (refactoringType.getRefactoringType()) {
            case MOVE_METHOD:
                break; //TODO
            case TYPE_CHECK:
                break; //TODO
            case EXTRACT_CLASS:
                notAbstractCandidates = ((ExtractClassCandidateGroup) candidateRefactoringGroup).getCandidates();
        }

        ExtractClassCandidateRefactoring extractClassCandidateRefactoring;

        for (Object candidate : notAbstractCandidates) {
            candidates.add(new AbstractCandidateRefactoring(refactoringType, candidate));
        }
    }

    public List<AbstractCandidateRefactoring> getCandidates() {
        return candidates;
    }

    public Object getCandidateRefactoringGroup() {
        return candidateRefactoringGroup;
    }
}
