package org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract public class RefactoringType {
    public abstract AbstractCandidateRefactoring newCandidateRefactoring(Object candidateRefactoring);

    public abstract boolean instanceOfCandidateRefactoring(Object o);

    public List<AbstractCandidateRefactoringGroup> getRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator) {
        Set<?> notAbstractRefactorOpporunities = getNotAbstractRefactoringOpportunities(projectInfo, indicator);
        List<AbstractCandidateRefactoringGroup> result = new ArrayList<>();
        for (Object candidate : notAbstractRefactorOpporunities) {
            result.add(newAbstractCandidateRefactoringGroup(candidate));
        }

        return result;
    }

    abstract Set<?> getNotAbstractRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator);

    public abstract AbstractRefactoring newAbstractRefactoring(AbstractCandidateRefactoring candidateRefactoring);

    public abstract AbstractCandidateRefactoringGroup newAbstractCandidateRefactoringGroup(Object candidateRefactoringGroup);

    public abstract static class AbstractCandidateRefactoring {
        private Object candidateRefactoring;

        public AbstractCandidateRefactoring(Object candidateRefactoring) {
            this.candidateRefactoring = candidateRefactoring;
        }

        public abstract PsiClass getSourceClass();

        public Object getCandidateRefactoring() {
            return candidateRefactoring;
        }

        public abstract void highlightCode();
    }

    public abstract static class AbstractCandidateRefactoringGroup {
        private List<AbstractCandidateRefactoring> candidates = new ArrayList<>();
        private Object candidateRefactoringGroup;

        public AbstractCandidateRefactoringGroup(RefactoringType refactoringType, Object candidateRefactoringGroup) {
            this.candidateRefactoringGroup = candidateRefactoringGroup;
            List<?> notAbstractCandidates = getNotAbstractCandidates();

            for (Object candidate : notAbstractCandidates) {
                candidates.add(refactoringType.newCandidateRefactoring(candidate));
            }
        }

        protected abstract List<?> getNotAbstractCandidates();

        public List<AbstractCandidateRefactoring> getCandidates() {
            return candidates;
        }

        public Object getCandidateRefactoringGroup() {
            return candidateRefactoringGroup;
        }

        @Override
        public int hashCode() {
            return candidateRefactoringGroup.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return candidateRefactoringGroup.equals(obj);
        }
    }

    abstract public static class AbstractRefactoring {
        abstract public void apply();
    }
}
