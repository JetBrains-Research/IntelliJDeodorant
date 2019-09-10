package ui;

import com.intellij.analysis.AnalysisScope;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import utils.IntelliJDeodorantBundle;

import javax.swing.*;

/**
 * A tool window panel that consist of panels for each code smell type.
 */
public class RefactoringsPanel extends SimpleToolWindowPanel {

    RefactoringsPanel(Project project) {
        super(false, true);
        addRefactoringPanels(project);
    }

    private void addRefactoringPanels(Project project) {
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.add(IntelliJDeodorantBundle.message("feature.envy.smell.name"), new MoveMethodPanel(new AnalysisScope(project)));
        setContent(jTabbedPane);
    }

}
