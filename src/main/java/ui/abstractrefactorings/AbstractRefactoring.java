package ui.abstractrefactorings;

import com.intellij.psi.PsiClass;
import core.distance.ExtractClassCandidateRefactoring;
import refactoring.MoveMethodRefactoring;
import refactoring.MyExtractClassRefactoring;

public class AbstractRefactoring {
    private PsiClass sourceClass;
    private AbstractRefactoringType refactoringType;
    private Object refactoring;

    public AbstractRefactoring(AbstractRefactoringType refactoringType, Object candidateRefactoring) {
        this.refactoringType = refactoringType;

        switch (refactoringType.getRefactoringType()) {
            case EXTRACT_CLASS:
                ExtractClassCandidateRefactoring extractClassCandidateRefactoring = (ExtractClassCandidateRefactoring) candidateRefactoring;
                this.refactoring = new MyExtractClassRefactoring(extractClassCandidateRefactoring.getSourceFile(),
                        extractClassCandidateRefactoring.getSourceClassTypeDeclaration(),
                        extractClassCandidateRefactoring.getExtractedFieldFragments(),
                        extractClassCandidateRefactoring.getExtractedMethods(),
                        extractClassCandidateRefactoring.getDelegateMethods(),
                        extractClassCandidateRefactoring.getTargetClassName());
            default:
                break; //TODO error
        }
    }

    public void apply() {
        switch (refactoringType.getRefactoringType()) {
            case TYPE_CHECK:
                break; //TODO
            case MOVE_METHOD:
                break; //TODO
            case EXTRACT_CLASS:
                ((MyExtractClassRefactoring) refactoring).apply();
        }
    }
}
