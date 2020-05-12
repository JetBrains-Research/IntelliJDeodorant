package org.jetbrains.research.intellijdeodorant.core.distance;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects information about project: files, classes, and methods.
 */
public class ProjectInfo {
    private final List<PsiClass> psiClasses;
    private final List<PsiMethod> psiMethods;
    private final Project project;

    public ProjectInfo(AnalysisScope scope, boolean analyseAllFiles) {
        this.project = scope.getProject();
        List<PsiJavaFile> psiFiles = analyseAllFiles ? PsiUtils.extractFiles(project) : PsiUtils.extractFiles(project).stream().filter(scope::contains).collect(Collectors.toList());
        this.psiClasses = psiFiles.stream()
                .flatMap(psiFile -> PsiUtils.extractClasses(psiFile).stream())
                .collect(Collectors.toList());

        this.psiMethods = psiClasses.stream()
                .flatMap(psiClass -> PsiUtils.extractMethods(psiClass).stream())
                .collect(Collectors.toList());
    }

    public boolean containsSuperMethodInvocation(final @NotNull PsiMethod psiMethod) {
        final Ref<Boolean> resultRef = new Ref<>(true);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(
                    final @NotNull PsiMethodCallExpression expression
            ) {
                super.visitMethodCallExpression(expression);

                if (expression.getMethodExpression().getQualifierExpression() instanceof PsiSuperExpression) {
                    resultRef.set(false);
                }
            }
        }.visitElement(psiMethod);

        return resultRef.get();
    }

    public boolean overridesMethod(final @NotNull PsiMethod psiMethod) {
        return AnnotationUtil.findAnnotation(psiMethod, "Override") == null &&
                psiMethod.findSuperMethods().length == 0;
    }

    public List<PsiClass> getClasses() {
        return psiClasses;
    }

    public List<PsiMethod> getMethods() {
        return psiMethods;
    }

    public Project getProject() {
        return project;
    }
}
