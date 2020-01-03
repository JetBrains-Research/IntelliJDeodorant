package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings.ExtractClassRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings.ExtractClassRefactoringType.AbstractExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings.RefactoringType.AbstractCandidateRefactoring;

import java.util.Collections;

/**
 * Panel for Type-State Checking refactorings.
 */
public class GodClassPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "god.class.identification.indicator";

    public GodClassPanel(@NotNull AnalysisScope scope) {
        super(scope, DETECT_INDICATOR_STATUS_TEXT_KEY,
                new ExtractClassRefactoringType(),
                new GodClassTableModel(Collections.emptyList(),
                        new String[]{"Source Class/General Concept", "Extractable Concept", "Source/Extracted accessed members"}),
                4); //TODO fix last column values
    }

    @Override
    public void doRefactor(AbstractCandidateRefactoring candidateRefactoring) {
        TransactionGuard.getInstance().submitTransactionAndWait(() -> {
            AbstractExtractClassRefactoring refactoring = (AbstractExtractClassRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);
            ui.GodClassUserInputDialog dialog = new ui.GodClassUserInputDialog(refactoring.getRefactoring());
            dialog.show();
        });
    }
}