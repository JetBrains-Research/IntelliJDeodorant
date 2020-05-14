package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

class RefactoringsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        AnalysisScope scope = new AnalysisScope(project);
        Content moveMethodPanel = contentManager.getFactory().createContent(new MoveMethodPanel(scope), IntelliJDeodorantBundle.message("feature.envy.smell.name"), false);
        Content extractMethodPanel = contentManager.getFactory().createContent(new ExtractMethodPanel(scope), IntelliJDeodorantBundle.message("long.method.smell.name"), false);
        Content godClassPanel = contentManager.getFactory().createContent(new GodClassPanel(scope), IntelliJDeodorantBundle.message("god.class.smell.name"), false);
        Content typeCheckPanel = contentManager.getFactory().createContent(new TypeCheckingPanel(scope), IntelliJDeodorantBundle.message("type.state.checking.smell.name"), false);
        contentManager.addContent(moveMethodPanel);
        contentManager.addContent(extractMethodPanel);
        contentManager.addContent(godClassPanel);
        contentManager.addContent(typeCheckPanel);
    }

}
