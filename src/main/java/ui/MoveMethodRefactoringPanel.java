package ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.Standalone;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.ProjectInfo;
import navigation.Reference;

import java.util.*;
import java.util.stream.Collectors;

public class MoveMethodRefactoringPanel extends SimpleToolWindowPanel {
    private final MySideBar sideBar;
    private ProjectInfo projectInfo;

    MoveMethodRefactoringPanel(Project project) {
        super(false, true);
        sideBar = new MySideBar(project);
        refresh(project);
    }

    private void refresh(Project project) {
        this.projectInfo = new ProjectInfo(project);
        final Task.Backgroundable backgroundable = new Task.Backgroundable(project, "Detecting a Feature Envy smell...", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    new ASTReader(projectInfo);
                    List<MoveMethodCandidateRefactoring> candidates = Standalone.getMoveMethodRefactoringOpportunities(projectInfo);
                    final List<Reference> references = candidates.stream().filter(Objects::nonNull)
                            .map(x -> new Reference(x.getSourceMethodDeclaration(), x.getTargetClass().getName())).collect(Collectors.toList());
                    sideBar.updateListItems(references);
                    setContent(sideBar.getPanel());
                });
            }
        };
        ProgressManager.getInstance().run(backgroundable);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
