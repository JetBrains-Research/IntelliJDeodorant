package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.JDeodorantFacade;
import org.jetbrains.research.intellijdeodorant.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringsApplier;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.moveMethod.MoveMethodRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.intellijdeodorant.utils.ExportResultsUtil;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.jetbrains.research.intellijdeodorant.ide.ui.MoveMethodTableModel.SELECTION_COLUMN_INDEX;

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
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private JScrollPane scrollPane = new JBScrollPane();
    private final JButton exportButton = new JButton();
    private final JLabel refreshLabel = new JLabel(
            IntelliJDeodorantBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );

    MoveMethodPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        model = new MoveMethodTableModel(refactorings);
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);
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
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel buttonsPanel = new JBPanel<JBPanel<JBPanel>>();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        selectAllButton.setText(IntelliJDeodorantBundle.message("select.all.button"));
        selectAllButton.addActionListener(e -> model.selectAll());
        selectAllButton.setEnabled(false);
        buttonsPanel.add(selectAllButton);

        deselectAllButton.setText(IntelliJDeodorantBundle.message("deselect.all.button"));
        deselectAllButton.addActionListener(e -> model.deselectAll());
        deselectAllButton.setEnabled(false);
        buttonsPanel.add(deselectAllButton);

        doRefactorButton.setText(IntelliJDeodorantBundle.message("refactor.button"));
        doRefactorButton.addActionListener(e -> refactorSelected());
        doRefactorButton.setEnabled(false);
        buttonsPanel.add(doRefactorButton);

        refreshButton.setText(IntelliJDeodorantBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        buttonsPanel.add(refreshButton);

        exportButton.setText(IntelliJDeodorantBundle.message("export"));
        exportButton.addActionListener(e -> ExportResultsUtil.export(getValidRefactoringsSuggestions(), this));
        exportButton.setEnabled(false);
        buttonsPanel.add(exportButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        model.addTableModelListener(l -> enableButtonsOnConditions());
        return panel;
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

    private void refactorSelected() {
        disableAllButtons();
        table.setEnabled(false);

        final Set<MoveMethodRefactoring> selectedRefactorings = new HashSet<>(model.pullSelected());

        Set<MoveMethodRefactoring> appliedRefactorings = RefactoringsApplier.moveRefactoring(new ArrayList<>(selectedRefactorings), scope);
        model.setAppliedRefactorings(new HashSet<>(appliedRefactorings));
        table.setEnabled(true);

        enableButtonsOnConditions();
    }

    private void refreshPanel() {
        refactorings.clear();
        model.clearTable();
        disableAllButtons();
        scrollPane.setVisible(false);
        calculateRefactorings();
    }

    private void calculateRefactorings() {
        Project project = scope.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);

        final Task.Backgroundable backgroundable = new Task.Backgroundable(project, IntelliJDeodorantBundle.message("feature.envy.detect.indicator.status"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    List<MoveMethodCandidateRefactoring> candidates = JDeodorantFacade.getMoveMethodRefactoringOpportunities(projectInfo, indicator);
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
        if (selectedRow == -1 || selectedColumn == -1 || selectedColumn == SELECTION_COLUMN_INDEX) {
            return;
        }
        openDefinition(model.getUnitAt(selectedRow, selectedColumn).orElse(null), scope);
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