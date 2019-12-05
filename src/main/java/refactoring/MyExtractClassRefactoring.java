package refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.sun.deploy.util.JVMParameters;
import core.ast.ASTReader;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.util.MethodDeclarationUtility;
import core.ast.util.TypeVisitor;
import core.ast.util.math.AdjacencyList;
import core.ast.util.math.Edge;
import core.ast.util.math.Node;
import core.ast.util.math.TarjanAlgorithm;

import java.util.*;
import java.util.stream.Collectors;

import com.intellij.refactoring.extractclass.*;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;

@SuppressWarnings("FieldCanBeLocal")
public class MyExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private PsiFile sourceFile;
    private PsiClass sourceTypeDeclaration;
    private Set<PsiElement> javaElementsToOpenInEditor;
    private Set<PsiClass> requiredImportDeclarationsInExtractedClass;
    private Map<PsiMethod, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
    private Map<PsiMethod, Set<PsiParameter>> additionalParametersAddedToExtractedMethods;
    private Set<String> sourceMethodBindingsChangedWithPublicModifier;
    private Set<String> sourceFieldBindingsWithCreatedSetterMethod;
    private Set<String> sourceFieldBindingsWithCreatedGetterMethod;
    private Set<PsiField> fieldDeclarationsChangedWithPublicModifier;
    private Set<?> memberTypeDeclarationsChangedWithPublicModifier;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> oldMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> newMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, PsiMethod> oldToNewExtractedMethodDeclarationMap;
    private Set<PsiField> extractedFieldFragments;
    private Set<PsiMethod> extractedMethods;
    private Set<PsiMethod> delegateMethods;
    private String extractedTypeName;
    private boolean leaveDelegateForPublicMethods;
    //this map holds for each constructor the assignment statements that initialize final extracted fields
    private Map<PsiMethod, Map<PsiField, PsiAssignmentExpression>> constructorFinalFieldAssignmentMap;
    //this map hold the parameters that should be passed in each constructor of the extracted class
    private Map<PsiMethod, Set<PsiParameter>> extractedClassConstructorParameterMap;
    private Set<PsiField> extractedFieldsWithThisExpressionInTheirInitializer;
    private Set<PsiMethod> staticallyImportedMethods;

    // TODO comments
    private PsiElementFactory factory;
    private PsiFileFactory fileFactory;
    private Project project;
    private PsiManager psiManager;



    private void modifySourceMemberAccessesInTargetClass(PsiMethod sourceMethod, PsiMethod newMethodDeclaration) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        oldToNewExtractedMethodDeclarationMap.put(sourceMethod, newMethodDeclaration);
        List<PsiExpression> sourceMethodInvocations = new ArrayList<>();
        List<PsiExpression> sourceFieldInstructions = new ArrayList<>();
        List<PsiExpression> sourceAssignments = new ArrayList<>();
        Arrays.stream(sourceMethod.getBody().getStatements()).forEach(statement -> {
            sourceMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));
            sourceFieldInstructions.addAll(expressionExtractor.getVariableInstructions(statement));
            sourceAssignments.addAll(expressionExtractor.getAssignments(statement));
        });

        List<PsiExpression> newMethodInvocations = new ArrayList<>();
        List<PsiExpression> newFieldInstructions = new ArrayList<>();
        List<PsiExpression> newAssignments = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> {
            newMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));
            newFieldInstructions.addAll(expressionExtractor.getVariableInstructions(statement));
            newAssignments.addAll(expressionExtractor.getAssignments(statement));
        });

        PsiVariable sourceClassParameter = null;
        boolean sourceClassParameterShouldBeFinal = false;
        Map<PlainVariable, PsiField> fieldParameterMap = new LinkedHashMap<>();
        Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
        String sourceTypeName = sourceTypeDeclaration.getName();
        String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1, sourceTypeName.length());

        int i = 0;
        for (PsiExpression expression : sourceAssignments) {
            PsiAssignmentExpression oldAssignment = (PsiAssignmentExpression) expression;
            PsiAssignmentExpression newAssignment = (PsiAssignmentExpression) newAssignments.get(i);
            PsiExpression oldLeftHandSide = oldAssignment.getLExpression();
            PsiExpression newLeftHandSide = newAssignment.getLExpression();

            PsiVariable oldAssignedVariable = null;
            PsiVariable newAssignedVariable = null;
            if (oldLeftHandSide instanceof PsiReferenceExpression) {
                oldAssignedVariable = (PsiVariable) ((PsiReferenceExpression) oldLeftHandSide).resolve();
                newAssignedVariable = (PsiVariable) ((PsiReferenceExpression) newLeftHandSide).resolve();
            } //TODO does it works with Field access?

            PsiExpression oldRightHandSide = oldAssignment.getLExpression();
            PsiExpression newRightHandSide = newAssignment.getRExpression();
            if (oldAssignedVariable != null) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding) binding;
                    if (variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if (declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(oldAssignment))
                                    sourceClassParameterShouldBeFinal = true;
                                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if (setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                } else {
                                    if (!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                if (!newAssignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
                                    IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                    MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                    if (getterMethodBinding != null) {
                                        targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                    } else {
                                        if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                            createGetterMethodInSourceClass(variableBinding);
                                            sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                        }
                                        String originalFieldName = variableBinding.getName();
                                        String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                        String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                        getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                        targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                    }
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                    InfixExpression infixExpression = ast.newInfixExpression();
                                    targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                    targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, newAssignment.getRightHandSide(), null);
                                    if (newAssignment.getOperator().equals(Assignment.Operator.PLUS_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.MINUS_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.TIMES_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.DIVIDE_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.DIVIDE, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.REMAINDER_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.REMAINDER, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.BIT_AND_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.AND, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.BIT_OR_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.OR, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.XOR, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.LEFT_SHIFT, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_SIGNED, null);
                                    } else if (newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, null);
                                    }
                                    setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                } else {
                                    setterMethodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
                                }
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newAssignment, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in left hand side, then replace all accessed fields in left hand side
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldLeftHandSide);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newLeftHandSide);
                for (Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName) expression2;
                    SimpleName newAccessedVariable = (SimpleName) newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if (rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding) rightHandBinding;
                        if (accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if (declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if (isParentAnonymousClassDeclaration(oldAssignment))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                }
                            }
                        }
                    }
                    j++;
                }
            }
            int j = 0;
            List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldRightHandSide);
            List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newRightHandSide);
            for (Expression expression2 : oldAccessedVariables) {
                SimpleName oldAccessedVariable = (SimpleName) expression2;
                SimpleName newAccessedVariable = (SimpleName) newAccessedVariables.get(j);
                IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                if (rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding accessedVariableBinding = (IVariableBinding) rightHandBinding;
                    if (accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if (declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                    if (isParentAnonymousClassDeclaration(oldAssignment))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            newAccessedVariable,
                                            accessedVariableBinding);
                                } else {
                                    if (isParentAnonymousClassDeclaration(oldAccessedVariable))
                                        fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast, fieldParameterMap,
                                            newAccessedVariable, accessedVariableBinding);
                                }
                            }
                        }
                    }
                }
                j++;
            }
            i++;
        }

        List<Expression> sourcePostfixExpressions = expressionExtractor.getPostfixExpressions(sourceMethod.getBody());
        List<Expression> newPostfixExpressions = expressionExtractor.getPostfixExpressions(newMethodDeclaration.getBody());
        i = 0;
        for (Expression expression : sourcePostfixExpressions) {
            PostfixExpression oldPostfixExpression = (PostfixExpression) expression;
            PostfixExpression newPostfixExpression = (PostfixExpression) newPostfixExpressions.get(i);
            Expression oldOperand = oldPostfixExpression.getOperand();
            Expression newOperand = newPostfixExpression.getOperand();
            SimpleName oldAssignedVariable = null;
            SimpleName newAssignedVariable = null;
            if (oldOperand instanceof SimpleName) {
                oldAssignedVariable = (SimpleName) oldOperand;
                newAssignedVariable = (SimpleName) newOperand;
            } else if (oldOperand instanceof FieldAccess) {
                FieldAccess oldFieldAccess = (FieldAccess) oldOperand;
                oldAssignedVariable = oldFieldAccess.getName();
                FieldAccess newFieldAccess = (FieldAccess) newOperand;
                newAssignedVariable = newFieldAccess.getName();
            }
            if (oldAssignedVariable != null) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding) binding;
                    if (variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if (declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(oldPostfixExpression))
                                    sourceClassParameterShouldBeFinal = true;
                                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if (setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                } else {
                                    if (!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                if (getterMethodBinding != null) {
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                }
                                targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                InfixExpression infixExpression = ast.newInfixExpression();
                                targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
                                if (newPostfixExpression.getOperator().equals(PostfixExpression.Operator.INCREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                } else if (newPostfixExpression.getOperator().equals(PostfixExpression.Operator.DECREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                }
                                setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newPostfixExpression, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName) expression2;
                    SimpleName newAccessedVariable = (SimpleName) newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if (rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding) rightHandBinding;
                        if (accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if (declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if (isParentAnonymousClassDeclaration(oldPostfixExpression))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                }
                            }
                        }
                    }
                    j++;
                }
            }
            i++;
        }

        List<Expression> sourcePrefixExpressions = expressionExtractor.getPrefixExpressions(sourceMethod.getBody());
        List<Expression> newPrefixExpressions = expressionExtractor.getPrefixExpressions(newMethodDeclaration.getBody());
        i = 0;
        for (Expression expression : sourcePrefixExpressions) {
            PrefixExpression oldPrefixExpression = (PrefixExpression) expression;
            PrefixExpression newPrefixExpression = (PrefixExpression) newPrefixExpressions.get(i);
            Expression oldOperand = oldPrefixExpression.getOperand();
            Expression newOperand = newPrefixExpression.getOperand();
            Operator oldOperator = oldPrefixExpression.getOperator();
            Operator newOperator = newPrefixExpression.getOperator();
            SimpleName oldAssignedVariable = null;
            SimpleName newAssignedVariable = null;
            if (oldOperand instanceof SimpleName) {
                oldAssignedVariable = (SimpleName) oldOperand;
                newAssignedVariable = (SimpleName) newOperand;
            } else if (oldOperand instanceof FieldAccess) {
                FieldAccess oldFieldAccess = (FieldAccess) oldOperand;
                oldAssignedVariable = oldFieldAccess.getName();
                FieldAccess newFieldAccess = (FieldAccess) newOperand;
                newAssignedVariable = newFieldAccess.getName();
            }
            if (oldAssignedVariable != null && (oldOperator.equals(PrefixExpression.Operator.INCREMENT) ||
                    oldOperator.equals(PrefixExpression.Operator.DECREMENT))) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding) binding;
                    if (variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if (declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(oldPrefixExpression))
                                    sourceClassParameterShouldBeFinal = true;
                                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if (setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                } else {
                                    if (!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                if (getterMethodBinding != null) {
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                }
                                targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                InfixExpression infixExpression = ast.newInfixExpression();
                                targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
                                if (newOperator.equals(PrefixExpression.Operator.INCREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                } else if (newOperator.equals(PrefixExpression.Operator.DECREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                }
                                setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newPrefixExpression, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName) expression2;
                    SimpleName newAccessedVariable = (SimpleName) newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if (rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding) rightHandBinding;
                        if (accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if (declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if (isParentAnonymousClassDeclaration(oldPrefixExpression))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                }
                            }
                        }
                    }
                    j++;
                }
            }
            i++;
        }

        i = 0;
        for (Expression expression : sourceFieldInstructions) {
            SimpleName simpleName = (SimpleName) expression;
            IBinding binding = simpleName.resolveBinding();
            if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                IVariableBinding variableBinding = (IVariableBinding) binding;
                if (variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                    if (declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                        if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                            if (!isAssignmentChild(expression)) {
                                SimpleName expressionName = (SimpleName) newFieldInstructions.get(i);
                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                    if (isParentAnonymousClassDeclaration(simpleName))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod,
                                            newMethodDeclaration,
                                            targetRewriter, ast,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            expressionName,
                                            variableBinding);
                                } else {
                                    if (isParentAnonymousClassDeclaration(simpleName))
                                        fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast, fieldParameterMap,
                                            expressionName, variableBinding);
                                }
                            }
                        }
                    } else {
                        Type superclassType = sourceTypeDeclaration.getSuperclassType();
                        ITypeBinding superclassTypeBinding = null;
                        if (superclassType != null)
                            superclassTypeBinding = superclassType.resolveBinding();
                        while (superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(variableBinding.getDeclaringClass())) {
                            superclassTypeBinding = superclassTypeBinding.getSuperclass();
                        }
                        if (superclassTypeBinding != null) {
                            IVariableBinding[] superclassFieldBindings = superclassTypeBinding.getDeclaredFields();
                            for (IVariableBinding superclassFieldBinding : superclassFieldBindings) {
                                if (superclassFieldBinding.isEqualTo(variableBinding)) {
                                    if (!isAssignmentChild(expression)) {
                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                        Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                        SimpleName expressionName = (SimpleName) newFieldInstructions.get(i);
                                        if (isParentAnonymousClassDeclaration(simpleName))
                                            fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                        if (!containsVariable(variableBinding, additionalArgumentsAddedToMovedMethod)) {
                                            SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, variableBinding, targetRewriter);
                                            addVariable(variableBinding, additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(fieldParameter);
                                            fieldParameterMap.put(new PlainVariable(variableBinding), fieldParameter);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            i++;
        }

        int j = 0;
        for (Expression expression : sourceMethodInvocations) {
            if (expression instanceof MethodInvocation) {
                MethodInvocation methodInvocation = (MethodInvocation) expression;
                if (methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
                    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                    if (methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
                        MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
                        for (MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
                            if (sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                                if (!methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding()) &&
                                        !sourceMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                                    SimpleName fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    int modifiers = sourceMethodDeclaration.getModifiers();
                                    MethodInvocation newMethodInvocation = (MethodInvocation) newMethodInvocations.get(j);
                                    if ((modifiers & Modifier.STATIC) != 0) {
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier()), null);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                                        }
                                    } else if (fieldName != null) {
                                        IVariableBinding fieldBinding = (IVariableBinding) fieldName.resolveBinding();
                                        if (!variableBindingCorrespondsToExtractedField(fieldBinding)) {
                                            if ((fieldBinding.getModifiers() & Modifier.STATIC) != 0) {
                                                SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
                                                QualifiedName qualifiedName = ast.newQualifiedName(qualifier, ast.newSimpleName(fieldName.getIdentifier()));
                                                targetRewriter.replace(newMethodInvocation, qualifiedName, null);
                                                setPublicModifierToSourceField(fieldBinding);
                                            } else {
                                                String parameterNameString = createNameForParameterizedFieldAccess(fieldName.getIdentifier());
                                                targetRewriter.replace(newMethodInvocation, ast.newSimpleName(parameterNameString), null);
                                                //targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
                                                if (isParentAnonymousClassDeclaration(methodInvocation))
                                                    fieldParameterFinalMap.put(new PlainVariable(fieldBinding), true);
                                                if (!containsVariable(fieldBinding, additionalArgumentsAddedToMovedMethod)) {
                                                    SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, fieldBinding, targetRewriter);
                                                    addVariable(fieldBinding, additionalArgumentsAddedToMovedMethod);
                                                    additionalParametersAddedToMovedMethod.add(fieldParameter);
                                                    fieldParameterMap.put(new PlainVariable(fieldBinding), fieldParameter);
                                                }
                                            }
                                        } else {
                                            targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
                                        }
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                                        }
                                    }
                                } else {
                                    if (!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                                        methodInvocations.add(methodInvocation);
                                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                                    } else {
                                        Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                                        methodInvocations.add(methodInvocation);
                                    }
                                    if (!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                                        methodInvocations.add((MethodInvocation) newMethodInvocations.get(j));
                                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                                    } else {
                                        Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                                        methodInvocations.add((MethodInvocation) newMethodInvocations.get(j));
                                    }
                                }
                            }
                        }
                    } else {
                        Type superclassType = sourceTypeDeclaration.getSuperclassType();
                        ITypeBinding superclassTypeBinding = null;
                        if (superclassType != null)
                            superclassTypeBinding = superclassType.resolveBinding();
                        while (superclassTypeBinding != null && !methodBinding.getDeclaringClass().isEqualTo(superclassTypeBinding)) {
                            superclassTypeBinding = superclassTypeBinding.getSuperclass();
                        }
                        if (superclassTypeBinding != null) {
                            IMethodBinding[] superclassMethodBindings = superclassTypeBinding.getDeclaredMethods();
                            for (IMethodBinding superclassMethodBinding : superclassMethodBindings) {
                                if (superclassMethodBinding.isEqualTo(methodBinding)) {
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    MethodInvocation newMethodInvocation = (MethodInvocation) newMethodInvocations.get(j);
                                    if ((superclassMethodBinding.getModifiers() & Modifier.STATIC) != 0) {
                                        SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifier, null);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodBinding.getKey())) {
                                            TypeDeclaration superclassTypeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(superclassMethodBinding, sourceTypeDeclaration);
                                            if (superclassTypeDeclaration != null) {
                                                setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), superclassTypeDeclaration);
                                            }
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodBinding.getKey());
                                        }
                                    }
                                }
                            }
                        } else {
                            //check if it is a statically imported method
                            List<ImportDeclaration> sourceImportDeclarations = sourceCompilationUnit.imports();
                            for (ImportDeclaration importDeclaration : sourceImportDeclarations) {
                                if (importDeclaration.isStatic()) {
                                    IBinding binding = importDeclaration.resolveBinding();
                                    //A single-static-import declaration imports all accessible static members with a given simple name from a type.
                                    //binding.isEqualTo(methodBinding) will not work when the static import actually imports multiple overloaded methods
                                    if (binding != null && binding.getKind() == IBinding.METHOD) {
                                        IMethodBinding importedMethodBinding = (IMethodBinding) binding;
                                        if (importedMethodBinding.getName().equals(methodBinding.getName()) &&
                                                importedMethodBinding.getDeclaringClass().getQualifiedName().equals(methodBinding.getDeclaringClass().getQualifiedName())) {
                                            this.staticallyImportedMethods.add(importedMethodBinding);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding())) {
                    if (!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                        methodInvocations.add(methodInvocation);
                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                    } else {
                        Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                        methodInvocations.add(methodInvocation);
                    }
                    if (!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                        methodInvocations.add((MethodInvocation) newMethodInvocations.get(j));
                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                    } else {
                        Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                        methodInvocations.add((MethodInvocation) newMethodInvocations.get(j));
                    }
                    if (methodInvocation.getExpression() != null && methodInvocation.getExpression().resolveTypeBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                        }
                    }
                }
            }
            j++;
        }
        //replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments
        int k = 0;
        for (Expression invocation : newMethodInvocations) {
            if (invocation instanceof MethodInvocation) {
                MethodInvocation methodInvocation = (MethodInvocation) invocation;
                List<Expression> arguments = methodInvocation.arguments();
                for (Expression argument : arguments) {
                    if (argument instanceof ThisExpression) {
                        if (isParentAnonymousClassDeclaration(sourceMethodInvocations.get(k)))
                            sourceClassParameterShouldBeFinal = true;
                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                        Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                        }
                        ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                        argumentRewrite.replace(argument, parameterName, null);
                    }
                }
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments
        List<Expression> sourceClassInstanceCreations = expressionExtractor.getClassInstanceCreations(sourceMethod.getBody());
        List<Expression> newClassInstanceCreations = expressionExtractor.getClassInstanceCreations(newMethodDeclaration.getBody());
        k = 0;
        for (Expression creation : newClassInstanceCreations) {
            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) creation;
            List<Expression> arguments = classInstanceCreation.arguments();
            for (Expression argument : arguments) {
                if (argument instanceof ThisExpression) {
                    if (isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
                        sourceClassParameterShouldBeFinal = true;
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
                    argumentRewrite.replace(argument, parameterName, null);
                }
            }
            ClassInstanceCreation oldClassInstanceCreation = (ClassInstanceCreation) sourceClassInstanceCreations.get(k);
            ITypeBinding classInstanceCreationTypeBinding = oldClassInstanceCreation.resolveTypeBinding();
            if (classInstanceCreationTypeBinding.isNested() && oldClassInstanceCreation.getAnonymousClassDeclaration() == null &&
                    sourceTypeDeclaration.resolveBinding().isEqualTo(classInstanceCreationTypeBinding.getDeclaringClass())) {
                if (isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                if ((classInstanceCreationTypeBinding.getModifiers() & Modifier.STATIC) != 0) {
                    Type qualifierType = ast.newSimpleType(ast.newSimpleName(sourceTypeDeclaration.resolveBinding().getName()));
                    QualifiedType qualifiedType = ast.newQualifiedType(qualifierType, ast.newSimpleName(classInstanceCreationTypeBinding.getName()));
                    targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, qualifiedType, null);
                    requiredImportDeclarationsInExtractedClass.add(classInstanceCreationTypeBinding);
                    setPublicModifierToSourceMemberType(classInstanceCreationTypeBinding);
                } else {
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    targetRewriter.set(classInstanceCreation, ClassInstanceCreation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                    Type oldClassInstanceCreationType = oldClassInstanceCreation.getType();
                    SimpleName simpleNameType = null;
                    if (oldClassInstanceCreationType instanceof QualifiedType) {
                        QualifiedType qualifiedType = (QualifiedType) oldClassInstanceCreationType;
                        simpleNameType = qualifiedType.getName();
                    } else if (oldClassInstanceCreationType instanceof SimpleType) {
                        SimpleType simpleType = (SimpleType) oldClassInstanceCreationType;
                        if (simpleType.getName() instanceof QualifiedName) {
                            QualifiedName qualifiedName = (QualifiedName) simpleType.getName();
                            simpleNameType = qualifiedName.getName();
                        }
                    }
                    if (simpleNameType != null) {
                        targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY,
                                ast.newSimpleType(ast.newSimpleName(simpleNameType.getIdentifier())), null);
                    }
                }
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers
        StatementExtractor statementExtractor = new StatementExtractor();
        List<VariableDeclarationFragment> sourceVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
        List<VariableDeclarationFragment> newVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
        List<Statement> sourceVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(sourceMethod.getBody());
        for (Statement statement : sourceVariableDeclarationStatements) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
            List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
            sourceVariableDeclarationFragments.addAll(fragments);
        }
        List<Statement> newVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody());
        for (Statement statement : newVariableDeclarationStatements) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
            List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
            newVariableDeclarationFragments.addAll(fragments);
        }
        List<Expression> sourceVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(sourceMethod.getBody());
        for (Expression expression : sourceVariableDeclarationExpressions) {
            VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;
            List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
            sourceVariableDeclarationFragments.addAll(fragments);
        }
        List<Expression> newVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(newMethodDeclaration.getBody());
        for (Expression expression : newVariableDeclarationExpressions) {
            VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;
            List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
            newVariableDeclarationFragments.addAll(fragments);
        }
        k = 0;
        for (VariableDeclarationFragment fragment : newVariableDeclarationFragments) {
            Expression initializer = fragment.getInitializer();
            if (initializer instanceof ThisExpression) {
                if (isParentAnonymousClassDeclaration(sourceVariableDeclarationFragments.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, parameterName, null);
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInReturnStatementExpressions
        List<Statement> sourceReturnStatements = statementExtractor.getReturnStatements(sourceMethod.getBody());
        List<Statement> newReturnStatements = statementExtractor.getReturnStatements(newMethodDeclaration.getBody());
        k = 0;
        for (Statement statement : newReturnStatements) {
            ReturnStatement newReturnStatement = (ReturnStatement) statement;
            if (newReturnStatement.getExpression() instanceof ThisExpression) {
                if (isParentAnonymousClassDeclaration(sourceReturnStatements.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                targetRewriter.set(newReturnStatement, ReturnStatement.EXPRESSION_PROPERTY, parameterName, null);
            }
            k++;
        }
        if (sourceClassParameter != null && sourceClassParameterShouldBeFinal) {
            ListRewrite modifiersRewrite = targetRewriter.getListRewrite(sourceClassParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
            modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
        }
        for (PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
            if (fieldParameterFinalMap.get(fieldName) == true) {
                SingleVariableDeclaration fieldParameter = fieldParameterMap.get(fieldName);
                if (fieldParameter != null) {
                    ListRewrite modifiersRewrite = targetRewriter.getListRewrite(fieldParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                    modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                }
            }
        }
    }

    private void handleAccessedFieldNotHavingSetterMethod(MethodDeclaration sourceMethod,
                                                          MethodDeclaration newMethodDeclaration, ASTRewrite targetRewriter, AST ast,
                                                          Map<PlainVariable, SingleVariableDeclaration> fieldParameterMap, SimpleName newAccessedVariable, IVariableBinding accessedVariableBinding) {
        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
        if(!containsVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod)) {
            SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, accessedVariableBinding, targetRewriter);
            addVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod);
            additionalParametersAddedToMovedMethod.add(fieldParameter);
            fieldParameterMap.put(new PlainVariable(accessedVariableBinding), fieldParameter);
        }
        if(newAccessedVariable.getParent() instanceof FieldAccess) {
            FieldAccess fieldAccess = (FieldAccess)newAccessedVariable.getParent();
            if(fieldAccess.getExpression() instanceof ThisExpression) {
                String parameterName = createNameForParameterizedFieldAccess(fieldAccess.getName().getIdentifier());
                targetRewriter.replace(newAccessedVariable.getParent(), ast.newSimpleName(parameterName), null);
            }
        }
        else if(newAccessedVariable.getParent() instanceof QualifiedName) {

        }
        else {
            String parameterName = createNameForParameterizedFieldAccess(accessedVariableBinding.getName());
            targetRewriter.replace(newAccessedVariable, ast.newSimpleName(parameterName), null);
        }
    }
}