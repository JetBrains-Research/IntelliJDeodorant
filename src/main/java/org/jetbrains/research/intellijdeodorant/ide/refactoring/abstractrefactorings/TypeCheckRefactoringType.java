package org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.jetbrains.research.intellijdeodorant.JDeodorantFacade;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.*;
import org.jetbrains.research.intellijdeodorant.ide.ui.abstractpanel.AbstractRefactoringPanel;
import org.jetbrains.research.intellijdeodorant.ide.ui.functionalinterfaces.QuadriFunction;

import java.util.List;
import java.util.Set;

public class TypeCheckRefactoringType extends RefactoringType {
    private AnalysisScope scope;

    public TypeCheckRefactoringType(Project project) {
        scope = new AnalysisScope(project);
    }

    @Override
    public AbstractCandidateRefactoring newCandidateRefactoring(Object candidateRefactoring) {
        return new AbstractTypeCheckCandidateRefactoring((TypeCheckElimination) candidateRefactoring);
    }

    @Override
    public boolean instanceOfCandidateRefactoring(Object o) {
        return o instanceof AbstractTypeCheckCandidateRefactoring;
    }

    @Override
    Set<?> getNotAbstractRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator) {
        return JDeodorantFacade.getTypeCheckEliminationRefactoringOpportunities(projectInfo, indicator);
    }

    @Override
    public AbstractRefactoring newAbstractRefactoring(AbstractCandidateRefactoring candidateRefactoring) {
        return new AbstractTypeCheckRefactoring((TypeCheckElimination) candidateRefactoring.getCandidateRefactoring());
    }

    @Override
    public AbstractCandidateRefactoringGroup newAbstractCandidateRefactoringGroup(Object candidateRefactoringGroup) {
        return new AbstractTypeCheckCandidateRefactoringGroup(candidateRefactoringGroup);
    }

    public class AbstractTypeCheckCandidateRefactoring extends AbstractCandidateRefactoring {
        private PsiClass sourceClass;

        public AbstractTypeCheckCandidateRefactoring(TypeCheckElimination candidateRefactoring) {
            super(candidateRefactoring);
            this.sourceClass = candidateRefactoring.getTypeCheckClass();
        }

        @Override
        public PsiClass getSourceClass() {
            return sourceClass;
        }

        @Override
        public void highlightCode() {
            TypeCheckElimination refactoring = (TypeCheckElimination) getCandidateRefactoring();
            AbstractRefactoringPanel.removeHighlighters(getSourceClass().getProject());

            AbstractRefactoringPanel.highlightStatement(
                    refactoring.getTypeCheckMethod(),
                    scope,
                    refactoring.getTypeCheckCodeFragment(),
                    true
            );
        }

        @Override
        public String toString() {
            return getCandidateRefactoring().toString();
        }
    }

    public class AbstractTypeCheckCandidateRefactoringGroup extends AbstractCandidateRefactoringGroup {
        public AbstractTypeCheckCandidateRefactoringGroup(Object candidateRefactoringGroup) {
            super(TypeCheckRefactoringType.this, candidateRefactoringGroup);
        }

        @Override
        protected List<?> getNotAbstractCandidates() {
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup) getCandidateRefactoringGroup();
            return group.getCandidates();
        }

        @Override
        public String toString() {
            return getCandidateRefactoringGroup().toString();
        }
    }

    public class AbstractTypeCheckRefactoring extends AbstractRefactoring {
        private PolymorphismRefactoring refactoring;

        public AbstractTypeCheckRefactoring(TypeCheckElimination typeCheckElimination) {
            PsiClass sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
            PsiFile sourceFile = sourceTypeDeclaration.getContainingFile();
            QuadriFunction<PsiFile, Project, PsiClass, TypeCheckElimination, PolymorphismRefactoring> constructor;
            if (typeCheckElimination.getExistingInheritanceTree() == null) {
                constructor = ReplaceTypeCodeWithStateStrategy::new;
            } else {
                constructor = ReplaceConditionalWithPolymorphism::new;
            }

            refactoring = constructor.apply(
                    sourceFile,
                    scope.getProject(),
                    sourceTypeDeclaration,
                    typeCheckElimination
            );
        }

        @Override
        public void apply() {
            refactoring.apply();
        }

        public PolymorphismRefactoring getRefactoring() {
            return refactoring;
        }
    }
}
