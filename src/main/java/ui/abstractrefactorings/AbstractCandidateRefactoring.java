package ui.abstractrefactorings;

import com.intellij.psi.PsiClass;
import core.distance.ExtractClassCandidateRefactoring;

public class AbstractCandidateRefactoring {
    private PsiClass sourceClass;
    private AbstractRefactoringType refactoringType;
    private Object candidateRefactoring;

    public AbstractCandidateRefactoring(AbstractRefactoringType refactoringType, Object candidateRefactoring) {
        this.refactoringType = refactoringType;
        this.candidateRefactoring = candidateRefactoring;

        switch (refactoringType.getRefactoringType()) {
            case MOVE_METHOD:
                break; //TODO
            case EXTRACT_CLASS:
                this.sourceClass = ((ExtractClassCandidateRefactoring) candidateRefactoring).getSourceClassTypeDeclaration();
                break;
            case TYPE_CHECK:
                break; //TODO
        }
    }

    public PsiClass getSourceClass() {
        return sourceClass;
    }

    public void highlightCode() {
        //TODO
    }
}
