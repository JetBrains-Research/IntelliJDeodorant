package utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PsiUtils {

    private static final String FILE_TYPE_NAME = "JAVA";

    public static String calculateSignature(PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();
        final String className;
        if (containingClass != null) {
            className = containingClass.getQualifiedName();
        } else {
            className = "";
        }
        final String methodName = method.getName();
        final StringBuilder out = new StringBuilder(50);
        out.append(className);
        out.append('.');
        out.append(methodName);
        out.append('(');
        final PsiParameterList parameterList = method.getParameterList();
        final PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i != 0) {
                out.append(',');
            }
            final PsiType parameterType = parameters[i].getType();
            final String parameterTypeText = parameterType.getPresentableText();
            out.append(parameterTypeText);
        }
        out.append(')');
        return out.toString();
    }

    public static String getHumanReadableName(@Nullable PsiElement element) {
        if (element instanceof PsiMethod) {
            return calculateSignature((PsiMethod) element);
        } else if (element instanceof PsiClass) {
            if (element instanceof PsiAnonymousClass) {
                return getHumanReadableName(((PsiAnonymousClass) element).getBaseClassReference().resolve());
            }
            return ((PsiClass) element).getQualifiedName();
        } else if (element instanceof PsiField) {
            final PsiMember field = (PsiMember) element;
            return getHumanReadableName(field.getContainingClass()) + "." + field.getName();
        }
        return "???";
    }

    public static PsiClass findClass(String className, Project project) {
        return JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));
    }

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

    public static boolean isPrimitive(PsiType psiType) {
        List<String> primitiveTypes = Arrays.asList("boolean", "byte", "char", "short", "int", "long",
                "float", "double", "void");
        return primitiveTypes.contains(psiType.getPresentableText());
    }

    public static List<PsiClass> extractClasses(PsiJavaFile psiFile) {
        return Arrays.asList(psiFile.getClasses());
    }

    public static List<PsiMethod> extractMethods(PsiClass psiClass) {
        return Arrays.asList(psiClass.getMethods());
    }

    public static boolean isChild(@NotNull PsiElement parent, @NotNull PsiElement child) {
        if (parent.equals(child)) return false;
        return child.getTextRange().getStartOffset() >= parent.getTextRange().getStartOffset()
                && child.getTextRange().getEndOffset() <= parent.getTextRange().getEndOffset();
    }
}
