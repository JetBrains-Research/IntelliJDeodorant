package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import java.util.Set;

public class ResourceInfo {

    public static class ICompilationUnitNotFoundException extends Exception {
        private static final long serialVersionUID = 1L;

        public ICompilationUnitNotFoundException(String message) {
            super(message);
        }
    }

    private final String sourceFolder;
    private final PsiFile iCompilationUnit;
    private final PsiFile compilationUnit;
    private final String packageName;
    private final String className;
    private final String givenPath;

    public ResourceInfo(String sourceFolder, PsiFile iCompilationUnit, String givenPath) {
        this.sourceFolder = sourceFolder;
        this.iCompilationUnit = iCompilationUnit;
        this.givenPath = givenPath;
        this.compilationUnit = iCompilationUnit;
        if (this.compilationUnit.getContainingDirectory() != null)
            packageName = this.compilationUnit.getContainingDirectory().getName();
        else
            packageName = "";
        this.className = iCompilationUnit.getName().substring(0, iCompilationUnit.getName().lastIndexOf("."));
    }

    public String getSourceFolder() {
        return sourceFolder;
    }

    public PsiFile getICompilationUnit() {
        return iCompilationUnit;
    }

    public PsiFile getCompilationUnit() {
        return compilationUnit;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public static ResourceInfo getResourceInfo(Project jProject, String fullResourceName, boolean isAbsoluteFilePath) throws ICompilationUnitNotFoundException {

        // First try the given path, if not found, prepend src dir
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(fullResourceName);
        PsiFile iCompilationUnit = null;
        if (virtualFile != null) {
            iCompilationUnit = PsiManager.getInstance(jProject).findFile(virtualFile);
        }
        Set<String> allSrcDirectories = JavaModelUtility.getAllSourceDirectories(jProject);

        if (iCompilationUnit != null) {
            for (String srcDirectory : allSrcDirectories) {
                if (fullResourceName.startsWith(srcDirectory)) {
                    return new ResourceInfo(srcDirectory, iCompilationUnit, fullResourceName);
                }
            }
        }

        for (String srcDirectory : allSrcDirectories) {
            String fullPath = "";
            if (isAbsoluteFilePath) {
                int indexOfSrcDirectorInTheAbsolutePath = fullResourceName.indexOf(srcDirectory);
                if (indexOfSrcDirectorInTheAbsolutePath >= 0) {
                    fullPath = fullResourceName.substring(indexOfSrcDirectorInTheAbsolutePath);
                } else {
                    continue;
                }
            } else {
                fullPath = srcDirectory + "/" + fullResourceName;
            }
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(fullPath);
            PsiFile psiFile = null;
            if (vFile != null) {
                psiFile = PsiManager.getInstance(jProject).findFile(vFile);
            }
            if (psiFile != null) {
                iCompilationUnit = PsiFileFactory.getInstance(jProject).createFileFromText(psiFile.getText(), psiFile);
                if (iCompilationUnit != null && iCompilationUnit.isValid()) {
                    return new ResourceInfo(srcDirectory, iCompilationUnit, fullResourceName);
                }
            }
        }

        throw new ICompilationUnitNotFoundException(String.format("ICompilationUnit not found for %s", fullResourceName));
    }

    public String getFullPath() {
        if (this.iCompilationUnit != null)
            return this.iCompilationUnit.getContainingDirectory().getName();
        return this.givenPath;
    }
}