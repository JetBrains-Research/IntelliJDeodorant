package ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.annotations.NotNull;
import ui.abstractrefactorings.RefactoringType;
import ui.abstractrefactorings.TypeCheckRefactoringType;

import java.util.Collections;

/**
 * Panel for Type-State Checking refactorings.
 */
class TypeCheckingPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "type.state.checking.identification.indicator";
    private static final String[] COLUMN_NAMES = new String[]{
            "Type Checking Method",
            "Refactoring Type",
            "System-Level Occurrences",
            "Class-Level Occurrences",
            "Average #statements per case"
    };
    private static final int REFACTOR_DEPTH = 3;

    TypeCheckingPanel(@NotNull AnalysisScope scope) {
        super(  scope,
                DETECT_INDICATOR_STATUS_TEXT_KEY,
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
    protected void doRefactor(RefactoringType.AbstractCandidateRefactoring candidateRefactoring) {
        removeHighlighters(scope.getProject());
        disableRefactoringsTable(true);
        TransactionGuard.getInstance().submitTransactionAndWait(() -> {
            RefactoringType.AbstractRefactoring refactoring =
                    getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);
            WriteCommandAction.runWriteCommandAction(scope.getProject(), refactoring::apply);
        });
    }
}