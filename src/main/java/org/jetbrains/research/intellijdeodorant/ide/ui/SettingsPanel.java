package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.research.intellijdeodorant.core.ast.ASTReader;
import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsPanel extends JPanel {
    private static final String REFACTORING_PLACE_CHOOSE_BOX = "refactorings.place.choose.box";
    private static final String SEARCH_REFACTORINGS_IN_WHOLE_PROJECT = "search.in.whole.project.status";
    private static final String SEARCH_REFACTORINGS_IN_OPENED_FILES = "search.in.all.opened.files.status";
    private static final String SEARCH_REFACTORINGS_IN_CURRENT_FILE = "search.in.current.file.status";

    @NotNull
    private static Project project;
    private final static ComboBox<String> refactoringPlaceChooseBox = new ComboBox<>(new String[]{
            IntelliJDeodorantBundle.message(SEARCH_REFACTORINGS_IN_WHOLE_PROJECT),
            IntelliJDeodorantBundle.message(SEARCH_REFACTORINGS_IN_OPENED_FILES),
            IntelliJDeodorantBundle.message(SEARCH_REFACTORINGS_IN_CURRENT_FILE),
    });

    SettingsPanel(@NotNull Project project) {
        SettingsPanel.project = project;
        setLayout(new BorderLayout());
        add(createComponentPanel(), BorderLayout.NORTH);
    }

    private JComponent createComponentPanel() {
        final JBPanel panel = new JBPanel();
        final JBPanel componentPanel = new JBPanel<JBPanel<JBPanel>>();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        componentPanel.add(new Label(IntelliJDeodorantBundle.message(REFACTORING_PLACE_CHOOSE_BOX)));
        componentPanel.add(refactoringPlaceChooseBox);
        panel.add(componentPanel, BorderLayout.WEST);
        return panel;
    }

    public static Set<ClassObject> getClassesToFindRefactorings(SystemObject systemObject) {
        String currentComboBoxValue = Objects.requireNonNull(refactoringPlaceChooseBox.getSelectedItem()).toString();
        if (currentComboBoxValue.equals("whole project")) {
            return new LinkedHashSet<>(systemObject.getClassObjects());
        } else {
            return getClassesToFindRefactorings(currentComboBoxValue).stream().map(ASTReader::processTypeDeclaration).collect(Collectors.toSet());
        }
    }

    public static Set<PsiClass> getClassesToFindRefactorings(String currentComboBoxValue) {
        if (currentComboBoxValue.equals("all opened files")) {
            return getAllObjectsOpenedInEditor();
        } else {
            return getCurrentObjectOpenedInEditor();
        }
    }

    private static Set<PsiClass> getAllObjectsOpenedInEditor() {
        List<PsiClass> classObjectsToBeExamined = new ArrayList<>();
        VirtualFile[] currentOpenedFiles = FileEditorManager.getInstance(project).getOpenFiles();
        for (VirtualFile virtualFile : currentOpenedFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile instanceof PsiJavaFile) {
                classObjectsToBeExamined.addAll(Arrays.asList(((PsiJavaFile) psiFile).getClasses()));
            }
        }
        return new LinkedHashSet<>(classObjectsToBeExamined);
    }

    private static Set<PsiClass> getCurrentObjectOpenedInEditor() {
        Set<PsiClass> classObjectsToBeExamined = new LinkedHashSet<>();
        FileEditor currentEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (currentEditor != null)
        {
            VirtualFile currentFile = currentEditor.getFile();
            if (currentFile != null) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
                if (psiFile instanceof PsiJavaFile) {
                    classObjectsToBeExamined.addAll(Arrays.asList(((PsiJavaFile) psiFile).getClasses()));
                }
            }
        }
        return new LinkedHashSet<>(classObjectsToBeExamined);
    }
}
