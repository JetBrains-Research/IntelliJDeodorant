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
    final ArrayList<ASTSlice> candidates;
    private final String qualifiedMethodName;

    /**
     * Creates refactoring instance.
     *
     * @param slices slice group that consist of candidates to extract.
     */
    public ExtractMethodCandidateGroup(Set<ASTSlice> slices) {
        this.method = toPointer(slices.iterator().next().getSourceMethodDeclaration());
        this.candidates = new ArrayList<>(slices);
        this.qualifiedMethodName = getHumanReadableName(method.getElement());
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
    public ArrayList<ASTSlice> getCandidates() {
        return candidates;
    }

    @NotNull
    @Override
    public String getDescription() {
        Optional<PsiMethod> method = getOptionalMethod();
        if (!method.isPresent()) return "";
        String methodName = getHumanReadableName(method.get());
        StringBuilder sb = new StringBuilder();
        Iterator<ASTSlice> candidatesIterator = candidates.iterator();
        for (int i = 0; i < candidates.size(); i++) {
            ASTSlice slice = candidatesIterator.next();
            sb.append(methodName).append(DELIMITER);
            sb.append(slice.getLocalVariableCriterion().getName()).append(DELIMITER);
            sb.append("B").append(slice.getBoundaryBlock().getId()).append(DELIMITER);
            sb.append(slice.getNumberOfDuplicatedStatements()).append("/").append(slice.getNumberOfSliceStatements());
            sb.append('\n');
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getExportDefaultFilename() {
        return "Long-Method";
    }

    @Override
    public String toString() {
        return qualifiedMethodName;
    }
}
