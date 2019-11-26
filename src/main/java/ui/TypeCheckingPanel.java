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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refactoring.*;
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

import static core.ast.Standalone.getTypeCheckEliminationRefactoringOpportunities;

/**
 * Panel for Type-State Checking refactorings.
 */
class TypeCheckingPanel extends JPanel {
    private static final String REFACTOR_BUTTON_TEXT_KEY = "refactor.button";
    private static final String REFRESH_BUTTON_TEXT_KEY = "refresh.button";
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "type.state.checking.identification.indicator";
    private static final String REFRESH_NEEDED_TEXT = "type.state.checking.refresh.needed.text";

    @NotNull
    private final AnalysisScope scope;
    private TypeCheckingTreeTableModel model = new TypeCheckingTreeTableModel(Collections.emptyList());
    private final TreeTable treeTable = new TreeTable(model);
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private JScrollPane scrollPane = new JBScrollPane();
    private JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message(REFRESH_NEEDED_TEXT),
            SwingConstants.CENTER
    );

    TypeCheckingPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        disableRefactoringsTable();
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
    private void disableRefactoringsTable() {
        scrollPane.setVisible(true);
        treeTable.getTree().setSelectionPath(null);
        scrollPane.setViewportView(refreshLabel);
    }

    /**
     * Creates scrollable treeTable panel and adds listeners.
     *
     * @return treeTable which can display available refactorings.
     */
    private JScrollPane createTablePanel() {
        treeTable.setRootVisible(false);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addMouseListener((DoubleClickListener) this::highlightTypeCheckingCode);
        treeTable.addKeyListener((EnterKeyListener) this::highlightTypeCheckingCode);
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
        if (selectedPath.getPathCount() == 3) {
            TypeCheckElimination computationSlice = (TypeCheckElimination) selectedPath.getLastPathComponent();
            disableRefactoringsTable();
            TransactionGuard.getInstance().submitTransactionAndWait((doExtract(computationSlice)));
        }
    }

    /**
     * Enables Refactor button only if a suggestion is selected.
     */
    private void enableRefactorButtonIfAnySelected() {
        boolean isAnySuggestionSelected = false;
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == 3) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof TypeCheckElimination) {
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
                IntelliJDeodorantBundle.message(DETECT_INDICATOR_STATUS_TEXT_KEY), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    Set<TypeCheckEliminationGroup> candidates = getTypeCheckEliminationRefactoringOpportunities(projectInfo, indicator);
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
    private void highlightTypeCheckingCode() {
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == 3) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof TypeCheckElimination) {
                TypeCheckElimination typeCheckElimination = (TypeCheckElimination) o;
                highlightStatement(
                        typeCheckElimination.getTypeCheckMethod(),
                        scope,
                        typeCheckElimination.getTypeCheckCodeFragment()
                );
            }
        }
    }

    /**
     * Returns Runnable which performs specified refactoring.
     */
    private Runnable doExtract(TypeCheckElimination typeCheckElimination) {
        return () -> {
            PsiClass sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
            PsiFile sourceFile = sourceTypeDeclaration.getContainingFile();
            QuadriFunction<PsiFile, Project, PsiClass, TypeCheckElimination, PolymorphismRefactoring> constructor;
            if(typeCheckElimination.getExistingInheritanceTree() == null) {
                constructor = ReplaceTypeCodeWithStateStrategy::new;
            } else {
                constructor = ReplaceConditionalWithPolymorphism::new;
            }
            PolymorphismRefactoring refactoring = constructor.apply(
                    sourceFile,
                    scope.getProject(),
                    sourceTypeDeclaration,
                    typeCheckElimination
            );
            WriteCommandAction.runWriteCommandAction(scope.getProject(), refactoring::apply);
        };
    }

    /**
     * Opens definition of method and highlights specified statement.
     */
    private static void highlightStatement(@Nullable PsiMethod sourceMethod,
                                           AnalysisScope scope,
                                           PsiStatement statement) {
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
                EditorHelper.openInEditor(statement);
                Editor editor = FileEditorManager.getInstance(sourceMethod.getProject()).getSelectedTextEditor();
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
                editor.getMarkupModel().removeAllHighlighters();
                editor.getMarkupModel().addRangeHighlighter(
                        statement.getTextRange().getStartOffset(),
                        statement.getTextRange().getEndOffset(),
                        HighlighterLayer.SELECTION,
                        attributes,
                        HighlighterTargetArea.EXACT_RANGE
                );
            }
        }.queue();
    }
}