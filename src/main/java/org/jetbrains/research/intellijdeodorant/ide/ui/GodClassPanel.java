package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassRefactoringType.AbstractExtractClassRefactoring;

import java.util.Collections;

/**
 * Panel for God Class Checking refactorings.
 */
public class GodClassPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "god.class.identification.indicator";

    public GodClassPanel(@NotNull AnalysisScope scope) {
        super(scope, DETECT_INDICATOR_STATUS_TEXT_KEY,
                new ExtractClassRefactoringType(),
                new GodClassTableModel(Collections.emptyList(),
                        new String[]{IntelliJDeodorantBundle.message(("god.class.panel.source.class"),
                                IntelliJDeodorantBundle.message("god.class.panel.extractable.concept"),
                                IntelliJDeodorantBundle.message("god.class.panel.source.extracted.members"))}),
                        4);
    }

    @Override
    public void doRefactor(AbstractCandidateRefactoring candidateRefactoring) {
        TransactionGuard.getInstance().submitTransactionAndWait(() -> {
            AbstractExtractClassRefactoring refactoring = (AbstractExtractClassRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);
            GodClassUserInputDialog dialog = new GodClassUserInputDialog(refactoring.getRefactoring());
            dialog.show();
        });
    }
}