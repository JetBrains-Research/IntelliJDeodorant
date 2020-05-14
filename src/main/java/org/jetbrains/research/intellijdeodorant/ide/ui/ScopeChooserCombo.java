package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.scopeChooser.EditScopesDialog;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.ui.ComboboxWithBrowseButton;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;

public class ScopeChooserCombo extends ComboboxWithBrowseButton {
    private AnalysisScope customScope = null;
    private final Project project;

    public ScopeChooserCombo(Project project) {
        setTextFieldPreferredWidth(27);
        configureComboBox(null);
        addActionListener(a -> browseCustomScope(project));
        this.project = project;
    }

    private void configureComboBox(String scope) {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(IntelliJDeodorantBundle.message("scope.all.files"));
        comboBoxModel.addElement(IntelliJDeodorantBundle.message("scope.current.file"));
        comboBoxModel.addElement(IntelliJDeodorantBundle.message("scope.opened.files"));
        if (scope != null) {
            comboBoxModel.addElement(scope);
        }
        getComboBox().setModel(comboBoxModel);
        getComboBox().setSelectedItem(scope == null ? IntelliJDeodorantBundle.message("scope.all.files") : scope);
    }

    private void browseCustomScope(Project project) {
        EditScopesDialog dialog = EditScopesDialog.showDialog(project, null);
        if (dialog.isOK()) {
            if (dialog.getSelectedScope() != null) {
                customScope = new AnalysisScope(GlobalSearchScopesCore.filterScope(project, dialog.getSelectedScope()), project);
                configureComboBox(customScope.getDisplayName());
            }
        }
    }

    public AnalysisScope getScope() {
        String s = getComboBox().getSelectedItem().toString();
        if (IntelliJDeodorantBundle.message("scope.all.files").equals(s)) {
            return new AnalysisScope(project);
        } else if (IntelliJDeodorantBundle.message("scope.current.file").equals(s)) {
            return getCurrentFileScope();
        } else if (IntelliJDeodorantBundle.message("scope.opened.files").equals(s)) {
            return getOpenedFilesScope();
        }
        return customScope;
    }

    private AnalysisScope getOpenedFilesScope() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getOpenFiles();
        return new AnalysisScope(project, Arrays.asList(files));
    }

    private AnalysisScope getCurrentFileScope() {
        FileEditor currentEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (currentEditor != null) {
            VirtualFile currentFile = currentEditor.getFile();
            PsiFile file = PsiManager.getInstance(project).findFile(currentFile);
            if (file instanceof PsiJavaFile)
                return new AnalysisScope(project, Collections.singletonList(currentFile));
        }
        return null;
    }
}
