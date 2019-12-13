package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.getHumanReadableName;

/**
 * Representation of a refactoring, which moves method to a target class.
 * Once the method moved, the corresponding pointer becomes invalid.
 */
public class MoveMethodRefactoring implements Refactoring {
    private final @NotNull
    SmartPsiElementPointer<PsiMethod> method;
    private final @NotNull
    SmartPsiElementPointer<PsiClass> targetClass;
    private final @NotNull String qualifiedMethodName;
    private final int sourceAccessedMembers;
    private final int targetAccessedMembers;

    /**
     * Creates refactoring.
     *
     * @param method      a method that is moved in this refactoring.
     * @param targetClass destination class in which given method is placed in this refactoring.
     */
    public MoveMethodRefactoring(
            final @NotNull PsiMethod method,
            final @NotNull PsiClass targetClass,
            int sourceAccessedMembers,
            int targetAccessedMembers
    ) {
        this.method = ApplicationManager.getApplication().runReadAction(
                (Computable<SmartPsiElementPointer<PsiMethod>>) () ->
                        SmartPointerManager.getInstance(method.getProject()).createSmartPsiElementPointer(method)
        );
        this.targetClass = ApplicationManager.getApplication().runReadAction(
                (Computable<SmartPsiElementPointer<PsiClass>>) () ->
                        SmartPointerManager.getInstance(targetClass.getProject()).createSmartPsiElementPointer(targetClass)
        );
        this.qualifiedMethodName = getHumanReadableName(this.method.getElement());
        this.sourceAccessedMembers = sourceAccessedMembers;
        this.targetAccessedMembers = targetAccessedMembers;
    }

    /**
     * Returns method that is moved in this refactoring.
     */
    public @NotNull
    Optional<PsiMethod> getOptionalMethod() {
        return Optional.ofNullable(method.getElement());
    }

    /**
     * Returns method that is moved in this refactoring.
     */
    public @NotNull
    PsiMethod getMethod() {
        return Optional.ofNullable(method.getElement()).orElseThrow(() ->
                new IllegalStateException("Cannot get method. Reference is invalid."));
    }

    public @Nullable
    Optional<PsiClass> getOptionalContainingClass() {
        return method.getElement() == null ?
                Optional.empty() : Optional.ofNullable(method.getElement().getContainingClass());
    }

    @NotNull
    public PsiClass getContainingClass() {
        return Optional.ofNullable(getMethod().getContainingClass())
                .orElseThrow(() -> new IllegalStateException("No containing class."));
    }

    /**
     * Returns class in which method is placed in this refactoring
     */
    public @NotNull
    Optional<PsiClass> getOptionalTargetClass() {
        return Optional.ofNullable(targetClass.getElement());
    }

    /**
     * Returns class in which method is placed in this refactoring
     */
    public @NotNull
    PsiClass getTargetClass() {
        return Optional.ofNullable(targetClass.getElement()).orElseThrow(() ->
                new IllegalStateException("Cannot get target class. Reference is invalid."));
    }

    @NotNull
    public <R> R accept(final @NotNull RefactoringVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoveMethodRefactoring that = (MoveMethodRefactoring) o;

        return method.equals(that.method) && targetClass.equals(that.targetClass);
    }

    public boolean methodEquals(@NotNull MoveMethodRefactoring that) {
        if (this == that) return true;

        return method.equals(that.method);
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + targetClass.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MoveToClassRefactoring{" +
                "method=" + method +
                ", targetClass=" + targetClass +
                '}';
    }

    @NotNull
    @Override
    public String getDescription() {
        return getHumanReadableName(method.getElement()) + DELIMITER + getHumanReadableName(targetClass.getElement());
    }

    @NotNull
    public String getQualifiedMethodName() {
        return qualifiedMethodName;
    }

    public int getSourceAccessedMembers() {
        return sourceAccessedMembers;
    }

    public int getTargetAccessedMembers() {
        return targetAccessedMembers;
    }
}