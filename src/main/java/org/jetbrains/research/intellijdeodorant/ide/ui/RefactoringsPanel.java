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
     * Creates a tabbed panel that consist of panels for each code smell type.
     *
     * @param project current project.
     */
    private void addRefactoringPanels(Project project) {
        JTabbedPane jTabbedPane = new JBTabbedPane();
        jTabbedPane.add(IntelliJDeodorantBundle.message("feature.envy.smell.name"), new MoveMethodPanel(new AnalysisScope(project)));
        jTabbedPane.add(IntelliJDeodorantBundle.message("long.method.smell.name"), new ExtractMethodPanel(new AnalysisScope(project)));
        setContent(jTabbedPane);
    }

}
