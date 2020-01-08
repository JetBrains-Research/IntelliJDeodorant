package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class ExtractClonePanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    private final JButton refreshButton = new JButton();
    private JScrollPane scrollPane = new JBScrollPane();
    private final JTree jTree = new Tree();

    ExtractClonePanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);
    }

    private JScrollPane createTablePanel() {
        jTree.setCellRenderer(new ExtractMethodCandidatesTreeCellRenderer());
        jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scrollPane = ScrollPaneFactory.createScrollPane(jTree);
        scrollPane.setVisible(false);
        return scrollPane;
    }

    private JComponent createButtonsPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel buttonPanel = new JBPanel<JBPanel<JBPanel>>();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        refreshButton.setText(IntelliJDeodorantBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        refreshButton.setEnabled(true);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    private void refreshPanel() {
        ExtractCloneDialog extractCloneDialog = new ExtractCloneDialog(scope.getProject(), true);
        extractCloneDialog.show();
    }

}
