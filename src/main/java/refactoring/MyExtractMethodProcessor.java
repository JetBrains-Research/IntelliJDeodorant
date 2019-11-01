package refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.extractMethod.*;
import com.intellij.refactoring.util.InlineUtil;
import com.intellij.refactoring.util.VariableData;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Extracts statements that responsible for calculating variable's value into new method.
 */
public class MyExtractMethodProcessor extends ExtractMethodProcessor {
    @NotNull
    private final PsiVariable variableCriterion;
    private final PsiElementFactory myElementFactory;

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
        this.myElementFactory = JavaPsiFacade.getElementFactory(PsiManager.getInstance(myProject).getProject());
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
     * Collects local variables that declared inside statements to extract.
     *
     * @return set of local variables.
     */
    @NotNull
    @Override
    protected Set<PsiVariable> getEffectivelyLocalVariables() {
        HashSet<PsiVariable> variables = new HashSet<>();
        for (PsiElement psiElement : myElements) {
            Collection<PsiReferenceExpression> referenceExpressions =
                    PsiTreeUtil.findChildrenOfType(psiElement, PsiReferenceExpression.class);
            for (PsiReferenceExpression referenceExpression : referenceExpressions) {
                if (referenceExpression.resolve() != null && referenceExpression.resolve() instanceof PsiLocalVariable) {
                    variables.add((PsiVariable) referenceExpression.resolve());
                }
            }
        }
        return variables;
    }

    /**
     * Collects used local variables, fields and method parameters.
     *
     * @return set of used variables.
     */
    private Set<PsiVariable> getUsedVariables() {
        HashSet<PsiVariable> variables = new HashSet<>();
        for (PsiElement psiElement : myElements) {
            Collection<PsiReferenceExpression> referenceExpressions =
                    PsiTreeUtil.findChildrenOfType(psiElement, PsiReferenceExpression.class);
            for (PsiReferenceExpression referenceExpression : referenceExpressions) {
                if (referenceExpression.resolve() != null && referenceExpression.resolve() instanceof PsiVariable) {
                    variables.add((PsiVariable) referenceExpression.resolve());
                }
            }
        }
        return variables;
    }

    /**
     * Creates new method in class and extracts statements into the method.
     */
    @Override
    public void doExtract() {
        PsiMethod newMethod = generateEmptyMethod(myMethodName, null);
        prepareMethodBody(newMethod);
        myExtractedMethod = addExtractedMethod(newMethod);
        setMethodCall(generateMethodCall(null, true));

        final String outputVariableName = variableCriterion.getName();
        if (isDeclaredInside(variableCriterion) && outputVariableName != null) {
            PsiDeclarationStatement statement = myElementFactory.createVariableDeclarationStatement(outputVariableName,
                    variableCriterion.getType(), getMethodCall());
            statement = (PsiDeclarationStatement) JavaCodeStyleManager.getInstance(myProject)
                    .shortenClassReferences(addToMethodCallLocation(statement));
            PsiVariable var = (PsiVariable) statement.getDeclaredElements()[0];
            setMethodCall((PsiMethodCallExpression) var.getInitializer());
        } else {
            PsiExpressionStatement statement = (PsiExpressionStatement) myElementFactory.createStatementFromText(outputVariableName + "=x;", null);
            statement = (PsiExpressionStatement) addToMethodCallLocation(statement);
            PsiAssignmentExpression assignment = (PsiAssignmentExpression) statement.getExpression();
            setMethodCall((PsiMethodCallExpression) Objects.requireNonNull(assignment.getRExpression()).replace(getMethodCall()));
        }

        for (PsiElement element : myElements) {
            element.delete();
        }
    }

    /**
     * Sets parameters for new method.
     */
    @Override
    public void setDataFromInputVariables() {
        final List<VariableData> variables = myInputVariables.getInputVariables();
        final Set<PsiVariable> usedVariables = getUsedVariables();
        ArrayList<PsiVariable> inputVariables = new ArrayList<>();
        for (VariableData data : variables) {
            if (usedVariables.contains(data.variable)) {
                inputVariables.add(data.variable);
            }
        }
        myInputVariables = new InputVariables(inputVariables, myProject, new LocalSearchScope(myElements), true);
        myVariableDatum = myInputVariables.getInputVariables().toArray(new VariableData[0]);
    }

    /**
     * Sets output variable for new method.
     */
    public void setOutputVariable() {
        myOutputVariables = new PsiVariable[1];
        myOutputVariables[0] = variableCriterion;
        myOutputVariable = variableCriterion;
    }

    /**
     * Prepares body for a new method.
     *
     * @param newMethod new method with empty body.
     */
    private void prepareMethodBody(PsiMethod newMethod) {
        PsiCodeBlock body = newMethod.getBody();
        if (body != null) {
            for (PsiElement psiElement : myElements) {
                body.add(psiElement);
            }

            PsiReturnStatement returnStatement;
            if (myNullConditionalCheck) {
                returnStatement = (PsiReturnStatement) myElementFactory.createStatementFromText("return null;", null);
            } else if (myOutputVariable != null) {
                returnStatement = (PsiReturnStatement) myElementFactory.createStatementFromText("return " + myOutputVariable.getName() + ";", null);
            } else if (myGenerateConditionalExit) {
                returnStatement = (PsiReturnStatement) myElementFactory.createStatementFromText("return true;", null);
            } else {
                returnStatement = (PsiReturnStatement) myElementFactory.createStatementFromText("return;", null);
            }
            final PsiReturnStatement insertedReturnStatement = (PsiReturnStatement) body.add(returnStatement);
            if (myOutputVariables.length == 1) {
                final PsiExpression returnValue = insertedReturnStatement.getReturnValue();
                if (returnValue instanceof PsiReferenceExpression) {
                    final PsiVariable variable = ObjectUtils.tryCast(((PsiReferenceExpression) returnValue).resolve(), PsiVariable.class);
                    if (variable != null && Comparing.strEqual(variable.getName(), myOutputVariable.getName())) {
                        final PsiStatement statement = PsiTreeUtil.getPrevSiblingOfType(insertedReturnStatement, PsiStatement.class);
                        if (statement instanceof PsiDeclarationStatement) {
                            final PsiElement[] declaredElements = ((PsiDeclarationStatement) statement).getDeclaredElements();
                            if (ArrayUtil.find(declaredElements, variable) != -1) {
                                InlineUtil.inlineVariable(variable, PsiUtil.skipParenthesizedExprDown(variable.getInitializer()),
                                        (PsiReferenceExpression) returnValue);
                                variable.delete();
                            }
                        }
                    }
                }
            }
        }
    }
}
