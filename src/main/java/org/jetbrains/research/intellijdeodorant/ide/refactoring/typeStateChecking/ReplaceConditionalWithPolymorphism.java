package org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.StatementExtractor;
import org.jetbrains.research.intellijdeodorant.inheritance.InheritanceTree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class ReplaceConditionalWithPolymorphism extends PolymorphismRefactoring {
    private final PsiVariable returnedVariable;
    private final Set<PsiClassType> thrownExceptions;
    private PsiVariable typeVariable;
    private final PsiMethodCallExpression typeMethodInvocation;

    public ReplaceConditionalWithPolymorphism(PsiFile sourceFile,
                                              Project project,
                                              PsiClass sourceTypeDeclaration,
                                              TypeCheckElimination typeCheckElimination) {
        super(sourceFile, project, sourceTypeDeclaration, typeCheckElimination);
        this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
        this.thrownExceptions = typeCheckElimination.getThrownExceptions();
        if (typeCheckElimination.getTypeField() != null) {
            this.typeVariable = typeCheckElimination.getTypeField();
        } else if (typeCheckElimination.getTypeLocalVariable() != null) {
            this.typeVariable = typeCheckElimination.getTypeLocalVariable();
        }
        this.typeMethodInvocation = typeCheckElimination.getTypeMethodInvocation();
    }

    @Override
    public void apply() {
        modifyInheritanceHierarchy();
        modifyClient();
    }

    private void modifyClient() {
        String methodName = typeCheckElimination.getAbstractMethodName();
        String qualifierExpressionText = "";

        if (typeVariable != null) {
            qualifierExpressionText = typeVariable.getName() + ".";
        } else if (typeMethodInvocation != null) {
            qualifierExpressionText = typeMethodInvocation.getText() + ".";
        }

        List<String> methodCallArguments = new ArrayList<>();

        if (returnedVariable != null && !typeCheckElimination.returnedVariableDeclaredAndReturnedInBranches()) {
            methodCallArguments.add(returnedVariable.getName());
        }
        for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
            if (!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
                methodCallArguments.add(abstractMethodParameter.getName());
            }
        }
        for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
            if (!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
                methodCallArguments.add(fragment.getName());
            }
        }
        if (sourceTypeRequiredForExtraction()) {
            methodCallArguments.add("this");
            setPublicModifierToSourceTypeDeclaration();
        }

        StringJoiner argumentsJoiner = new StringJoiner(", ", "(", ");");
        methodCallArguments.forEach(argumentsJoiner::add);
        PsiStatement resultingStatement;

        if (returnedVariable != null && !typeCheckElimination.returnedVariableDeclaredAndReturnedInBranches()) {
            resultingStatement = elementFactory.createStatementFromText(
                    returnedVariable.getName() + "=" + qualifierExpressionText + methodName + argumentsJoiner.toString(),
                    null
            );

        } else if (typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
            resultingStatement = elementFactory.createStatementFromText(
                    "return " + qualifierExpressionText + methodName + argumentsJoiner.toString(),
                    null
            );
        } else {
            resultingStatement = elementFactory.createStatementFromText(
                    qualifierExpressionText + methodName + argumentsJoiner.toString(),
                    null
            );
        }
        typeCheckElimination.getTypeCheckCodeFragment().replace(resultingStatement);
        codeStyleManager.reformat(resultingStatement);

        generateGettersForAccessedFields();
        generateSettersForAssignedFields();
        setPublicModifierToAccessedMethods();
    }

    private void setPublicModifierToSourceTypeDeclaration() {
        InheritanceTree tree = null;
        if (typeCheckElimination.getExistingInheritanceTree() != null) {
            tree = typeCheckElimination.getExistingInheritanceTree();
        } else if (typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes() != null) {
            tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
        }

        String abstractClassName = null;
        if (tree != null) {
            DefaultMutableTreeNode root = tree.getRootNode();
            abstractClassName = (String) root.getUserObject();
        }
        String sourcePackageName = PsiUtil.getPackageName(sourceTypeDeclaration);
        if (sourcePackageName != null && abstractClassName != null && abstractClassName.contains(".")) {
            String targetPackageName = abstractClassName.substring(0, abstractClassName.lastIndexOf("."));
            if (!sourcePackageName.equals(targetPackageName)) {
                PsiUtil.setModifierProperty(sourceTypeDeclaration, PsiModifier.PUBLIC, true);
            }
        }
    }

    private void modifyInheritanceHierarchy() {
        String abstractClassFullyQualifiedName = typeCheckElimination.getAbstractClassName();
        PsiClass abstractClass = ClassUtil.findPsiClass(PsiManager.getInstance(project), abstractClassFullyQualifiedName);

        Set<PsiField> accessedFields = typeCheckElimination.getAccessedFields();
        Set<PsiField> assignedFields = typeCheckElimination.getAssignedFields();
        Set<PsiMethod> accessedMethods = typeCheckElimination.getAccessedMethods();
        Set<PsiMethod> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
        Set<PsiField> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
        Set<PsiField> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();

        if (!typeCheckElimination.getSubclassNames().contains(abstractClassFullyQualifiedName)) {
            boolean isAbstract = abstractClass.getModifierList().hasExplicitModifier(PsiModifier.ABSTRACT);
            if (!isAbstract && !abstractClass.isInterface()) {
                PsiUtil.setModifierProperty(abstractClass, PsiModifier.ABSTRACT, true);
            }

            PsiMethod abstractMethod = createPolymorphicMethodHeader();
            PsiUtil.setModifierProperty(abstractMethod, PsiModifier.ABSTRACT, true);
            abstractMethod.getBody().replace(semicolon);
            abstractClass.add(abstractMethod);
        }

        List<ArrayList<PsiStatement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
        List<String> subclassNames = typeCheckElimination.getSubclassNames();
        DefaultMutableTreeNode root = typeCheckElimination.getExistingInheritanceTree().getRootNode();
        Enumeration enumeration = root.children();

        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) enumeration.nextElement();
            String childClassName = (String) child.getUserObject();
            if (!subclassNames.contains(childClassName))
                subclassNames.add(childClassName);
        }

        for (int i = 0; i < subclassNames.size(); i++) {
            ArrayList<PsiStatement> statements;
            DefaultMutableTreeNode remainingIfStatementExpression = null;
            if (i < typeCheckStatements.size()) {
                statements = typeCheckStatements.get(i);
                PsiExpression expression = typeCheckElimination.getExpressionCorrespondingToTypeCheckStatementList(statements);
                remainingIfStatementExpression = typeCheckElimination.getRemainingIfStatementExpression(expression);
            } else {
                statements = typeCheckElimination.getDefaultCaseStatements();
            }
            PsiClass subClass = ClassUtil.findPsiClass(PsiManager.getInstance(project), subclassNames.get(i));
            PsiMethod concreteMethod = createPolymorphicMethodHeader();
            PsiCodeBlock concreteMethodBody = concreteMethod.getBody();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            PsiCodeBlock ifStatementBody = null;

            if (remainingIfStatementExpression != null) {
                PsiExpression enclosingIfStatementExpression = constructExpression(remainingIfStatementExpression);
                PsiExpression newEnclosingIfStatementExpression = elementFactory.createExpressionFromText(
                        enclosingIfStatementExpression.getText(),
                        enclosingIfStatementExpression.getContext()
                );
                List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(enclosingIfStatementExpression);
                List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
                List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
                List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
                modifySourceVariableInstructionsInSubclass(
                        oldVariableInstructions,
                        newVariableInstructions,
                        accessedFields,
                        assignedFields,
                        superAccessedFields,
                        superAssignedFields
                );
                modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
                modifySubclassMethodInvocations(oldMethodInvocations, newMethodInvocations, subClass, null);
                replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);

                PsiIfStatement enclosingIfStatement = (PsiIfStatement) elementFactory.createStatementFromText(
                        "if(true){} else{}",
                        null
                );
                enclosingIfStatement.getCondition().replace(newEnclosingIfStatementExpression);
                enclosingIfStatement = (PsiIfStatement) concreteMethodBody.add(enclosingIfStatement);
                if (!typeCheckElimination.getDefaultCaseStatements().isEmpty()) { // TODO: base class method is not created if base class is abstract
                    PsiCodeBlock elseStatementBody = ((PsiBlockStatement) enclosingIfStatement.getElseBranch()).getCodeBlock();
                    PsiMethodCallExpression superMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                            "super." + typeCheckElimination.getAbstractMethodName() + "()",
                            null
                    );
                    PsiExpressionList superMethodInvocationArgumentRewrite = superMethodInvocation.getArgumentList();
                    if (returnedVariable != null && !typeCheckElimination.returnedVariableDeclaredAndReturnedInBranches()) {
                        superMethodInvocationArgumentRewrite.add(elementFactory.createExpressionFromText(returnedVariable.getName(), null));
                    }
                    for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                        if (!abstractMethodParameter.equals(returnedVariable) && !abstractMethodParameter.equals(typeVariable)) {
                            superMethodInvocationArgumentRewrite.add(elementFactory.createExpressionFromText(abstractMethodParameter.getName(), null));
                        }
                    }
                    for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                        if (!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
                            superMethodInvocationArgumentRewrite.add(elementFactory.createExpressionFromText(fragment.getName(), null));
                        }
                    }
                    if (sourceTypeRequiredForExtraction()) {
                        String parameterName = sourceTypeDeclaration.getName();
                        parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
                        superMethodInvocationArgumentRewrite.add(elementFactory.createExpressionFromText(parameterName, null));
                    }
                    if (returnedVariable != null) {
                        PsiStatement superMethodInvocationReturnStatement = elementFactory.createStatementFromText(
                                "return " + superMethodInvocation.getText() + ";",
                                null
                        );
                        elseStatementBody.add(superMethodInvocationReturnStatement);
                    } else {
                        PsiStatement superMethodInvocationExpressionStatement = elementFactory.createStatementFromText(
                                superMethodInvocation.getText() + ";",
                                null
                        );
                        elseStatementBody.add(superMethodInvocationExpressionStatement);
                    }
                } else {
                    enclosingIfStatement.deleteChildRange(enclosingIfStatement.getElseElement(), enclosingIfStatement.getElseBranch());
                }
                ifStatementBody = ((PsiBlockStatement) enclosingIfStatement.getThenBranch()).getCodeBlock();
            }

            PsiVariable subclassCastInvoker = null;
            for (PsiStatement statement : statements) {
                PsiStatement newStatement = elementFactory.createStatementFromText(
                        statement.getText(),
                        statement.getContext()
                );
                boolean insert = true;
                if (statement instanceof PsiDeclarationStatement) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    PsiElement fragment = fragments[0];
                    if (fragment instanceof PsiVariable && ((PsiVariable) fragment).getInitializer() instanceof PsiTypeCastExpression) {
                        PsiVariable declaredVariable = (PsiVariable) fragment;
                        PsiTypeCastExpression castExpression = (PsiTypeCastExpression) declaredVariable.getInitializer();
                        if (castExpression.getCastType().getType().equals(PsiTypesUtil.getClassType(subClass))) {
                            if (castExpression.getOperand() instanceof PsiReferenceExpression) {
                                PsiReferenceExpression castSimpleName = (PsiReferenceExpression) castExpression.getOperand();
                                if (typeVariable != null && typeVariable.equals(castSimpleName.resolve())) {
                                    subclassCastInvoker = declaredVariable;
                                    insert = false;
                                }
                            } else if (castExpression.getOperand() instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression castMethodInvocation = (PsiMethodCallExpression) castExpression.getOperand();
                                if (typeMethodInvocation != null && typeMethodInvocation.textMatches(castMethodInvocation)) { // TODO: not sure if it works correctly
                                    subclassCastInvoker = declaredVariable;
                                    insert = false;
                                }
                            }
                        }
                    }
                } else {
                    StatementExtractor statementExtractor = new StatementExtractor();
                    List<PsiStatement> oldVariableDeclarations = statementExtractor.getVariableDeclarationStatements(statement);
                    List<PsiStatement> newVariableDeclarations = statementExtractor.getVariableDeclarationStatements(newStatement);
                    int j = 0;
                    for (PsiStatement oldVariableDeclaration : oldVariableDeclarations) {
                        PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) oldVariableDeclaration;
                        PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                        PsiElement fragment = fragments[0];
                        if (fragment instanceof PsiVariable && ((PsiVariable) fragment).getInitializer() instanceof PsiTypeCastExpression) {
                            PsiVariable declaredVariable = (PsiVariable) fragment;
                            PsiTypeCastExpression castExpression = (PsiTypeCastExpression) declaredVariable.getInitializer();
                            if (castExpression.getCastType().getType().equals(PsiTypesUtil.getClassType(subClass))) {
                                if (castExpression.getOperand() instanceof PsiReferenceExpression) {
                                    PsiReferenceExpression castSimpleName = (PsiReferenceExpression) castExpression.getOperand();
                                    if (typeVariable != null && typeVariable.equals(castSimpleName.resolve())) {
                                        subclassCastInvoker = declaredVariable;
                                        newVariableDeclarations.get(j).delete();
                                        break;
                                    }
                                } else if (castExpression.getOperand() instanceof PsiMethodCallExpression) {
                                    PsiMethodCallExpression castMethodInvocation = (PsiMethodCallExpression) castExpression.getOperand();
                                    if (typeMethodInvocation != null && typeMethodInvocation.textMatches(castMethodInvocation)) { // TODO: not sure if it works correctly
                                        subclassCastInvoker = declaredVariable;
                                        newVariableDeclarations.get(j).delete();
                                        break;
                                    }
                                }
                            }
                        }
                        j++;
                    }
                }
                List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
                List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
                List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
                List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
                modifySourceVariableInstructionsInSubclass(
                        oldVariableInstructions,
                        newVariableInstructions,
                        accessedFields,
                        assignedFields,
                        superAccessedFields,
                        superAssignedFields
                );
                modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
                modifySubclassMethodInvocations(oldMethodInvocations, newMethodInvocations, subClass, subclassCastInvoker);
                replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);
                replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement);
                List<PsiExpression> oldCastExpressions = expressionExtractor.getCastExpressions(statement);
                List<PsiExpression> newCastExpressions = expressionExtractor.getCastExpressions(newStatement);
                replaceCastExpressionWithThisExpression(oldCastExpressions, newCastExpressions, subClass);
                if (insert) {
                    if (ifStatementBody != null) {
                        ifStatementBody.add(newStatement);
                    } else {
                        concreteMethodBody.add(newStatement);
                    }
                }
            }
            if (returnedVariable != null && !typeCheckElimination.returnedVariableReturnedInBranches()) {
                concreteMethodBody.add(elementFactory.createStatementFromText("return " + returnedVariable.getName() + ";", null));
            }
            subClass.add(concreteMethod);
        }
    }

    private PsiMethod createPolymorphicMethodHeader() {
        String methodName = typeCheckElimination.getAbstractMethodName();
        PsiType returnType = PsiType.VOID;

        if (returnedVariable != null) {
            returnType = returnedVariable.getType();
        } else if (typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) { // TODO: looks really suspicious
            returnType = typeCheckElimination.getTypeCheckMethodReturnType();
        }

        PsiMethod createdMethod = elementFactory.createMethod(methodName, returnType);
        PsiUtil.setModifierProperty(createdMethod, PsiModifier.PUBLIC, true);

        PsiParameterList abstractMethodParameters = createdMethod.getParameterList();
        if (returnedVariable != null && !typeCheckElimination.returnedVariableDeclaredAndReturnedInBranches()) {
            abstractMethodParameters.add(elementFactory.createParameter(returnedVariable.getName(), returnedVariable.getType()));
        }
        for (PsiParameter accessedParameter : typeCheckElimination.getAccessedParameters()) {
            if (!accessedParameter.equals(returnedVariable) && !accessedParameter.equals(typeVariable)) {
                abstractMethodParameters.add(elementFactory.createParameter(accessedParameter.getName(), accessedParameter.getType()));
            }
        }
        for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
            if (!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
                abstractMethodParameters.add(elementFactory.createParameter(fragment.getName(), fragment.getType()));
            }
        }

        if (sourceTypeRequiredForExtraction()) {
            String parameterName = sourceTypeDeclaration.getName();
            parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
            PsiType parameterType = PsiTypesUtil.getClassType(sourceTypeDeclaration);
            abstractMethodParameters.add(elementFactory.createParameter(parameterName, parameterType));
        }

        PsiReferenceList abstractMethodThrownExceptionsRewrite = createdMethod.getThrowsList();
        for (PsiClassType typeBinding : thrownExceptions) {
            abstractMethodThrownExceptionsRewrite.add(elementFactory.createReferenceElementByType(typeBinding));
        }
        return createdMethod;
    }

    private void replaceCastExpressionWithThisExpression(List<PsiExpression> oldCastExpressions,
                                                         List<PsiExpression> newCastExpressions,
                                                         PsiClass subclassTypeDeclaration) {
        int j = 0;
        for (PsiExpression expression : oldCastExpressions) {
            PsiTypeCastExpression castExpression = (PsiTypeCastExpression) expression;
            if (castExpression.getCastType().getType().equals(PsiTypesUtil.getClassType(subclassTypeDeclaration))) {
                if (castExpression.getOperand() instanceof PsiReferenceExpression) {
                    PsiReferenceExpression castSimpleName = (PsiReferenceExpression) castExpression.getOperand();
                    if (typeVariable != null && typeVariable.equals(castSimpleName.resolve())) {
                        newCastExpressions.get(j).replace(elementFactory.createExpressionFromText("this", null));
                    }
                } else if (castExpression.getOperand() instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression castMethodInvocation = (PsiMethodCallExpression) castExpression.getOperand();
                    if (typeMethodInvocation != null && typeMethodInvocation.resolveMethod().equals(castMethodInvocation.resolveMethod())) { // TODO: not sure if it works correctly
                        newCastExpressions.get(j).replace(elementFactory.createExpressionFromText("this", null));
                    }
                }
            }
            j++;
        }
    }

    private void modifySubclassMethodInvocations(List<PsiExpression> oldMethodInvocations,
                                                 List<PsiExpression> newMethodInvocations,
                                                 PsiClass subclassTypeDeclaration,
                                                 PsiVariable subclassCastInvoker) {
        int j = 0;
        for (PsiExpression expression : newMethodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) expression;
                PsiMethodCallExpression oldMethodInvocation = (PsiMethodCallExpression) oldMethodInvocations.get(j);
                List<PsiExpression> newMethodInvocationExpressions = new ArrayList<>();
                List<PsiExpression> oldMethodInvocationExpressions = new ArrayList<>();
                PsiExpression newMethodInvocationExpression = newMethodInvocation.getMethodExpression().getQualifierExpression();
                if (newMethodInvocationExpression != null) {
                    newMethodInvocationExpressions.add(newMethodInvocationExpression);
                }
                PsiExpression oldMethodInvocationExpression = oldMethodInvocation.getMethodExpression().getQualifierExpression();
                if (oldMethodInvocationExpression != null) {
                    oldMethodInvocationExpressions.add(oldMethodInvocationExpression);
                }
                newMethodInvocationExpressions.addAll(Arrays.asList(newMethodInvocation.getArgumentList().getExpressions()));
                oldMethodInvocationExpressions.addAll(Arrays.asList(oldMethodInvocation.getArgumentList().getExpressions()));

                int k = 0;
                for (PsiExpression oldExpression : oldMethodInvocationExpressions) {
                    if (oldExpression instanceof PsiReferenceExpression) {
                        PsiReferenceExpression invoker = (PsiReferenceExpression) oldExpression;
                        if (typeVariable != null && typeVariable.equals(invoker.resolve())) {
                            newMethodInvocationExpressions.get(k).replace(elementFactory.createExpressionFromText("this", null));
                        }
                        if (subclassCastInvoker != null && subclassCastInvoker.equals(invoker.resolve())) {
                            newMethodInvocationExpressions.get(k).replace(elementFactory.createExpressionFromText("this", null));
                        }
                    } else if (oldExpression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression invoker = (PsiMethodCallExpression) oldExpression;
                        if (typeMethodInvocation != null && typeMethodInvocation.textMatches(invoker)) { // TODO: not sure if it works correctly
                            newMethodInvocationExpressions.get(k).replace(elementFactory.createExpressionFromText("this", null));
                        }
                    } else if (oldExpression instanceof PsiParenthesizedExpression) {
                        PsiParenthesizedExpression oldParenthesizedExpression = (PsiParenthesizedExpression) oldExpression;
                        if (oldParenthesizedExpression.getExpression() instanceof PsiTypeCastExpression) {
                            PsiTypeCastExpression oldCastExpression = (PsiTypeCastExpression) oldParenthesizedExpression.getExpression();
                            if (oldCastExpression.getCastType().getType().equals(PsiTypesUtil.getClassType(subclassTypeDeclaration))) {
                                if (oldCastExpression.getOperand() instanceof PsiReferenceExpression) {
                                    PsiReferenceExpression castSimpleName = (PsiReferenceExpression) oldCastExpression.getOperand();
                                    if (typeVariable != null && typeVariable.equals(castSimpleName.resolve())) {
                                        newMethodInvocationExpressions.get(k).replace(elementFactory.createExpressionFromText("this", null));
                                    }
                                } else if (oldCastExpression.getOperand() instanceof PsiMethodCallExpression) {
                                    PsiMethodCallExpression castMethodInvocation = (PsiMethodCallExpression) oldCastExpression.getOperand();
                                    if (typeMethodInvocation != null && typeMethodInvocation.textMatches(castMethodInvocation)) { // TODO: not sure if it works correctly
                                        newMethodInvocationExpressions.get(k).replace(elementFactory.createExpressionFromText("this", null));
                                    }
                                }
                            }
                        }
                    }
                    k++;
                }
            }
            j++;
        }
    }

    private Set<PsiType> getRequiredImportDeclarationsBasedOnSignature() {
        Set<PsiType> typeBindings = new LinkedHashSet<>();
        if (returnedVariable != null) {
            PsiType returnType = returnedVariable.getType();
            typeBindings.add(returnType);
        }

        Set<PsiParameter> parameters = typeCheckElimination.getAccessedParameters();
        for (PsiParameter parameter : parameters) {
            if (!parameter.equals(returnedVariable) && !parameter.equals(typeVariable)) {
                PsiType parameterType = parameter.getType();
                typeBindings.add(parameterType);
            }
        }

        Set<PsiVariable> accessedLocalVariables = typeCheckElimination.getAccessedLocalVariables();
        for (PsiVariable fragment : accessedLocalVariables) {
            if (!fragment.equals(returnedVariable) && !fragment.equals(typeVariable)) {
                PsiType variableType = fragment.getType();
                typeBindings.add(variableType);
            }
        }

        if (typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
                typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
                typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
            typeBindings.add(PsiTypesUtil.getClassType(sourceTypeDeclaration));
        }

        typeBindings.addAll(thrownExceptions);

        return typeBindings;
    }
}
