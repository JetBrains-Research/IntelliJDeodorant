package utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
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

    public static Optional<PsiField> whoseSetter(PsiMethod method) {
        if (method.isConstructor()) {
            return Optional.empty();
        }

        if (method.getParameterList().getParametersCount() != 1) {
            return Optional.empty();
        }

        PsiParameter parameter = method.getParameterList().getParameters()[0];
        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Optional.empty();
        }

        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return Optional.empty();
        }

        PsiStatement theStatement = statements[0];
        if (!(theStatement instanceof PsiExpressionStatement)) {
            return Optional.empty();
        }

        PsiExpression expression = ((PsiExpressionStatement) theStatement).getExpression();
        if (!(expression instanceof PsiAssignmentExpression)) {
            return Optional.empty();
        }

        PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) expression;
        PsiExpression leftExpression = assignmentExpression.getLExpression();
        PsiExpression rightExpression = assignmentExpression.getRExpression();

        if (!(leftExpression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiField field = getReferencedField((PsiReferenceExpression) leftExpression);
        if (field == null) {
            return Optional.empty();
        }

        if (!(rightExpression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) rightExpression;
        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        if (resolveResult.getElement() != parameter) {
            return Optional.empty();
        }

        return Optional.of(field);
    }

    public static Optional<PsiField> whoseGetter(PsiMethod method) {
        if (method.getParameterList().getParametersCount() != 0) {
            return Optional.empty();
        }

        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Optional.empty();
        }

        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return Optional.empty();
        }

        PsiStatement theStatement = statements[0];
        if (!(theStatement instanceof PsiReturnStatement)) {
            return Optional.empty();
        }

        PsiReturnStatement returnStatement = (PsiReturnStatement) theStatement;
        PsiExpression expression = returnStatement.getReturnValue();

        if (!(expression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiField field = getReferencedField((PsiReferenceExpression) expression);
        if (field == null) {
            return Optional.empty();
        }

        PsiClass fieldClass = field.getContainingClass();
        if (fieldClass == null || !fieldClass.equals(method.getContainingClass())) { // what if it's just a super class?
            return Optional.empty();
        }

        if (!field.getType().equals(method.getReturnType())) {
            return Optional.empty();
        }

        return Optional.of(field);
    }

    private static PsiField getReferencedField(
            final PsiReferenceExpression referenceExpression
    ) {
        if (referenceExpression.isQualified()) {
            PsiExpression qualifierExpression = referenceExpression.getQualifierExpression();
            if (!(qualifierExpression instanceof PsiThisExpression)) {
                return null;
            }
        }

        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        PsiElement referencedElement = resolveResult.getElement();
        if (referencedElement == null) {
            return null;
        }

        if (!(referencedElement instanceof PsiField)) {
            return null;
        }

        return (PsiField) referencedElement;
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

}
