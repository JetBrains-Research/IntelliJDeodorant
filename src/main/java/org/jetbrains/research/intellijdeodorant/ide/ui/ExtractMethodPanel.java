package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSliceGroup;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PDGNode;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod.ExtractMethodCandidateGroup;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod.MyExtractMethodProcessor;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.ElementSelectionListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.EnterKeyListener;
import org.jetbrains.research.intellijdeodorant.utils.ExportResultsUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.jetbrains.research.intellijdeodorant.JDeodorantFacade.getExtractMethodRefactoringOpportunities;
import static org.jetbrains.research.intellijdeodorant.ide.ui.AbstractRefactoringPanel.expandOrCollapsePath;
import static org.jetbrains.research.intellijdeodorant.ide.ui.AbstractRefactoringPanel.runAfterCompilationCheck;
import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.isChild;

/**
 * Panel for Extract Method refactoring.
 */
class ExtractMethodPanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    private final ExtractMethodTreeTableModel treeTableModel = new ExtractMethodTreeTableModel();
    private final TreeTable treeTable = new TreeTable(treeTableModel);
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private JScrollPane scrollPane = new JBScrollPane();
    private final JButton exportButton = new JButton();
    private final JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );

    ExtractMethodPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates scrollable table panel and adds mouse listener.
     *
     * @return result panel.
     */
    private JScrollPane createTablePanel() {
        treeTable.setRootVisible(false);
        treeTable.setTreeCellRenderer(new ExtractMethodCandidatesTreeCellRenderer());
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(800);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addMouseListener((DoubleClickListener) this::openMethodDefinition);
        treeTable.addKeyListener((EnterKeyListener) this::openMethodDefinition);
        treeTable.getTree().addTreeSelectionListener((ElementSelectionListener) this::enableRefactorButtonIfAnySelected);
        refreshLabel.setForeground(JBColor.GRAY);
        scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        scrollPane.setViewportView(refreshLabel);
        scrollPane.setVisible(true);
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

        doRefactorButton.setText(IntelliJDeodorantBundle.message("refactor.button"));
        doRefactorButton.addActionListener(e -> refactorSelected());
        doRefactorButton.setEnabled(false);
        buttonPanel.add(doRefactorButton);

        refreshButton.setText(IntelliJDeodorantBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        refreshButton.setEnabled(true);
        buttonPanel.add(refreshButton);

        exportButton.setText(IntelliJDeodorantBundle.message("export"));
        exportButton.addActionListener(e -> ExportResultsUtil.export(getAvailableRefactoringSuggestions(), panel));
        exportButton.setEnabled(false);
        buttonPanel.add(exportButton);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * Filters available refactorings suggestions from refactoring list.
     *
     * @return list of available refactorings suggestions.
     */
    private List<ExtractMethodCandidateGroup> getAvailableRefactoringSuggestions() {
        return treeTableModel.getCandidateRefactoringGroups().stream()
                .filter(extractMethodCandidateGroup -> extractMethodCandidateGroup.getCandidates()
                        .stream()
                        .allMatch(ASTSlice::areSliceStatementsValid))
                .collect(toList());
    }

    /**
     * Preforms the selected refactoring.
     */
    private void refactorSelected() {
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof ASTSlice) {
                TransactionGuard.getInstance().submitTransactionAndWait(doExtract((ASTSlice) o));
            }
        }
    }

    /**
     * Enables Refactor button only if any suggestion is selected.
     */
    private void enableRefactorButtonIfAnySelected() {
        boolean isAnySuggestionSelected = false;
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof ASTSlice) {
                ASTSlice slice = (ASTSlice) o;
                if (slice.areSliceStatementsValid()) {
                    isAnySuggestionSelected = true;
                }
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
        exportButton.setEnabled(false);
        refreshButton.setEnabled(false);
        scrollPane.setVisible(false);
        calculateRefactorings();
    }

    /**
     * Calculates suggestions for whole project.
     */
    private void calculateRefactorings() {
        ProjectInfo projectInfo = new ProjectInfo(scope.getProject());

        final Task.Backgroundable backgroundable = new Task.Backgroundable(scope.getProject(),
                IntelliJDeodorantBundle.message("long.method.detect.indicator.status"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    Set<ASTSliceGroup> candidates = getExtractMethodRefactoringOpportunities(projectInfo, indicator);
                    final List<ExtractMethodCandidateGroup> extractMethodCandidateGroups = candidates.stream().filter(Objects::nonNull)
                            .map(sliceGroup ->
                                    sliceGroup.getCandidates().stream()
                                            .filter(c -> canBeExtracted(c))
                                            .collect(toSet()))
                            .filter(set -> !set.isEmpty())
                            .map(ExtractMethodCandidateGroup::new)
                            .collect(toList());
                    treeTableModel.setCandidateRefactoringGroups(extractMethodCandidateGroups);
                    ApplicationManager.getApplication().invokeLater(() -> showRefactoringsTable());
                });
            }

            @Override
            public void onCancel() {
                showEmptyPanel();
            }
        };
        runAfterCompilationCheck(backgroundable, scope.getProject(), projectInfo);
    }

    private void showEmptyPanel() {
        scrollPane.setVisible(true);
        scrollPane.setViewportView(refreshLabel);
        refreshButton.setEnabled(true);
    }

    /**
     * Shows treeTable with available refactorings.
     */
    private void showRefactoringsTable() {
        treeTableModel.reload();
        treeTable.setRootVisible(false);
        scrollPane.setViewportView(treeTable);
        scrollPane.setVisible(true);
        exportButton.setEnabled(!treeTableModel.getCandidateRefactoringGroups().isEmpty());
        refreshButton.setEnabled(true);
    }

    /**
     * Opens the definition of appropriate method for the selected suggestion by double-clicking or Enter key pressing.
     */
    private void openMethodDefinition(InputEvent e) {
        TreeTableTree treeTableTree = treeTable.getTree();
        TreePath selectedPath = treeTableTree.getSelectionModel().getSelectionPath();
        if (selectedPath != null) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof ASTSlice) {
                openDefinition(((ASTSlice) o).getSourceMethodDeclaration(), scope, (ASTSlice) o);
            } else if (o instanceof ExtractMethodCandidateGroup) {
                expandOrCollapsePath(e, treeTableTree, selectedPath);
            }
        }
    }

    /**
     * Checks that the slice can be extracted into a separate method without compilation errors.
     */
    private boolean canBeExtracted(ASTSlice slice) {
        SmartList<PsiStatement> statementsToExtract = getStatementsToExtract(slice);

        MyExtractMethodProcessor processor = new MyExtractMethodProcessor(scope.getProject(),
                null, statementsToExtract.toArray(new PsiElement[0]), slice.getLocalVariableCriterion().getType(),
                IntelliJDeodorantBundle.message("extract.method.refactoring.name"), "", HelpID.EXTRACT_METHOD,
                slice.getSourceTypeDeclaration(), slice.getLocalVariableCriterion());

        processor.setOutputVariable();

        try {
            processor.setShowErrorDialogs(false);
            return processor.prepare();

        } catch (PrepareFailedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Collects statements that can be extracted into a separate method.
     */
    public SmartList<PsiStatement> getStatementsToExtract(ASTSlice slice) {
        Set<PDGNode> nodes = slice.getSliceNodes();
        SmartList<PsiStatement> statementsToExtract = new SmartList<>();

        for (PDGNode pdgNode : nodes) {
            boolean isNotChild = true;
            for (PDGNode node : nodes) {
                if (isChild(node.getASTStatement(), pdgNode.getASTStatement())) {
                    isNotChild = false;
                }
            }
            if (isNotChild) {
                statementsToExtract.add(pdgNode.getASTStatement());
            }
        }
        return statementsToExtract;
    }

    /**
     * Extracts statements into new method.
     *
     * @param slice computation slice.
     * @return callback to run when "Refactor" button is selected.
     */
    private Runnable doExtract(ASTSlice slice) {
        return () -> {
            Editor editor = FileEditorManager.getInstance(slice.getSourceMethodDeclaration().getProject()).getSelectedTextEditor();
            SmartList<PsiStatement> statementsToExtract = getStatementsToExtract(slice);

            MyExtractMethodProcessor processor = new MyExtractMethodProcessor(slice.getSourceMethodDeclaration().getProject(),
                    editor, statementsToExtract.toArray(new PsiElement[0]), slice.getLocalVariableCriterion().getType(),
                    "", "", HelpID.EXTRACT_METHOD,
                    slice.getSourceTypeDeclaration(), slice.getLocalVariableCriterion());

            processor.setOutputVariable();

            try {
                processor.setShowErrorDialogs(true);
                if (processor.prepare()) {
                    ExtractMethodHandler.invokeOnElements(slice.getSourceMethodDeclaration().getProject(), processor,
                            slice.getSourceMethodDeclaration().getContainingFile(), true);
                }
            } catch (PrepareFailedException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Opens definition of method and highlights statements, which should be extracted.
     *
     * @param sourceMethod method from which code is proposed to be extracted into separate method.
     * @param scope        scope of the current project.
     * @param slice        computation slice.
     */
    private static void openDefinition(@Nullable PsiMethod sourceMethod, AnalysisScope scope, ASTSlice slice) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
            }

            @Override
            public void onSuccess() {
                if (sourceMethod != null) {
                    Set<SmartPsiElementPointer<PsiElement>> statements = slice.getSliceStatements();
                    PsiStatement psiStatement = (PsiStatement) statements.iterator().next().getElement();
                    if (psiStatement != null && psiStatement.isValid()) {
                        EditorHelper.openInEditor(psiStatement);
                        Editor editor = FileEditorManager.getInstance(sourceMethod.getProject()).getSelectedTextEditor();
                        if (editor != null) {
                            TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
                            editor.getMarkupModel().removeAllHighlighters();
                            statements.stream()
                                    .filter(statement -> statement.getElement() != null)
                                    .forEach(statement ->
                                            editor.getMarkupModel().addRangeHighlighter(statement.getElement().getTextRange().getStartOffset(),
                                                    statement.getElement().getTextRange().getEndOffset(), HighlighterLayer.SELECTION,
                                                    attributes, HighlighterTargetArea.EXACT_RANGE));
                        }
                    }
                }
            }
        }.queue();
    }
}