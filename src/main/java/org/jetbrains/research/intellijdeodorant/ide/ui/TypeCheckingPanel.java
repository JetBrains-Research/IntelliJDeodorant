package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.fus.collectors.IntelliJDeodorantCounterCollector;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckRefactoringType.AbstractTypeCheckRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.ReplaceTypeCodeWithStateStrategy;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.PolymorphismRefactoring;

import java.util.Collections;

/**
 * Panel for Type-State Checking refactorings.
 */
class TypeCheckingPanel extends AbstractRefactoringPanel {
    private static final String[] COLUMN_NAMES = new String[]{
            IntelliJDeodorantBundle.message("type.state.checking.panel.column.method"),
            IntelliJDeodorantBundle.message("type.state.checking.panel.column.refactoring.type"),
            IntelliJDeodorantBundle.message("type.state.checking.panel.column.system.occurrences"),
            IntelliJDeodorantBundle.message("type.state.checking.panel.column.class.occurrences"),
            IntelliJDeodorantBundle.message("type.state.checking.panel.column.average.statements")
    };
    private static final int REFACTOR_DEPTH = 3;

    TypeCheckingPanel(@NotNull AnalysisScope scope) {
        super(scope,
                "type.state.checking.identification.indicator",
                new TypeCheckRefactoringType(scope.getProject()),
                new TypeCheckingTreeTableModel(
                        Collections.emptyList(),
                        COLUMN_NAMES,
                        scope.getProject()
                ),
                REFACTOR_DEPTH
        );
    }

    @Override
    protected void logFound(Project project, Integer total) {
        IntelliJDeodorantCounterCollector.getInstance().refactoringFound(project, "replace.conditional.type", total);
    }

    @Override
    protected void doRefactor(AbstractCandidateRefactoring candidateRefactoring) {
        AbstractTypeCheckRefactoring abstractRefactoring =
                (AbstractTypeCheckRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);
        PolymorphismRefactoring refactoring = abstractRefactoring.getRefactoring();

        Project project = scope.getProject();
        IntelliJDeodorantCounterCollector.getInstance().typeStateCheckingRefactoringApplied(project,
                refactoring.getTypeCheckElimination().getAverageNumberOfStatements());

        Runnable applyRefactoring = () -> {
            removeHighlighters(project);
            showRefreshingProposal();
            WriteCommandAction.runWriteCommandAction(scope.getProject(), refactoring::apply);
        };

        if (refactoring instanceof ReplaceTypeCodeWithStateStrategy) {
            TransactionGuard.getInstance().submitTransactionAndWait(() -> new ReplaceTypeCodeWithStateStrategyDialog(
                    (ReplaceTypeCodeWithStateStrategy) refactoring,
                    applyRefactoring
            ).show());
        } else {
            applyRefactoring.run();
        }
    }

}
