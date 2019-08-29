package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import gr.uom.java.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RecommendationToolWindowFactory implements ToolWindowFactory {
    private JPanel jPanel = new JPanel();
    private JButton button = new JButton();
    private ProjectInfo projectInfo;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        jPanel.add(button);
        Content content = contentFactory.createContent(jPanel, "", false);
        toolWindow.getContentManager().addContent(content);
       // init(project);
    }

    private void init(Project project) {
        this.projectInfo = new ProjectInfo(project);
    }

}
