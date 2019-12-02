package ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import core.ast.ClassObject;
import core.ast.SystemObject;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SettingsPanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    private final static ComboBox<String> refactoringPlaceChooseBox = new ComboBox<>(new String[]{"whole project",
            "selected package",
            "opened class",
            "opened classes",
    });

    SettingsPanel(Project project) {
        scope = new AnalysisScope(project);
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

    public static Set<ClassObject> getClassesToFindRefactorings(SystemObject systemObject) {
        String currentComboBoxValue = Objects.requireNonNull(refactoringPlaceChooseBox.getSelectedItem()).toString();
        switch (currentComboBoxValue) {
            case "selected package":
                return new LinkedHashSet<>();
            default:
                return new LinkedHashSet<>(systemObject.getClassObjects());
        }
    }
}
