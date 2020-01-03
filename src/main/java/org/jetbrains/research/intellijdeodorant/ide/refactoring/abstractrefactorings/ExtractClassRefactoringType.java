package ui.abstractrefactorings;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import core.ast.Standalone;
import core.distance.ExtractClassCandidateGroup;
import core.distance.ExtractClassCandidateRefactoring;
import core.distance.ProjectInfo;
import refactoring.ExtractClassRefactoring;
import ui.AbstractRefactoringPanel;

import java.util.List;
import java.util.Set;

public class ExtractClassRefactoringType extends RefactoringType {
    @Override
    public AbstractCandidateRefactoring newCandidateRefactoring(Object candidateRefactoring) {
        return new AbstractExtractClassCandidateRefactoring((ExtractClassCandidateRefactoring) candidateRefactoring);
    }

    @Override
    public boolean instanceOfCandidateRefactoring(Object o) {
        return o instanceof AbstractExtractClassCandidateRefactoring;
    }

    @Override
    Set<?> getNotAbstractRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator) {
        return Standalone.getExtractClassRefactoringOpportunities(projectInfo, indicator);
    }

    @Override
    public AbstractRefactoring newAbstractRefactoring(AbstractCandidateRefactoring candidateRefactoring) {
        return new AbstractExtractClassRefactoring((ExtractClassCandidateRefactoring) candidateRefactoring.getCandidateRefactoring());
    }

    @Override
    public AbstractCandidateRefactoringGroup newAbstractCandidateRefactoringGroup(Object candidateRefactoringGroup) {
        return new AbstractExtractClassCandidateRefactoringGroup(candidateRefactoringGroup);
    }

    public static class AbstractExtractClassCandidateRefactoring extends AbstractCandidateRefactoring {
        private PsiClass sourceClass;

        public AbstractExtractClassCandidateRefactoring(ExtractClassCandidateRefactoring candidateRefactoring) {
            super(candidateRefactoring);
            this.sourceClass = candidateRefactoring.getSourceClassTypeDeclaration();
        }

        @Override
        public PsiClass getSourceClass() {
            return sourceClass;
        }

        @Override
        public void highlightCode() {
            ExtractClassCandidateRefactoring refactoring = (ExtractClassCandidateRefactoring) getCandidateRefactoring();

            AbstractRefactoringPanel.removeHighlighters(getSourceClass().getProject());

            boolean openInEditor = true; //open only first element

            for (PsiMethod psiMethod : refactoring.getExtractedMethods())  {
                AbstractRefactoringPanel.highlightMethod(psiMethod, new AnalysisScope(psiMethod.getProject()), openInEditor);
                openInEditor = false;
            }

            for (PsiField psiField : refactoring.getExtractedFieldFragments())  {
                AbstractRefactoringPanel.highlightField(psiField, new AnalysisScope(psiField.getProject()), openInEditor);
                openInEditor = false;
            }
        }

        @Override
        public String toString() {
            return ((ExtractClassCandidateRefactoring) getCandidateRefactoring()).getTopics().toString();
        }
    }

    public class AbstractExtractClassCandidateRefactoringGroup extends AbstractCandidateRefactoringGroup {
        public AbstractExtractClassCandidateRefactoringGroup(Object candidateRefactoringGroup) {
            super(ExtractClassRefactoringType.this, candidateRefactoringGroup);
        }

        @Override
        protected List<?> getNotAbstractCandidates() {
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) getCandidateRefactoringGroup();
            return group.getCandidates();
        }

        @Override
        public String toString() {
            return ((ExtractClassCandidateGroup) getCandidateRefactoringGroup()).getSource();
        }
    }

    public static class AbstractExtractClassRefactoring extends AbstractRefactoring {
        private ExtractClassRefactoring refactoring;

        public AbstractExtractClassRefactoring(ExtractClassCandidateRefactoring extractClassCandidateRefactoring) {
            this.refactoring = new ExtractClassRefactoring(extractClassCandidateRefactoring.getSourceFile(),
                extractClassCandidateRefactoring.getSourceClassTypeDeclaration(),
                extractClassCandidateRefactoring.getExtractedFieldFragments(),
                extractClassCandidateRefactoring.getExtractedMethods(),
                extractClassCandidateRefactoring.getDelegateMethods(),
                extractClassCandidateRefactoring.getTargetClassName());
        }

        @Override
        public void apply() {
            refactoring.apply();
        }

        public ExtractClassRefactoring getRefactoring() {
            return refactoring;
        }
    }
}
