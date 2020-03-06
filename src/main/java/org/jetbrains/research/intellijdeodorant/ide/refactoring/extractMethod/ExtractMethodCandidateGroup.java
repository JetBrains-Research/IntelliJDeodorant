package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.Refactoring;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.*;

/**
 * Representation of a refactoring, which suggests to extract code into separate method.
 */
public class ExtractMethodCandidateGroup implements Refactoring {
    private final @NotNull
    SmartPsiElementPointer<PsiElement> method;
    private @NotNull
    Set<ASTSlice> candidates;

    /**
     * Creates refactoring instance.
     *
     * @param slices slice group that consist of candidates to extract.
     */
    public ExtractMethodCandidateGroup(Set<ASTSlice> slices) {
        this.method = toPointer(slices.iterator().next().getSourceMethodDeclaration());
        this.candidates = slices;
    }

    /**
     * Returns a method from which code is proposed to be extracted into a separate method.
     */
    public @NotNull
    PsiMethod getMethod() {
        return Optional.ofNullable((PsiMethod) method.getElement()).orElseThrow(() ->
                new IllegalStateException("Cannot get method. Reference is invalid."));
    }

    /**
     * Returns a method that is proposed to be moved in this refactoring.
     */
    public @NotNull
    Optional<PsiMethod> getOptionalMethod() {
        return Optional.ofNullable((PsiMethod) method.getElement());
    }

    @NotNull
    public Set<ASTSlice> getCandidates() {
        return candidates;
    }

    @NotNull
    @Override
    public String getDescription() {
        Optional<PsiMethod> method = getOptionalMethod();
        return method.map(psiMethod ->
                String.join(DELIMITER, getHumanReadableName(psiMethod),
                        candidates.iterator().next().getLocalVariableCriterion().getName())).orElse("");
    }

    @NotNull
    @Override
    public String getExportDefaultFilename() {
        return "Long-Method";
    }

    @Override
    public String toString() {
        PsiMethod psiMethod = getMethod();
        return psiMethod.getContainingClass() == null ? "" :
                psiMethod.getContainingClass().getQualifiedName() + "::" + psiMethod.getName();
    }
}
