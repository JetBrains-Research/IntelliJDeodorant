package ui.abstractrefactorings;

import com.intellij.openapi.progress.ProgressIndicator;
import core.ast.Standalone;
import core.distance.ExtractClassCandidateGroup;
import core.distance.ExtractClassCandidateRefactoring;
import core.distance.MoveMethodCandidateRefactoring;
import core.distance.ProjectInfo;
import refactoring.MoveMethodRefactoring;
import refactoring.MyExtractClassRefactoring;

import java.util.*;

//TODO comments
public class AbstractRefactoringType {
    private Class<?> candidateRefactoringClass;
    private Class<?> candidateRefactoringGroupClass;
    private Class<?> refactoringClass;

    private REFACTORING_TYPE refactoringType;

    enum REFACTORING_TYPE{
        EXTRACT_CLASS, MOVE_METHOD, TYPE_CHECK
    }

    REFACTORING_TYPE getRefactoringType() {
        return refactoringType;
    }

    public static AbstractRefactoringType getExtractClassRefactoringTypeInstance() {
        return new AbstractRefactoringType(MyExtractClassRefactoring.class);
    }

    private AbstractRefactoringType(Class refactoringClass) {
        this.refactoringClass = refactoringClass;

        if (refactoringClass.equals(MyExtractClassRefactoring.class)) {
            refactoringType = REFACTORING_TYPE.EXTRACT_CLASS;
            candidateRefactoringGroupClass = ExtractClassCandidateGroup.class;
            candidateRefactoringClass = ExtractClassCandidateRefactoring.class;
        } else if (refactoringClass.equals(MoveMethodRefactoring.class)) {
            refactoringType = REFACTORING_TYPE.MOVE_METHOD;
            candidateRefactoringGroupClass = null;
            candidateRefactoringClass = null; //TODO
        } else {
            //TODO ERROR
        }
    }

    public boolean instanceOfCandidateRefactoring(Object o) {
        return (candidateRefactoringClass.isInstance(o));
    }

    public boolean instanceOfRefactoring(Object o) {
        return (refactoringClass.isInstance(o));
    }

    public Set<AbstractCandidateRefactoringGroup> getRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator) {
        List<?> notAbstractCandidateRefactoringGroups = new ArrayList<>();

        switch (refactoringType) {
            case TYPE_CHECK:
                break; //TODO
            case MOVE_METHOD:
                notAbstractCandidateRefactoringGroups = Standalone.getMoveMethodRefactoringOpportunities(projectInfo, indicator);
                break;
            case EXTRACT_CLASS:
                notAbstractCandidateRefactoringGroups = new ArrayList<>(Standalone.getExtractClassRefactoringOpportunities(projectInfo, indicator));
                break;
            default:
                break; //TODO error
        }

        Set<AbstractCandidateRefactoringGroup> refactoringGroups = new HashSet<>();
        for (Object candidateGroup: notAbstractCandidateRefactoringGroups) {
            refactoringGroups.add(new AbstractCandidateRefactoringGroup(this, candidateGroup));
        }

        return refactoringGroups;
    }
}
