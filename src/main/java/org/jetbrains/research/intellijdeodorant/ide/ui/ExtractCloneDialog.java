package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;

public class ExtractCloneDialog extends DialogWrapper {
    private JPanel contentPanel = new JPanel();
    private JRadioButton DeckardButton = new JRadioButton();
    private JRadioButton CCFinderButton = new JRadioButton();
    private JRadioButton ConQATButton = new JRadioButton();
    private JRadioButton NiCadButton = new JRadioButton();
    private JRadioButton CloneDRButton = new JRadioButton();
    private JTextPane selectCloneDetectionToolPane = new JTextPane();
    private JTextPane selectInputFilePane = new JTextPane();

    protected ExtractCloneDialog(@NotNull Project project, boolean canBeParent) {
        super(project, canBeParent);
        configurePanel();
        setTitle(IntelliJDeodorantBundle.message("duplicated.code.dialog.title") + " " + project.getName());
        setSize(1000, 1000);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    private void configurePanel() {
        JPanel buttonsPanel = new JPanel(new GridLayout(5, 0));
        DeckardButton.setText(IntelliJDeodorantBundle.message("duplicated.code.tool.name.deckard"));
        CCFinderButton.setText(IntelliJDeodorantBundle.message("duplicated.code.tool.name.ccfinder"));
        ConQATButton.setText(IntelliJDeodorantBundle.message("duplicated.code.tool.name.conqat"));
        NiCadButton.setText(IntelliJDeodorantBundle.message("duplicated.code.tool.name.nicad"));
        CloneDRButton.setText(IntelliJDeodorantBundle.message("duplicated.code.tool.name.clonedr"));

        buttonsPanel.add(CloneDRButton);
        buttonsPanel.add(DeckardButton);
        buttonsPanel.add(CCFinderButton);
        buttonsPanel.add(ConQATButton);
        buttonsPanel.add(NiCadButton);

        buttonsPanel.setVisible(true);

        selectCloneDetectionToolPane.setText(IntelliJDeodorantBundle.message("duplicated.code.dialog.select.tool.text"));
        selectInputFilePane.setText(IntelliJDeodorantBundle.message("duplicated.code.dialog.select.file.text"));
        selectInputFilePane.setVisible(false);

        contentPanel.add(selectCloneDetectionToolPane, BorderLayout.WEST);
        contentPanel.add(selectInputFilePane, BorderLayout.EAST);
        contentPanel.add(buttonsPanel, BorderLayout.WEST);
    }

    private ItemListener createSelectListener() {
        return a -> {
            selectInputFilePane.setVisible(true);
            contentPanel.updateUI();
        };
    }
}
