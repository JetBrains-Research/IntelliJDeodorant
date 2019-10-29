package refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import core.ast.decomposition.cfg.ASTSlice;
import core.ast.decomposition.cfg.ASTSliceGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Representation of a refactoring, which suggests to extract code into separate method.
 */
public class ExtractMethodRefactoring {
    private final @NotNull
    SmartPsiElementPointer<PsiMethod> method;
    private final @NotNull
    Set<ASTSlice> candidates;

    /**
     * Creates refactoring instance.
     *
     * @param sliceGroup slice group that consist of candidates to extract.
     */
    public ExtractMethodRefactoring(
            final @NotNull ASTSliceGroup sliceGroup) {
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

    @NotNull
    public Set<ASTSlice> getCandidates() {
        return candidates;
    }
}
