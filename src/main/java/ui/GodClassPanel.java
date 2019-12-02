package ui;

import com.intellij.analysis.AnalysisScope;
import org.jetbrains.annotations.NotNull;

import ui.abstractrefactorings.ExtractClassRefactoringType;

import java.util.Collections;

/**
 * Panel for Type-State Checking refactorings.
 */
class GodClassPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "god.class.identification.indicator";

    GodClassPanel(@NotNull AnalysisScope scope) {
        super(scope, DETECT_INDICATOR_STATUS_TEXT_KEY,
                new ExtractClassRefactoringType(),
                new GodClassTableModel(Collections.emptyList(),
                        new String[]{"Source Class/General Concept", "Extractable Concept", "Source/Extracted accessed members"}),
                4); //TODO fix last column values
    }
}