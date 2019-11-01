package ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;

import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.*;

import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import core.ast.decomposition.cfg.ASTSlice;
import core.ast.decomposition.cfg.ASTSliceGroup;
import core.ast.decomposition.cfg.PDGNode;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refactoring.ExtractMethodRefactoring;
import refactoring.MyExtractMethodProcessor;
import utils.IntelliJDeodorantBundle;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static core.ast.Standalone.getExtractMethodRefactoringOpportunities;
import static utils.PsiUtils.isChild;

/**
 * Panel for Extract Method refactoring.
 */
class ExtractMethodPanel extends JPanel {
    private static final String REFACTOR_BUTTON_TEXT_KEY = "refactor.button";
    private static final String REFRESH_BUTTON_TEXT_KEY = "refresh.button";
    private static final String DETECT_INDICATOR_STATUS_TEXT_KEY = "long.method.detect.indicator.status";
    private static final String EXTRACT_METHOD_REFACTORING_NAME = "extract.method.refactoring.name";

    @NotNull
    private final AnalysisScope scope;
    private ExtractMethodTableModel model;
    private final JTree jTree = new JTree();
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final List<ExtractMethodRefactoring> refactorings = new ArrayList<>();
    private JScrollPane scrollPane = new JScrollPane();

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
        jTree.setModel(model);
        jTree.setCellRenderer(new DefaultTreeCellRenderer());
        jTree.addMouseListener((DoubleClickListener) this::onDoubleClick);
        jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scrollPane = ScrollPaneFactory.createScrollPane(jTree);
        scrollPane.setVisible(false);
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
        ASTSlice computationSlice = (ASTSlice) jTree.getAnchorSelectionPath().getPath()[2];
        TransactionGuard.getInstance().submitTransactionAndWait((doExtract(computationSlice)));
    }

    /**
     * Refreshes the panel with suggestions.
     */
    private void refreshPanel() {
        Editor editor = FileEditorManager.getInstance(scope.getProject()).getSelectedTextEditor();
        if (editor != null) {
            editor.getMarkupModel().removeAllHighlighters();
        }
        refactorings.clear();
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
                    Set<ASTSliceGroup> candidates = getExtractMethodRefactoringOpportunities(projectInfo, indicator);
                    final List<ExtractMethodRefactoring> references = candidates.stream().filter(Objects::nonNull)
                            .map(ExtractMethodRefactoring::new).collect(Collectors.toList());
                    refactorings.clear();
                    refactorings.addAll(new ArrayList<>(references));
                    model = new ExtractMethodTableModel(new ArrayList<>(candidates));
                    jTree.setModel(model);
                    scrollPane.setVisible(true);
                });
            }
        };
        ProgressManager.getInstance().run(backgroundable);
    }

    /**
     * Opens the definition of appropriate method for the selected suggestion by double-clicking.
     */
    private void onDoubleClick() {
        if (jTree.getAnchorSelectionPath().getPath().length == 3) {
            Object o = jTree.getAnchorSelectionPath().getPath()[2];
            if (o instanceof ASTSlice) {
                ASTSlice selectedRow = (ASTSlice) jTree.getAnchorSelectionPath().getPath()[2];
                openDefinition(selectedRow.getSourceMethodDeclaration(), scope, selectedRow);
            }
        }
    }

    /**
     * Extracts statements into new method.
     *
     * @param slice computation slice.
     * @return callback to run when "Refactor" button is selected.
     */
    private Runnable doExtract(ASTSlice slice) {
        return () -> {
            Editor editor = FileEditorManager.getInstance(scope.getProject()).getSelectedTextEditor();
            Set<PDGNode> nodes = slice.getSliceNodes();
            ArrayList<PsiStatement> statementsToExtract = new ArrayList<>();

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

            MyExtractMethodProcessor processor = new MyExtractMethodProcessor(scope.getProject(),
                    editor, statementsToExtract.toArray(new PsiElement[0]), slice.getLocalVariableCriterion().getType(),
                    IntelliJDeodorantBundle.message(EXTRACT_METHOD_REFACTORING_NAME), "", HelpID.EXTRACT_METHOD,
                    slice.getSourceTypeDeclaration(), slice.getLocalVariableCriterion());

            processor.setOutputVariable();
            processor.testTargetClass(slice.getSourceTypeDeclaration());

            try {
                processor.setShowErrorDialogs(false);
                processor.prepare();
            } catch (PrepareFailedException e) {
                e.printStackTrace();
            }

            ExtractMethodHandler.invokeOnElements(scope.getProject(), processor,
                    slice.getSourceMethodDeclaration().getContainingFile(), true);
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
                    EditorHelper.openInEditor(sourceMethod);
                    Editor editor = FileEditorManager.getInstance(sourceMethod.getProject()).getSelectedTextEditor();
                    if (editor != null) {
                        TextAttributes attributes = new TextAttributes(editor.getColorsScheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
                                new JBColor(new Color(84, 168, 78), new Color(16, 105, 15)), null, null, 0);
                        Set<PsiStatement> statements = slice.getSliceStatements();
                        editor.getMarkupModel().removeAllHighlighters();
                        statements.forEach(statement -> editor.getMarkupModel().addRangeHighlighter(statement.getTextRange().getStartOffset(),
                                statement.getTextRange().getEndOffset(), HighlighterLayer.SELECTION, attributes, HighlighterTargetArea.EXACT_RANGE));
                    }
                }
            }
        }.queue();
    }

    @FunctionalInterface
    private interface DoubleClickListener extends MouseListener {
        void onDoubleClick();

        default void mouseClicked(MouseEvent e) {
            if (e.getClickCount() >= 2) {
                onDoubleClick();
            }
        }

        default void mousePressed(MouseEvent e) {
        }

        default void mouseReleased(MouseEvent e) {
        }

        default void mouseEntered(MouseEvent e) {
        }

        default void mouseExited(MouseEvent e) {
        }
    }
}