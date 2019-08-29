package navigation;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class DataHolder {
    private Editor EDITOR;
    private Module MODULE;
    private PsiElement PSI_ELEMENT;
    private Navigatable NAVIGATABLE;
    private VirtualFile VIRTUAL_FILE;
    private PsiFile PSI_FILE;

    private DataHolder() {
    }

    private static DataHolder _instance = null;

    public static DataHolder getInstance() {
        return _instance = (_instance == null ? new DataHolder() : _instance);
    }

    public void initDataHolder(DataContext dataContext) {
        EDITOR = dataContext.getData(DataKeys.EDITOR);
        MODULE = dataContext.getData(DataKeys.MODULE);
        PSI_ELEMENT = dataContext.getData(DataKeys.PSI_ELEMENT);
        NAVIGATABLE = dataContext.getData(DataKeys.NAVIGATABLE);
        VIRTUAL_FILE = dataContext.getData(DataKeys.VIRTUAL_FILE);
        PSI_FILE = dataContext.getData(DataKeys.PSI_FILE);
    }
}