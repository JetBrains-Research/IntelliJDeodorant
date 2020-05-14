package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.swing.*;

class RefactoringsPanel extends SimpleToolWindowPanel {

    RefactoringsPanel(Project project) {
        super(false, true);
        addRefactoringPanels(project);
    }

    /**
     * Adds a panel for each code smell to the main panel.
     *
     * @param project current project.
     */
    private void addRefactoringPanels(Project project) {
        JTabbedPane jTabbedPane = new JBTabbedPane();
        jTabbedPane.add(IntelliJDeodorantBundle.message("feature.envy.smell.name"), new MoveMethodPanel(new AnalysisScope(project)));
        jTabbedPane.add(IntelliJDeodorantBundle.message("long.method.smell.name"), new ExtractMethodPanel(new AnalysisScope(project)));
        jTabbedPane.add(IntelliJDeodorantBundle.message("god.class.smell.name"), new GodClassPanel(new AnalysisScope(project)));
        jTabbedPane.add(IntelliJDeodorantBundle.message("type.state.checking.smell.name"), new TypeCheckingPanel(new AnalysisScope(project)));
        setContent(jTabbedPane);
    }
}
