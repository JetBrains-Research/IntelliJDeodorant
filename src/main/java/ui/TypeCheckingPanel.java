package ui;

import com.intellij.analysis.AnalysisScope;
import org.jetbrains.annotations.NotNull;
import ui.abstractrefactorings.TypeCheckRefactoringType;

import java.util.Collections;

/**
 * Panel for Type-State Checking refactorings.
 */
class TypeCheckingPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "type.state.checking.identification.indicator";

    TypeCheckingPanel(@NotNull AnalysisScope scope) {
        super(scope, DETECT_INDICATOR_STATUS_TEXT_KEY,
                new TypeCheckRefactoringType(scope.getProject()),
                new TypeCheckingTreeTableModel(Collections.emptyList(),
                        new String[]{"Type Checking Method", "Refactoring Type", "System-Level Occurrences", "Class-Level Occurrences", "Average #statements per case"},
                        scope.getProject()),
                3); //TODO fix last column values
    }
}