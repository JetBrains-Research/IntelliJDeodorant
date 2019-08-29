package navigation;

import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;

public class ReferenceNavigator {
    private Reference reference;

    public ReferenceNavigator(Reference reference) {
        this.reference = reference;
    }

    public void navigateToReference(Project project) {
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).openFile(reference.getVirtualFile(), true);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof NavigatableFileEditor)
                ((NavigatableFileEditor) fileEditor).navigateTo(reference.location());
            scrollToCaret(fileEditor);
        }
    }

    private void scrollToCaret(FileEditor fileEditor) {
        if (fileEditor instanceof TextEditor)
            ((TextEditor) fileEditor).getEditor().getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }
}