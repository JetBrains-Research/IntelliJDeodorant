package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import core.ast.ClassObject;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SettingsPanel extends JPanel {
    private static ComboBox<String> refactoringPlaceChooseBox = new ComboBox<>(new String[]{"whole project",
            "selected package",
            "opened class",
            "opened classes",
    });

    SettingsPanel(Project project) {
        setLayout(new BorderLayout());
        add(createComponentPanel(), BorderLayout.NORTH);
    }

    private JComponent createComponentPanel() {
        final JBPanel panel = new JBPanel();
        final JBPanel componentPanel = new JBPanel<JBPanel<JBPanel>>();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        componentPanel.add(new Label("Search refactorings in:"));
        componentPanel.add(refactoringPlaceChooseBox);
        panel.add(componentPanel, BorderLayout.WEST);
        return panel;
    }

    public static Set<ClassObject> getClassesToFindRefactorings() {
        String currentComboBoxValue = Objects.requireNonNull(refactoringPlaceChooseBox.getSelectedItem()).toString();
        switch (currentComboBoxValue) {
            case "whole project":
                return new LinkedHashSet<>();
            default:
                return null;
        }
    }
}
