package utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PsiUtils {

    private static final String FILE_TYPE_NAME = "JAVA";

    public static List<PsiJavaFile> extractFiles(Project project) {
        final List<PsiJavaFile> javaFiles = new ArrayList<>();

        ProjectFileIndex.SERVICE.getInstance(project).iterateContent(
                file -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null && !psiFile.isDirectory()
                            && FILE_TYPE_NAME.equals(psiFile.getFileType().getName())) {
                        javaFiles.add((PsiJavaFile) psiFile);
                    }
                    return true;
                }
        );
        return javaFiles;
    }

    public static List<PsiClass> extractClasses(PsiJavaFile psiFile) {
        return Arrays.asList(psiFile.getClasses());
    }

    public static List<PsiMethod> extractMethods(PsiClass psiClass) {
        return Arrays.asList(psiClass.getMethods());
    }

}
