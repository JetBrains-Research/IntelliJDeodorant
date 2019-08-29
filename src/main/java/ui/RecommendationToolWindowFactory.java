package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import gr.uom.java.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RecommendationToolWindowFactory implements ToolWindowFactory {
    private JPanel jPanel = new JPanel();
    private JButton button = new JButton();
    private ProjectInfo projectInfo;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(new MoveMethodRefactoringPanel(true, true, project), null, false);
        contentManager.addContent(content);
    }

    private void init(Project project) {
        this.projectInfo = new ProjectInfo(project);
    }

}
