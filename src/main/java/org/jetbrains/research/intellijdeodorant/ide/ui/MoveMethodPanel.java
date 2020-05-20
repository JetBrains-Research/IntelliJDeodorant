package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.JDeodorantFacade;
import org.jetbrains.research.intellijdeodorant.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.ide.fus.collectors.IntelliJDeodorantCounterCollector;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringsApplier;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.moveMethod.MoveMethodRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.intellijdeodorant.utils.ExportResultsUtil;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.jetbrains.research.intellijdeodorant.ide.ui.MoveMethodTableModel.*;

/**
 * Panel for Move Method refactoring.
 */
class MoveMethodPanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    @NotNull
    private final MoveMethodTableModel model;
    private final JBTable table = new JBTable();
    private final JButton selectAllButton = new JButton();
    private final JButton deselectAllButton = new JButton();
    private final JButton doRefactorButton = new JButton(AllIcons.Actions.RefactoringBulb);
    private final JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private JScrollPane scrollPane = new JBScrollPane();
    private final JButton exportButton = new JButton(AllIcons.ToolbarDecorator.Export);
    private final JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );
    private final ScopeChooserCombo scopeChooserCombo;

    MoveMethodPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        this.scopeChooserCombo = new ScopeChooserCombo(scope.getProject());
        setLayout(new BorderLayout());
        model = new MoveMethodTableModel(refactorings);
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.NORTH);
    }

    private JScrollPane createTablePanel() {
        new TableSpeedSearch(table);
        table.setModel(model);
        model.setupRenderer(table);
        table.addMouseListener((DoubleClickListener) this::onDoubleClick);
        table.getSelectionModel().setSelectionMode(SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        setupTableLayout();
        refreshLabel.setForeground(JBColor.GRAY);
        scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setViewportView(refreshLabel);
        return scrollPane;
    }

    private void setupTableLayout() {
        final TableColumn selectionColumn = table.getTableHeader().getColumnModel().getColumn(SELECTION_COLUMN_INDEX);
        selectionColumn.setMaxWidth(30);
        selectionColumn.setMinWidth(30);

        final TableColumn dependencies = table.getTableHeader().getColumnModel().getColumn(SELECTION_COLUMN_INDEX);
        dependencies.setMaxWidth(30);
        dependencies.setMinWidth(30);
    }

    private JComponent createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(scopeChooserCombo);

        refreshButton.setToolTipText(IntelliJDeodorantBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        buttonsPanel.add(refreshButton);

        doRefactorButton.setToolTipText(IntelliJDeodorantBundle.message("refactor.button"));
        doRefactorButton.addActionListener(e -> doRefactor());
        doRefactorButton.setEnabled(false);
        buttonsPanel.add(doRefactorButton);

        exportButton.setToolTipText(IntelliJDeodorantBundle.message("export"));
        exportButton.addActionListener(e -> ExportResultsUtil.export(getValidRefactoringsSuggestions(), this));
        exportButton.setEnabled(false);
        buttonsPanel.add(exportButton);

        model.addTableModelListener(l -> enableButtonsOnConditions());
        return buttonsPanel;
    }

    private List<MoveMethodRefactoring> getValidRefactoringsSuggestions() {
        return refactorings.stream()
                .filter(refactoring -> refactoring.getOptionalMethod()
                        .isPresent())
                .collect(Collectors.toList());
    }

    private void enableButtonsOnConditions() {
        doRefactorButton.setEnabled(model.isAnySelected());
        selectAllButton.setEnabled(model.getRowCount() != 0);
        deselectAllButton.setEnabled(model.isAnySelected());
        refreshButton.setEnabled(true);
        exportButton.setEnabled(refactorings.stream()
                .anyMatch(refactoring -> refactoring.getOptionalMethod().isPresent()));
    }

    private void disableAllButtons() {
        doRefactorButton.setEnabled(false);
        selectAllButton.setEnabled(false);
        deselectAllButton.setEnabled(false);
        refreshButton.setEnabled(false);
        exportButton.setEnabled(false);
    }

    private void doRefactor() {
        disableAllButtons();
        table.setEnabled(false);
        final Set<MoveMethodRefactoring> selectedRefactorings = new HashSet<>(model.pullSelected());
        RefactoringsApplier.moveRefactoring(new ArrayList<>(selectedRefactorings));
        model.updateRows();
        table.setEnabled(true);
        enableButtonsOnConditions();
    }

    private void refreshPanel() {
        if (scopeChooserCombo.getScope() != null) {
            refactorings.clear();
            model.clearTable();
            disableAllButtons();
            scrollPane.setVisible(false);
            calculateRefactorings();
        }
    }

    private void calculateRefactorings() {
        Project project = scope.getProject();
        ProjectInfo projectInfo = new ProjectInfo(scopeChooserCombo.getScope(), true);
        Set<String> classNamesToBeExamined = new HashSet<>();

        PsiUtils.extractFiles(project).stream()
                .filter(file -> scopeChooserCombo.getScope().contains(file))
                .forEach(list ->
                        Arrays.stream(list.getClasses()).map(PsiClass::getQualifiedName).forEach(classNamesToBeExamined::add));

        final Task.Backgroundable backgroundable = new Task.Backgroundable(project, IntelliJDeodorantBundle.message("feature.envy.detect.indicator.status"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    List<MoveMethodCandidateRefactoring> candidates = JDeodorantFacade.getMoveMethodRefactoringOpportunities(projectInfo, indicator, classNamesToBeExamined);
                    final List<MoveMethodRefactoring> references = candidates.stream().filter(Objects::nonNull)
                            .map(x ->
                                    new MoveMethodRefactoring(x.getSourceMethodDeclaration(),
                                            x.getTargetClass().getClassObject().getPsiClass(),
                                            x.getDistinctSourceDependencies(),
                                            x.getDistinctTargetDependencies()))
                            .collect(Collectors.toList());
                    refactorings.clear();
                    refactorings.addAll(new ArrayList<>(references));
                    model.updateTable(refactorings);
                    scrollPane.setVisible(true);
                    scrollPane.setViewportView(table);
                    enableButtonsOnConditions();
                    IntelliJDeodorantCounterCollector.getInstance().refactoringFound(project, "move.method", references.size());
                });
            }

            @Override
            public void onCancel() {
                showEmptyPanel();
            }
        };

        AbstractRefactoringPanel.runAfterCompilationCheck(backgroundable, project, projectInfo);
    }

    private void showEmptyPanel() {
        scrollPane.setVisible(true);
        scrollPane.setViewportView(refreshLabel);
        refreshButton.setEnabled(true);
    }

    private void onDoubleClick(InputEvent e) {
        final int selectedRow = table.getSelectedRow() == -1 ? -1 : table.convertRowIndexToModel(table.getSelectedRow());
        final int selectedColumn = table.getSelectedColumn();
        if (selectedRow != -1 && (selectedColumn == ENTITY_COLUMN_INDEX || selectedColumn == MOVE_TO_COLUMN_INDEX)) {
            openDefinition(model.getUnitAt(selectedRow, selectedColumn).orElse(null), scope);
        }
    }

    private static void openDefinition(@Nullable PsiMember unit, AnalysisScope scope) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            private PsiElement result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                result = unit;
            }

            @Override
            public void onSuccess() {
                if (result != null) {
                    EditorHelper.openInEditor(result);
                }
            }
        }.queue();
    }
}
