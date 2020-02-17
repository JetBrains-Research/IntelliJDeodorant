package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * Extracts statements that responsible for calculating variable's value into new method.
 */
public class MyExtractMethodProcessor extends ExtractMethodProcessor {
    @NotNull
    private final PsiVariable variableCriterion;

    /**
     * Constructs a processor for extracting statements into a new method.
     *
     * @param project           project that contains statements.
     * @param editor            editor that shows extracted statements.
     * @param elements          statements to extract.
     * @param forcedReturnType  return type.
     * @param refactoringName   refactoring name.
     * @param initialMethodName initial name of new method.
     * @param helpId            id of help.
     * @param psiClass          source class.
     * @param psiVariable       variable, which calculation of value can be extracted into a separate method.
     */
    public MyExtractMethodProcessor(@NotNull Project project, Editor editor, PsiElement[] elements, @NotNull PsiType forcedReturnType,
                                    String refactoringName, String initialMethodName, String helpId, @NotNull PsiClass psiClass,
                                    @NotNull PsiVariable psiVariable) {
        super(project, editor, elements, forcedReturnType, refactoringName, initialMethodName, helpId);
        this.myReturnType = forcedReturnType;
        this.myThrownExceptions = new PsiClassType[0];
        this.myTargetClass = psiClass;
        this.variableCriterion = psiVariable;
        this.myOutputVariable = psiVariable;
        this.myOutputVariables = new PsiVariable[1];
        this.myOutputVariables[0] = psiVariable;
    }

    /**
     * Checks if a variable is declared inside statements to extract.
     *
     * @param variable variable to check.
     * @return true if a variable is declared inside statements to extract and false otherwise.
     */
    @Override
    public boolean isDeclaredInside(PsiVariable variable) {
        if (variable instanceof ImplicitVariable) return false;
        int startOffset;
        int endOffset;
        for (PsiElement element : myElements) {
            startOffset = element.getTextRange().getStartOffset();
            endOffset = element.getTextRange().getEndOffset();
            PsiIdentifier nameIdentifier = variable.getNameIdentifier();
            if (nameIdentifier == null) return false;
            final TextRange range = nameIdentifier.getTextRange();
            if (range == null) return false;
            int offset = range.getStartOffset();
            if (startOffset <= offset && offset <= endOffset) return true;
        }
        return false;
    }

    /**
     * Sets output variable for new method.
     */
    public void setOutputVariable() {
        myOutputVariables = new PsiVariable[1];
        myOutputVariables[0] = variableCriterion;
        myOutputVariable = variableCriterion;
    }
}
