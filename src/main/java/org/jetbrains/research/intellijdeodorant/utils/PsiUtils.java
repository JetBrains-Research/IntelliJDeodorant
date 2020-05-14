package org.jetbrains.research.intellijdeodorant.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.infos.MethodCandidateInfo;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.editor.Document;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        out.append("::");
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
                (VirtualFile file) -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile instanceof PsiJavaFile && !psiFile.isDirectory()
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
        ArrayList<PsiClass> allClasses = new ArrayList<>();
        PsiClass[] psiClasses = psiFile.getClasses();
        for (PsiClass psiClass : psiClasses) {
            allClasses.add(psiClass);
            allClasses.addAll(Arrays.asList(psiClass.getInnerClasses()));
        }
        return allClasses;
    }

    public static List<PsiMethod> extractMethods(PsiClass psiClass) {
        return Arrays.asList(psiClass.getMethods());
    }

    public static boolean isChild(@NotNull PsiElement parent, @NotNull PsiElement child) {
        if (parent.equals(child)) return false;
        return child.getTextRange().getStartOffset() >= parent.getTextRange().getStartOffset()
                && child.getTextRange().getEndOffset() <= parent.getTextRange().getEndOffset();
    }

    public static boolean isTestClass(final @NotNull PsiClass psiClass) {
        PsiJavaFile file = (PsiJavaFile) psiClass.getContainingFile();
        return file != null && isInsideTestDirectory(file);
    }

    private static boolean isInsideTestDirectory(final @NotNull PsiJavaFile file) {
        Optional<PsiDirectory> optionalDirectory = getDirectoryWithRootPackageFor(file);

        if (!optionalDirectory.isPresent()) {
            return false;
        }

        PsiDirectory directory = optionalDirectory.get();

        while (directory != null) {
            String dirName = directory.getName().toLowerCase();
            if (dirName.equals("test") || dirName.equals("tests")) {
                return true;
            }

            directory = directory.getParentDirectory();
        }

        return false;
    }

    private static @NotNull
    Optional<PsiDirectory> getDirectoryWithRootPackageFor(final @NotNull PsiJavaFile file) {
        String packageName = file.getPackageName();
        String[] packageSequence;

        if ("".equals(packageName)) {
            packageSequence = new String[0];
        } else {
            packageSequence = packageName.split("\\.");
        }

        ArrayUtils.reverse(packageSequence);

        PsiDirectory directory = file.getParent();
        if (directory == null) {
            throw new IllegalStateException("File has no parent directory");
        }

        for (String packagePart : packageSequence) {
            if (!packagePart.equals(directory.getName())) {
                return Optional.empty();
            }

            directory = directory.getParentDirectory();
            if (directory == null) {
                return Optional.empty();
            }
        }

        return Optional.of(directory);
    }

    public static PsiMethod resolveMethod(PsiMethodCallExpression methodInvocation) {
        JavaResolveResult[] result = methodInvocation.getMethodExpression().multiResolve(false);
        for (JavaResolveResult resolveResult : result) {
            if (resolveResult instanceof MethodCandidateInfo) {
                MethodCandidateInfo candidateInfo = (MethodCandidateInfo) resolveResult;
                return candidateInfo.getElement();
            }
        }
        return null;
    }

    public static SmartPsiElementPointer<PsiElement> toPointer(@NotNull PsiElement psiElement) {
        return SmartPointerManager.createPointer(psiElement);
    }

    public static SmartPsiElementPointer<PsiExpression> toPointer(@NotNull PsiExpression psiElement) {
        return SmartPointerManager.createPointer(psiElement);
    }

    public static int getNumberOfLinesInMethod(PsiMethod psiMethod) {
        final Document document = PsiDocumentManager.getInstance(psiMethod.getProject()).getDocument(psiMethod.getContainingFile());
        assert document != null;
        final PsiIdentifier methodNameIdentifier = psiMethod.getNameIdentifier();
        int start;
        final TextRange range = psiMethod.getTextRange();
        if (methodNameIdentifier != null) {
            start = document.getLineNumber(methodNameIdentifier.getTextOffset());
        } else {
            start = document.getLineNumber(range.getStartOffset());
        }
        final int end = document.getLineNumber(range.getEndOffset());
        return end - start;
    }

    public static int getMethodStatementCount(PsiMethod psiMethod) {
        PsiCodeBlock codeBlock = psiMethod.getBody();
        return codeBlock == null ? 0 : codeBlock.getStatementCount();
    }

}
