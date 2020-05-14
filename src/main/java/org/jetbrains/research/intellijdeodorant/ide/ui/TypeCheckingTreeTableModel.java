package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.openapi.project.Project;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoringGroup;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckRefactoringType.AbstractTypeCheckCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckRefactoringType.AbstractTypeCheckCandidateRefactoringGroup;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckElimination;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckEliminationGroup;

import java.util.List;

public class TypeCheckingTreeTableModel extends AbstractTreeTableModel {
    public TypeCheckingTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames, Project project) {
        super(candidateRefactoringGroups, columnNames, new TypeCheckRefactoringType(project));
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof AbstractTypeCheckCandidateRefactoringGroup) {
            AbstractTypeCheckCandidateRefactoringGroup abstractGroup = (AbstractTypeCheckCandidateRefactoringGroup) o;
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup) abstractGroup.getCandidateRefactoringGroup();

            switch (index) {
                case 0:
                    return group.toString();
                case 2:
                    return Integer.toString(group.getGroupSizeAtSystemLevel());
                case 3:
                    return Double.toString(group.getAverageGroupSizeAtClassLevel());
                case 4:
                    return String.format("%.2f", group.getAverageNumberOfStatementsInGroup());
            }
        }

        if (o instanceof AbstractTypeCheckCandidateRefactoring) {
            AbstractTypeCheckCandidateRefactoring abstractTypeCheckElimination = (AbstractTypeCheckCandidateRefactoring) o;
            TypeCheckElimination typeCheckElimination = (TypeCheckElimination) abstractTypeCheckElimination.getCandidateRefactoring();
            switch (index) {
                case 0:
                    return typeCheckElimination.toString();
                case 1:
                    if (typeCheckElimination.getExistingInheritanceTree() == null) {
                        return IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.name");
                    }
                    return IntelliJDeodorantBundle.message("replace.conditional.with.polymorphism.name");
                case 3:
                    return Integer.toString(typeCheckElimination.getGroupSizeAtClassLevel());
                case 4:
                    return String.format("%.2f", typeCheckElimination.getAverageNumberOfStatements());
            }
        }
        return "";
    }
}