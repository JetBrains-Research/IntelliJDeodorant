package ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.Standalone;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.ProjectInfo;
import navigation.DataHolder;
import navigation.Reference;

import java.util.*;
import java.util.stream.Collectors;

public class MoveMethodRefactoringPanel extends SimpleToolWindowPanel {
    private final MySideBar sideBar;
    private static MoveMethodRefactoringPanel instance = null;
    private DumbService dumbService;
    private ProjectInfo projectInfo;

    public MoveMethodRefactoringPanel(boolean vertical, boolean borderless, Project project) {
        super(false, true);
        sideBar = new MySideBar(project);
        this.dumbService = DumbService.getInstance(project);
        refresh(project);
    }

    private void refresh(Project project) {
        dumbService.runWhenSmart(() -> {

            this.projectInfo = new ProjectInfo(project);
            new ASTReader(projectInfo);
            List<MoveMethodCandidateRefactoring> candidates = Standalone.getMoveMethodRefactoringOpportunities(projectInfo);
            final DataContext context = DataManager.getInstance().getDataContext(this);
            DataHolder.getInstance().initDataHolder(DataManager.getInstance().getDataContext(this));
            final List<Reference> references = candidates.stream().filter(Objects::nonNull).limit(50)
                    .map(x -> new Reference(x.getSourceMethodDeclaration(), x.getTargetClass().getName())).collect(Collectors.toList());
            sideBar.updateListItems(references);
            setContent(sideBar.getPanel());
            instance = this;
        });
    }

    public static void refreshList(Project project) {
        if (instance == null) {
            instance = new MoveMethodRefactoringPanel(false, false, project);
        } else
            instance.refresh(project);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.dumbService = null;
        instance = null;
    }
}
