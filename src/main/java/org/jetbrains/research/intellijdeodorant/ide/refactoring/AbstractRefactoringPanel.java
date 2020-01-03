package ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import core.distance.ExtractClassCandidateRefactoring;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refactoring.*;
import ui.abstractrefactorings.RefactoringType;
import ui.abstractrefactorings.RefactoringType.AbstractCandidateRefactoring;
import ui.abstractrefactorings.RefactoringType.AbstractRefactoring;
import ui.functionalinterfaces.DoubleClickListener;
import ui.functionalinterfaces.ElementSelectionListener;
import ui.functionalinterfaces.EnterKeyListener;
import ui.functionalinterfaces.QuadriFunction;
import utils.IntelliJDeodorantBundle;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Panel for Type-State Checking refactorings.
 */
public class AbstractRefactoringPanel extends JPanel {
    private static final String REFACTOR_BUTTON_TEXT_KEY = "refactor.button";
    private static final String REFRESH_BUTTON_TEXT_KEY = "refresh.button";
    private String detect_indicator_status_text_key; //TODO "type.state.checking.identification.indicator";
    private static final String REFRESH_NEEDED_TEXT = "refresh.needed.text";

    @NotNull
    private final AnalysisScope scope;
    private AbstractTreeTableModel model;
    private final TreeTable treeTable;
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private JScrollPane scrollPane = new JBScrollPane();
    private JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message(REFRESH_NEEDED_TEXT),
            SwingConstants.CENTER
    );

    private RefactoringType refactoringType;
    private int refactorDepth;

    AbstractRefactoringPanel(@NotNull AnalysisScope scope, String detect_indicator_status_text_key, RefactoringType refactoringType, AbstractTreeTableModel model, int refactorDepth) {
        this.scope = scope;
        this.detect_indicator_status_text_key = detect_indicator_status_text_key;
        this.refactoringType = refactoringType;
        this.model = model;
        this.treeTable =  new TreeTable(model);
        this.refactorDepth = refactorDepth;
        setLayout(new BorderLayout());
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        disableRefactoringsTable(true);
    }

    /**
     * Shows treeTable with available refactorings.
     */
    private void enableRefactoringsTable() {
        scrollPane.setVisible(true);
        treeTable.getTree().setSelectionPath(null);
        model.reload();
        scrollPane.setViewportView(treeTable);
    }

    /**
     * Hides treeTable with refactorings and shows text which proposes refreshing available refactorings.
     */
    private void disableRefactoringsTable(boolean hideTree) {
        scrollPane.setVisible(true);
        treeTable.getTree().setSelectionPath(null);
        if (hideTree) {
            scrollPane.setViewportView(refreshLabel);
        }
    }

    /**
     * Creates scrollable treeTable panel and adds listeners.
     *
     * @return treeTable which can display available refactorings.
     */
    private JScrollPane createTablePanel() {
        treeTable.setRootVisible(false);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addMouseListener((DoubleClickListener) this::highlightCode);
        treeTable.addKeyListener((EnterKeyListener) this::highlightCode);
        treeTable.getTree().addTreeSelectionListener((ElementSelectionListener) this::enableRefactorButtonIfAnySelected);
        scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        return scrollPane;
    }

    /**
     * Creates button panel and adds action listeners for buttons.
     *
     * @return panel with buttons.
     */
    private JComponent createButtonPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel buttonPanel = new JBPanel<JBPanel<JBPanel>>();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        doRefactorButton.setText(IntelliJDeodorantBundle.message(REFACTOR_BUTTON_TEXT_KEY));
        doRefactorButton.setEnabled(false);
        doRefactorButton.addActionListener(e -> refactorSelected());
        buttonPanel.add(doRefactorButton);

        refreshButton.setText(IntelliJDeodorantBundle.message(REFRESH_BUTTON_TEXT_KEY));
        refreshButton.addActionListener(l -> refreshPanel());
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * Preforms selected refactoring.
     */
    private void refactorSelected() {
        TreePath selectedPath = treeTable.getTree().getSelectionPath();
        if (selectedPath.getPathCount() == refactorDepth) {
            AbstractCandidateRefactoring computationSlice = (AbstractCandidateRefactoring) selectedPath.getLastPathComponent();
            disableRefactoringsTable(false);
            doRefactor(computationSlice);
        }
    }

    //TODO comment
    void doRefactor(AbstractCandidateRefactoring computationSlice) {
        TransactionGuard.getInstance().submitTransactionAndWait((doExtract(computationSlice)));
    }

    /**
     * Enables Refactor button only if a suggestion is selected.
     */
    private void enableRefactorButtonIfAnySelected() {
        boolean isAnySuggestionSelected = false;
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            Object o = selectedPath.getLastPathComponent();
            if (refactoringType.instanceOfCandidateRefactoring(o)) {
                isAnySuggestionSelected = true;
            }
        }
        doRefactorButton.setEnabled(isAnySuggestionSelected);
    }

    /**
     * Refreshes the panel with suggestions.
     */
    private void refreshPanel() {
        Editor editor = FileEditorManager.getInstance(scope.getProject()).getSelectedTextEditor();
        if (editor != null) {
            editor.getMarkupModel().removeAllHighlighters();
        }
        doRefactorButton.setEnabled(false);
        scrollPane.setVisible(false);
        calculateRefactorings();
    }

    /**
     * Calculates suggestions for whole project.
     */
    private void calculateRefactorings() {
        ProjectInfo projectInfo = new ProjectInfo(scope.getProject());

        final Task.Backgroundable backgroundable = new Task.Backgroundable(scope.getProject(),
                IntelliJDeodorantBundle.message(detect_indicator_status_text_key), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    Set<RefactoringType.AbstractCandidateRefactoringGroup> candidates =
                            refactoringType.getRefactoringOpportunities(projectInfo, indicator);
                    model.setEliminationGroups(new ArrayList<>(candidates));
                    ApplicationManager.getApplication().invokeLater(() -> enableRefactoringsTable());
                });
            }
        };
        ProgressManager.getInstance().run(backgroundable);
    }

    /**
     * Highlights type-checking code fragment.
     */
    private void highlightCode() {
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            Object o = selectedPath.getLastPathComponent();
            if (refactoringType.instanceOfCandidateRefactoring(o)) {
                AbstractCandidateRefactoring refactoring = (AbstractCandidateRefactoring) o;
                refactoring.highlightCode();
            }
        }
    }

    AbstractRefactoring getAbstractRefactoringFromAbstractCandidateRefactoring(AbstractCandidateRefactoring candidate) {
        PsiClass sourceTypeDeclaration = candidate.getSourceClass();
        PsiFile sourceFile = sourceTypeDeclaration.getContainingFile();
        return refactoringType.newAbstractRefactoring(candidate);
    }

    /**
     * Returns Runnable which performs specified refactoring.
     */
    private Runnable doExtract(AbstractCandidateRefactoring candidateRefactoring) {
        return () -> {
            AbstractRefactoring refactoring = getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);
            WriteCommandAction.runWriteCommandAction(scope.getProject(), refactoring::apply);
        };
    }

    /**
     * Opens definition of method and highlights specified element in the method.
     */
    public static void highlightStatement(@Nullable PsiMethod sourceMethod,
                                           AnalysisScope scope,
                                           PsiElement statement, boolean openInEditor) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
            }

            @Override
            public void onSuccess() {
                if (sourceMethod == null || !statement.isValid()) {
                    return;
                }
                highlightPsiElement(statement, openInEditor);
            }
        }.queue();
    }

    //TODO
    public static void highlightMethod(@Nullable PsiMethod sourceMethod,
                                          AnalysisScope scope, boolean openInEditor) {
        highlightStatement(sourceMethod, scope, sourceMethod, openInEditor);
    }

    //TODO
    public static void highlightField(@Nullable PsiField sourceField, AnalysisScope scope, boolean openInEditor) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
            }

            @Override
            public void onSuccess() {
                if (sourceField == null || !sourceField.isValid()) {
                    return;
                }

                highlightPsiElement(sourceField, openInEditor);
            }
        }.queue();
    }

    //TODO
    private static void highlightPsiElement(PsiElement psiElement, boolean openInEditor) {
        if (openInEditor) {
            EditorHelper.openInEditor(psiElement);
        }

        Editor editor = FileEditorManager.getInstance(psiElement.getProject()).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        Color foregroundColor = editor.getColorsScheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR);
        Color backgroundColor = new JBColor(new Color(84, 168, 78), new Color(16, 105, 15));
        TextAttributes attributes = new TextAttributes(foregroundColor,
                backgroundColor,
                null,
                null,
                0
        );

        // TODO remove? editor.getMarkupModel().removeAllHighlighters();
        editor.getMarkupModel().addRangeHighlighter(
                psiElement.getTextRange().getStartOffset(),
                psiElement.getTextRange().getEndOffset(),
                HighlighterLayer.SELECTION,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
        );
    }

    public static void removeHighlighters(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        editor.getMarkupModel().removeAllHighlighters();
    }
}