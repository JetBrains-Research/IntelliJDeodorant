package ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.components.JBPanel;
import core.ast.ClassObject;
import core.ast.SystemObject;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import utils.PsiUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SettingsPanel extends JPanel {
    @NotNull
    private static Project project;
    private final static ComboBox<String> refactoringPlaceChooseBox = new ComboBox<>(new String[]{"whole project",
            "current class",
            "all opened classes",
    });

    SettingsPanel(Project project) {
        SettingsPanel.project = project;
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
            case "all opened classes":
                return getAllOpenedClassObjects();
            case "current class":
                return getCurrentOpenedClassObject();
            default:
                return new LinkedHashSet<>(systemObject.getClassObjects());
        }

    }

    private static Set<ClassObject> getAllOpenedClassObjects() {
        List<ClassObject> classObjectsToBeExamined = new ArrayList<>();
        VirtualFile[] currentOpenedFiles = FileEditorManager.getInstance(project).getOpenFiles();
        for (VirtualFile virtualFile : currentOpenedFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile != null) {
                for (PsiClass psiClass : ((PsiJavaFile) psiFile).getClasses()) {
                    classObjectsToBeExamined.add(new ClassObject(psiClass));
                }
            }
        }
        System.out.println(classObjectsToBeExamined.size());
        return new LinkedHashSet<>(classObjectsToBeExamined);
    }

    private static Set<ClassObject> getCurrentOpenedClassObject() {
        Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>();
        FileEditor currentEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (currentEditor != null && currentEditor.getFile() != null) {
            PsiClass psiClass = PsiUtils.findClass(currentEditor.getFile().getName(), project);
            if (psiClass != null) {
                classObjectsToBeExamined.add(new ClassObject(psiClass));
            }
        }
        return classObjectsToBeExamined;
    }
}
