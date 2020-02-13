package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.Refactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.ElementSelectionListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.EnterKeyListener;
import org.jetbrains.research.intellijdeodorant.utils.ExportResultsUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractRefactoringPanel extends JPanel {
    private static final String REFACTOR_BUTTON_TEXT_KEY = "refactor.button";
    private static final String REFRESH_BUTTON_TEXT_KEY = "refresh.button";
    private static final String EXPORT_BUTTON_TEXT_KEY = "export";
    private static final String REFRESH_NEEDED_TEXT = "press.refresh.to.find.refactoring.opportunities";
    private static final NotificationGroup NOTIFICATION_GROUP =
            new NotificationGroup(IntelliJDeodorantBundle.message("intellijdeodorant"), NotificationDisplayType.STICKY_BALLOON, true);
    private String detectIndicatorStatusTextKey;
    @NotNull
    protected final AnalysisScope scope;
    private AbstractTreeTableModel model;
    private final TreeTable treeTable;
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final JButton exportButton = new JButton();
    private JScrollPane scrollPane = new JBScrollPane();
    private JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message(REFRESH_NEEDED_TEXT),
            SwingConstants.CENTER
    );
    private RefactoringType refactoringType;
    private static Notification errorNotification;
    private int refactorDepth;

    public AbstractRefactoringPanel(@NotNull AnalysisScope scope,
                                    String detectIndicatorStatusTextKey,
                                    RefactoringType refactoringType,
                                    AbstractTreeTableModel model,
                                    int refactorDepth) {
        this.scope = scope;
        this.detectIndicatorStatusTextKey = detectIndicatorStatusTextKey;
        this.refactoringType = refactoringType;
        this.model = model;
        this.treeTable = new TreeTable(model);
        this.refactorDepth = refactorDepth;
        refreshLabel.setForeground(JBColor.GRAY);
        setLayout(new BorderLayout());
        setupGUI();
    }

    public static void runAfterCompilationCheck(Task.Backgroundable afterCompilationBackgroundable, Project project, ProjectInfo projectInfo) {
        final Task.Backgroundable compilationBackgroundable = new Task.Backgroundable(project, IntelliJDeodorantBundle.message("project.compiling.indicator.text"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                runAfterCompilationCheck(projectInfo, afterCompilationBackgroundable);
            }
        };

        ProgressManager.getInstance().run(compilationBackgroundable);
    }

    /**
     * Runs task only if there are no compilation errors in the project.
     */
    private static void runAfterCompilationCheck(ProjectInfo projectInfo, Task task) {
        ApplicationManager.getApplication().invokeLater(() -> {
            List<PsiClass> classes = projectInfo.getClasses();

            if (!classes.isEmpty()) {
                VirtualFile[] virtualFiles = classes.stream()
                        .map(classObject -> classObject.getContainingFile().getVirtualFile()).toArray(VirtualFile[]::new);
                Project project = projectInfo.getProject();

                CompilerManager compilerManager = CompilerManager.getInstance(project);
                CompileStatusNotification callback = (aborted, errors, warnings, compileContext) -> {
                    if (errors == 0 && !aborted) {
                        ProgressManager.getInstance().run(task);
                    } else {
                        AbstractRefactoringPanel.showCompilationErrorNotification(project);
                    }
                };
                CompileScope compileScope = compilerManager.createFilesCompileScope(virtualFiles);
                compilerManager.make(compileScope, callback);
            } else {
                ProgressManager.getInstance().run(task);
            }
        });
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        registerPsiModificationListener();
        showRefreshingProposal();
    }

    private void removeSelection() {
        treeTable.getTree().setSelectionPath(null);
    }

    /**
     * Shows treeTable with available refactorings.
     */
    private void showRefactoringsTable() {
        removeSelection();
        scrollPane.setVisible(true);
        model.reload();
        exportButton.setEnabled(!model.getCandidateRefactoringGroups().isEmpty());
        scrollPane.setViewportView(treeTable);
    }

    /**
     * Hides treeTable with refactorings and shows text which proposes refreshing available refactorings.
     */
    protected void showRefreshingProposal() {
        removeSelection();
        if (errorNotification != null && !errorNotification.isExpired()) {
            errorNotification.expire();
        }
        scrollPane.setVisible(true);
        exportButton.setEnabled(false);
        scrollPane.setViewportView(refreshLabel);
    }

    /**
     * Hides treeTable with refactorings and leaves panel empty
     */
    private void showEmptyPanel() {
        removeSelection();
        exportButton.setEnabled(false);
        scrollPane.setVisible(false);
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
     * Adds a listener that invalidates found refactoring opportunities if the structure of PSI is changed.
     */
    private void registerPsiModificationListener() {
        MessageBus projectMessageBus = scope.getProject().getMessageBus();
        projectMessageBus.connect().subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
            @Override
            public void modificationCountChanged() {
                ApplicationManager.getApplication().invokeLater(() -> showRefreshingProposal());
            }
        });
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
        doRefactorButton.addActionListener(l -> refactorSelected());
        buttonPanel.add(doRefactorButton);

        refreshButton.setText(IntelliJDeodorantBundle.message(REFRESH_BUTTON_TEXT_KEY));
        refreshButton.addActionListener(l -> refreshPanel());
        buttonPanel.add(refreshButton);

        exportButton.setText(IntelliJDeodorantBundle.message(EXPORT_BUTTON_TEXT_KEY));
        exportButton.addActionListener(l -> exportResults());
        buttonPanel.add(exportButton);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    private void exportResults() {
        List<? extends Refactoring> refactorings = model.getCandidateRefactoringGroups().stream()
                .flatMap(group -> group.getCandidates().stream())
                .collect(Collectors.toList());
        ExportResultsUtil.export(refactorings, this);
    }

    /**
     * Preforms selected refactoring.
     */
    private void refactorSelected() {
        TreePath selectedPath = treeTable.getTree().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            AbstractCandidateRefactoring computationSlice = (AbstractCandidateRefactoring) selectedPath.getLastPathComponent();
            removeSelection();
            doRefactor(computationSlice);
        }
    }

    protected abstract void doRefactor(AbstractCandidateRefactoring candidateRefactoring);

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
        showEmptyPanel();
        calculateRefactorings();
    }

    /**
     * Calculates suggestions for whole project.
     */
    private void calculateRefactorings() {
        ProjectInfo projectInfo = new ProjectInfo(scope.getProject());

        final Task.Backgroundable backgroundable = new Task.Backgroundable(scope.getProject(),
                IntelliJDeodorantBundle.message(detectIndicatorStatusTextKey), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    List<RefactoringType.AbstractCandidateRefactoringGroup> candidates =
                            refactoringType.getRefactoringOpportunities(projectInfo, indicator);
                    if (candidates == null) {
                        showCompilationErrorNotification(getProject());
                        candidates = new ArrayList<>();
                    }
                    model.setCandidateRefactoringGroups(candidates);
                    ApplicationManager.getApplication().invokeLater(() -> showRefactoringsTable());
                });
            }
        };

        runAfterCompilationCheck(backgroundable, scope.getProject(), projectInfo);
    }

    /**
     * Highlights refactoring-specific code fragment.
     */
    private void highlightCode() {
        TreeTableTree treeTableTree = treeTable.getTree();
        TreePath selectedPath = treeTableTree.getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            Object o = selectedPath.getLastPathComponent();
            if (refactoringType.instanceOfCandidateRefactoring(o)) {
                AbstractCandidateRefactoring refactoring = (AbstractCandidateRefactoring) o;
                refactoring.highlightCode();
            } else {
                if (treeTableTree.isExpanded(selectedPath)) {
                    treeTableTree.collapsePath(selectedPath);
                } else {
                    treeTableTree.expandPath(selectedPath);
                }
            }
        }
    }

    public AbstractRefactoring getAbstractRefactoringFromAbstractCandidateRefactoring(AbstractCandidateRefactoring candidate) {
        return refactoringType.newAbstractRefactoring(candidate);
    }

    /**
     * Opens definition of method and highlights specified element in the method.
     */
    public static void highlightStatement(@Nullable PsiMethod sourceMethod,
                                          AnalysisScope scope,
                                          PsiElement statement,
                                          boolean openInEditor) {
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

    public static void highlightMethod(@Nullable PsiMethod sourceMethod,
                                       AnalysisScope scope, boolean openInEditor) {
        highlightStatement(sourceMethod, scope, sourceMethod, openInEditor);
    }

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

    private static void highlightPsiElement(PsiElement psiElement, boolean openInEditor) {
        if (openInEditor) {
            EditorHelper.openInEditor(psiElement);
        }

        Editor editor = FileEditorManager.getInstance(psiElement.getProject()).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
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

    public static void showCompilationErrorNotification(Project project) {
        errorNotification = NOTIFICATION_GROUP.createNotification(IntelliJDeodorantBundle.message("compilation.error.notification.text"), MessageType.ERROR);
        Notifications.Bus.notify(errorNotification, project);
    }
}