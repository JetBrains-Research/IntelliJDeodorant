package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassRefactoring;

import java.util.Collections;

/**
 * Panel for God Class Checking refactorings.
 */
public class GodClassPanel extends AbstractRefactoringPanel {
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "god.class.identification.indicator";

    private static final String[] COLUMN_NAMES = new String[]{IntelliJDeodorantBundle.message("god.class.panel.source.class"),
            IntelliJDeodorantBundle.message("god.class.panel.extractable.concept"),
            IntelliJDeodorantBundle.message("god.class.panel.source.extracted.members")};

    private static final int REFACTOR_DEPTH = 4;

    public GodClassPanel(@NotNull AnalysisScope scope) {
        super(scope,
                DETECT_INDICATOR_STATUS_TEXT_KEY,
                new ExtractClassRefactoringType(),
                new GodClassTreeTableModel(Collections.emptyList(), COLUMN_NAMES),
                REFACTOR_DEPTH);
    }

    @Override
    protected void doRefactor(AbstractCandidateRefactoring candidateRefactoring) {
        AbstractExtractClassRefactoring abstractRefactoring = (AbstractExtractClassRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);

        TransactionGuard.getInstance().submitTransactionAndWait(() -> {
            removeHighlighters(scope.getProject());
            showRefreshingProposal();
            GodClassUserInputDialog dialog = new GodClassUserInputDialog(abstractRefactoring.getRefactoring());
            dialog.show();
        });
    }
}