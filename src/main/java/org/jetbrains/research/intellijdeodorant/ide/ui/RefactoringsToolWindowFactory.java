package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

class RefactoringsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(new RefactoringsPanel(project), "Code Smells", false);
        contentManager.addContent(content);
        content = contentManager.getFactory().createContent(new SettingsPanel(project), "Settings", false);
        contentManager.addContent(content);
    }
}
