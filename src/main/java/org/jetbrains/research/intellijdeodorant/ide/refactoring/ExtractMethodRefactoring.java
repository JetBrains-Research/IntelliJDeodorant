package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSliceGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.getHumanReadableName;

/**
 * Representation of a refactoring, which suggests to extract code into separate method.
 */
public class ExtractMethodRefactoring implements Refactoring {
    private final @NotNull
    SmartPsiElementPointer<PsiMethod> method;
    private final @NotNull
    Set<ASTSlice> candidates;

    /**
     * Creates refactoring instance.
     *
     * @param sliceGroup slice group that consist of candidates to extract.
     */
    public ExtractMethodRefactoring(final @NotNull ASTSliceGroup sliceGroup) {
        this.method = ApplicationManager.getApplication().runReadAction(
                (Computable<SmartPsiElementPointer<PsiMethod>>) () ->
                        SmartPointerManager.getInstance(sliceGroup.getSourceMethodDeclaration().getProject())
                                .createSmartPsiElementPointer(sliceGroup.getSourceMethodDeclaration())
        );
        this.candidates = sliceGroup.getCandidates();
    }

    /**
     * Returns method from which code is proposed to be extracted into separate method.
     */
    public @NotNull
    PsiMethod getMethod() {
        return Optional.ofNullable(method.getElement()).orElseThrow(() ->
                new IllegalStateException("Cannot get method. Reference is invalid."));
    }

    /**
     * Returns method that is moved in this refactoring.
     */
    public @NotNull
    Optional<PsiMethod> getOptionalMethod() {
        return Optional.ofNullable(method.getElement());
    }

    @NotNull
    public Set<ASTSlice> getCandidates() {
        return candidates;
    }

    @NotNull
    @Override
    public String getDescription() {
        Optional<PsiMethod> method = getOptionalMethod();
        return method.map(psiMethod -> String.join(DELIMITER, getHumanReadableName(psiMethod.getContainingClass()),
                getHumanReadableName(psiMethod), candidates.iterator().next().getLocalVariableCriterion().getName())).orElse("");
    }
}
