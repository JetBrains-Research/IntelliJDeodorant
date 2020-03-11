package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;
import org.jetbrains.research.intellijdeodorant.core.ast.util.StatementExtractor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckElimination;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class TypeCheckCodeFragmentAnalyzer {
    private final TypeCheckElimination typeCheckElimination;
    private final SmartPsiElementPointer<PsiElement> typeDeclaration;
    private final SmartPsiElementPointer<PsiElement> typeCheckMethod;
    private final PsiField[] fields;
    private final PsiMethod[] methods;
    private final Map<PsiVariable, Integer> typeVariableCounterMap;
    private final Map<PsiMethodCallExpression, Integer> typeMethodInvocationCounterMap;
    private final Map<PsiExpression, IfStatementExpressionAnalyzer> complexExpressionMap;

    public TypeCheckCodeFragmentAnalyzer(TypeCheckElimination typeCheckElimination,
                                         PsiClass typeDeclaration,
                                         PsiMethod typeCheckMethod) {
        this.typeCheckElimination = typeCheckElimination;
        this.typeDeclaration = toPointer(typeDeclaration);
        this.typeCheckMethod = toPointer(typeCheckMethod);
        this.fields = typeDeclaration.getFields();
        this.methods = typeDeclaration.getMethods();
        this.typeVariableCounterMap = new LinkedHashMap<>();
        this.typeMethodInvocationCounterMap = new LinkedHashMap<>();
        this.complexExpressionMap = new LinkedHashMap<>();
        typeCheckElimination.setTypeCheckClass(typeDeclaration);
        typeCheckElimination.setTypeCheckMethod(typeCheckMethod);
        processTypeCheckCodeFragment();
    }

    private void processTypeCheckCodeFragment() {
        if (typeCheckElimination.getTypeCheckCodeFragment() instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) typeCheckElimination.getTypeCheckCodeFragment();
            PsiExpression switchStatementExpression = switchStatement.getExpression();
            PsiElement switchStatementExpressionName = extractOperand(switchStatementExpression);
            if (switchStatementExpressionName != null) {
                if (switchStatementExpressionName instanceof PsiReferenceExpression) {
                    PsiReferenceExpression switchStatementExpressionSimpleName = (PsiReferenceExpression) switchStatementExpressionName;
                    PsiElement switchStatementExpressionNameBinding = switchStatementExpressionSimpleName.resolve();
                    if (switchStatementExpressionNameBinding instanceof PsiVariable) {
                        PsiVariable switchStatementExpressionNameVariableBinding = (PsiVariable) switchStatementExpressionNameBinding;
                        if (switchStatementExpressionNameVariableBinding instanceof PsiField) {
                            for (PsiField field : fields) {
                                if (field.equals(switchStatementExpressionNameVariableBinding)) {
                                    typeCheckElimination.setTypeField(field);
                                    for (PsiMethod method : methods) {
                                        PsiElement resolvedField = MethodDeclarationUtility.isSetter(method);
                                        if (field.equals(resolvedField)) {
                                            typeCheckElimination.setTypeFieldSetterMethod(method);
                                        }
                                        PsiReferenceExpression resolvedFieldInstruction = (PsiReferenceExpression) MethodDeclarationUtility.isGetter(method);
                                        if (resolvedFieldInstruction != null && field.equals(resolvedFieldInstruction.resolve())) {
                                            typeCheckElimination.setTypeFieldGetterMethod(method);
                                        }
                                    }
                                    break;
                                }

                            }
                        } else if (switchStatementExpressionNameVariableBinding instanceof PsiParameter) {
                            PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                            for (PsiParameter parameter : parameters) {
                                if (parameter.equals(switchStatementExpressionNameVariableBinding)) {
                                    typeCheckElimination.setTypeLocalVariable(parameter);
                                    break;
                                }
                            }
                        } else {
                            StatementExtractor statementExtractor = new StatementExtractor();
                            List<PsiVariable> declaredVariables = new ArrayList<>();
                            List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(getTypeCheckMethod().getBody());
                            for (PsiStatement statement : variableDeclarationStatements) {
                                PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                                PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                                List<PsiVariable> fragmentsAsVariables = Arrays.stream(fragments)
                                        .map(element -> (PsiVariable) element)
                                        .collect(Collectors.toList());
                                declaredVariables.addAll(fragmentsAsVariables);
                            }
                            for (PsiVariable declaredVariable : declaredVariables) {
                                if (declaredVariable.equals(switchStatementExpressionNameVariableBinding)) {
                                    typeCheckElimination.setTypeLocalVariable(declaredVariable);
                                    break;
                                }
                            }
                            List<PsiStatement> enhancedForStatements = new ArrayList<>();
                            for (PsiStatement statementInBlock : getTypeCheckMethod().getBody().getStatements()) {
                                enhancedForStatements.addAll(statementExtractor.getEnhancedForStatements(statementInBlock));
                            }
                            for (PsiStatement eFStatement : enhancedForStatements) {
                                PsiForeachStatement enhancedForStatement = (PsiForeachStatement) eFStatement;
                                PsiParameter formalParameter = enhancedForStatement.getIterationParameter();
                                if (formalParameter.equals(switchStatementExpressionNameVariableBinding)) {
                                    typeCheckElimination.setTypeLocalVariable(formalParameter);
                                    break;
                                }
                            }
                        }
                    }
                } else if (switchStatementExpressionName instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression switchStatementExpressionMethodInvocation = (PsiMethodCallExpression) switchStatementExpressionName;
                    PsiExpression invoker = switchStatementExpressionMethodInvocation.getMethodExpression().getQualifierExpression();
                    PsiMethod switchStatementExpressionMethodBinding = (PsiMethod) switchStatementExpressionMethodInvocation.getMethodExpression().resolve();
                    PsiClass psiClass = switchStatementExpressionMethodBinding == null ? null : switchStatementExpressionMethodBinding.getContainingClass();
                    if (psiClass != null && !psiClass.equals(getTypeDeclaration()) &&
                            invoker != null && !(invoker instanceof PsiThisExpression)) {
                        typeCheckElimination.setTypeMethodInvocation(switchStatementExpressionMethodInvocation);
                    }
                }
            }
        }

        Set<PsiExpression> typeCheckExpressions = typeCheckElimination.getTypeCheckExpressions();
        for (PsiExpression typeCheckExpression : typeCheckExpressions) {
            if (typeCheckExpression.getParent().getParent() instanceof PsiSwitchLabelStatement) {
                if (typeCheckExpression instanceof PsiReferenceExpression) {
                    PsiReferenceExpression referenceExpression = ((PsiReferenceExpression) typeCheckExpression);
                    if (referenceExpression.resolve() instanceof PsiVariable) {
                        PsiVariable variable = (PsiVariable) referenceExpression.resolve();
                        if (variable instanceof PsiField && variable.hasModifierProperty(PsiModifier.STATIC)) {
                            ArrayList<PsiField> staticTypes = new ArrayList<>();
                            staticTypes.add((PsiField) variable);
                            typeCheckElimination.addStaticType(typeCheckExpression, staticTypes);
                        }
                    }
                }
            } else if (typeCheckExpression instanceof PsiInstanceOfExpression) {
                PsiInstanceOfExpression instanceofExpression = (PsiInstanceOfExpression) typeCheckExpression;
                IfStatementExpressionAnalyzer analyzer = new IfStatementExpressionAnalyzer(instanceofExpression);
                PsiElement operandExpression = extractOperand(instanceofExpression.getOperand());
                if (operandExpression != null) {
                    if (operandExpression instanceof PsiReferenceExpression) {
                        PsiReferenceExpression operandName = (PsiReferenceExpression) operandExpression;
                        PsiVariable resolvedReference = (PsiVariable) operandName.resolve();
                        typeVariableCounterMap.put(
                                resolvedReference,
                                typeVariableCounterMap.getOrDefault(resolvedReference, 0) + 1
                        );
                        analyzer.putTypeVariableExpression(operandName, instanceofExpression);
                        analyzer.putTypeVariableSubclass(operandName, instanceofExpression.getCheckType().getType());
                    } else if (operandExpression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression operandMethodInvocation = (PsiMethodCallExpression) operandExpression;
                        PsiMethodCallExpression keyMethodInvocation = containsTypeMethodInvocationKey(operandMethodInvocation);
                        if (keyMethodInvocation != null) {
                            typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation) + 1);
                        } else {
                            typeMethodInvocationCounterMap.put(operandMethodInvocation, 1);
                        }
                        analyzer.putTypeMethodInvocationExpression(operandMethodInvocation, instanceofExpression);
                        analyzer.putTypeMethodInvocationSubclass(operandMethodInvocation, instanceofExpression.getCheckType().getType());
                    }
                    complexExpressionMap.put(typeCheckExpression, analyzer);
                }
            } else if (typeCheckExpression instanceof PsiBinaryExpression) {
                PsiBinaryExpression infixExpression = (PsiBinaryExpression) typeCheckExpression;
                IfStatementExpressionAnalyzer analyzer = new IfStatementExpressionAnalyzer(infixExpression);
                for (PsiBinaryExpression leafInfixExpression : analyzer.getInfixExpressionsWithEqualsOperator()) {
                    PsiExpression leftOperand = leafInfixExpression.getLOperand();
                    PsiExpression rightOperand = leafInfixExpression.getROperand();
                    PsiExpression leftOperandExpression = extractOperand(leftOperand);
                    PsiExpression rightOperandExpression = extractOperand(rightOperand);
                    PsiReferenceExpression typeVariableName = null;
                    PsiReferenceExpression staticFieldName = null;
                    PsiMethodCallExpression typeMethodInvocation = null;
                    PsiType subclassType = null;
                    if (leftOperandExpression != null && rightOperandExpression != null) {
                        if (leftOperandExpression instanceof PsiReferenceExpression) {
                            PsiReferenceExpression leftOperandName = (PsiReferenceExpression) leftOperandExpression;
                            PsiElement leftOperandNameBinding = leftOperandName.resolve();
                            if (leftOperandNameBinding instanceof PsiVariable) {
                                PsiVariable leftOperandNameVariableBinding = (PsiVariable) leftOperandNameBinding;
                                if (leftOperandNameVariableBinding instanceof PsiField && leftOperandNameVariableBinding.hasModifierProperty(PsiModifier.STATIC))
                                    staticFieldName = leftOperandName;
                            }
                        }
                        if (rightOperandExpression instanceof PsiReferenceExpression) {
                            PsiReferenceExpression rightOperandName = (PsiReferenceExpression) rightOperandExpression;
                            PsiElement rightOperandNameBinding = rightOperandName.resolve();
                            if (rightOperandNameBinding instanceof PsiVariable) {
                                PsiVariable rightOperandNameVariableBinding = (PsiVariable) rightOperandNameBinding;
                                if (rightOperandNameVariableBinding instanceof PsiField && rightOperandNameVariableBinding.hasModifierProperty(PsiModifier.STATIC))
                                    staticFieldName = rightOperandName;
                            }
                        }
                        if (staticFieldName != null && staticFieldName.equals(leftOperandExpression)) {
                            if (rightOperandExpression instanceof PsiReferenceExpression) {
                                typeVariableName = (PsiReferenceExpression) rightOperandExpression;
                            } else if (rightOperandExpression instanceof PsiMethodCallExpression) {
                                typeMethodInvocation = (PsiMethodCallExpression) rightOperandExpression;
                            }
                        } else if (staticFieldName != null && staticFieldName.equals(rightOperandExpression)) {
                            if (leftOperandExpression instanceof PsiReferenceExpression) {
                                typeVariableName = (PsiReferenceExpression) leftOperandExpression;
                            } else if (leftOperandExpression instanceof PsiMethodCallExpression) {
                                typeMethodInvocation = (PsiMethodCallExpression) leftOperandExpression;
                            }
                        }
                    } else if (leftOperandExpression != null) {
                        if (rightOperand instanceof PsiClassObjectAccessExpression) {
                            PsiClassObjectAccessExpression typeLiteral = (PsiClassObjectAccessExpression) rightOperand;
                            subclassType = typeLiteral.getOperand().getType();
                            if (leftOperandExpression instanceof PsiReferenceExpression) {
                                typeVariableName = (PsiReferenceExpression) leftOperandExpression;
                            } else if (leftOperandExpression instanceof PsiMethodCallExpression) {
                                typeMethodInvocation = (PsiMethodCallExpression) leftOperandExpression;
                            }
                        }
                    } else if (rightOperandExpression != null) {
                        if (leftOperand instanceof PsiClassObjectAccessExpression) {
                            PsiClassObjectAccessExpression typeLiteral = (PsiClassObjectAccessExpression) leftOperand;
                            subclassType = typeLiteral.getOperand().getType();
                            if (rightOperandExpression instanceof PsiReferenceExpression) {
                                typeVariableName = (PsiReferenceExpression) rightOperandExpression;
                            } else if (rightOperandExpression instanceof PsiMethodCallExpression) {
                                typeMethodInvocation = (PsiMethodCallExpression) rightOperandExpression;
                            }
                        }
                    }
                    if (typeVariableName != null && staticFieldName != null) {
                        PsiVariable resolvedReference = (PsiVariable) typeVariableName.resolve();
                        typeVariableCounterMap.put(
                                resolvedReference,
                                typeVariableCounterMap.getOrDefault(resolvedReference, 0) + 1
                        );
                        analyzer.putTypeVariableExpression(typeVariableName, leafInfixExpression);
                        analyzer.putTypeVariableStaticField(typeVariableName, staticFieldName);
                    }
                    if (typeMethodInvocation != null && staticFieldName != null) {
                        PsiMethodCallExpression keyMethodInvocation = containsTypeMethodInvocationKey(typeMethodInvocation);
                        if (keyMethodInvocation != null) {
                            typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation) + 1);
                        } else {
                            typeMethodInvocationCounterMap.put(typeMethodInvocation, 1);
                        }
                        analyzer.putTypeMethodInvocationExpression(typeMethodInvocation, leafInfixExpression);
                        analyzer.putTypeMethodInvocationStaticField(typeMethodInvocation, staticFieldName);
                    }
                    if (typeVariableName != null && subclassType != null) {
                        PsiVariable resolvedReference = (PsiVariable) typeVariableName.resolve();
                        typeVariableCounterMap.put(
                                resolvedReference,
                                typeVariableCounterMap.getOrDefault(resolvedReference, 0) + 1
                        );
                        analyzer.putTypeVariableExpression(typeVariableName, leafInfixExpression);
                        analyzer.putTypeVariableSubclass(typeVariableName, subclassType);
                    }
                    if (typeMethodInvocation != null && subclassType != null) {
                        PsiMethodCallExpression keyMethodInvocation = containsTypeMethodInvocationKey(typeMethodInvocation);
                        if (keyMethodInvocation != null) {
                            typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation) + 1);
                        } else {
                            typeMethodInvocationCounterMap.put(typeMethodInvocation, 1);
                        }
                        analyzer.putTypeMethodInvocationExpression(typeMethodInvocation, leafInfixExpression);
                        analyzer.putTypeMethodInvocationSubclass(typeMethodInvocation, subclassType);
                    }
                }
                for (PsiInstanceOfExpression leafInstanceofExpression : analyzer.getInstanceofExpressions()) {
                    PsiExpression operandExpression = extractOperand(leafInstanceofExpression.getOperand());
                    if (operandExpression != null) {
                        if (operandExpression instanceof PsiReferenceExpression) {
                            PsiReferenceExpression operandName = (PsiReferenceExpression) operandExpression;
                            PsiVariable resolvedReference = (PsiVariable) operandName.resolve();
                            typeVariableCounterMap.put(
                                    resolvedReference,
                                    typeVariableCounterMap.getOrDefault(resolvedReference, 0) + 1
                            );
                            analyzer.putTypeVariableExpression(operandName, leafInstanceofExpression);
                            analyzer.putTypeVariableSubclass(operandName, leafInstanceofExpression.getCheckType().getType());
                        } else if (operandExpression instanceof PsiMethodCallExpression) {
                            PsiMethodCallExpression operandMethodInvocation = (PsiMethodCallExpression) operandExpression;
                            PsiMethodCallExpression keyMethodInvocation = containsTypeMethodInvocationKey(operandMethodInvocation);
                            if (keyMethodInvocation != null) {
                                typeMethodInvocationCounterMap.put(keyMethodInvocation, typeMethodInvocationCounterMap.get(keyMethodInvocation) + 1);
                            } else {
                                typeMethodInvocationCounterMap.put(operandMethodInvocation, 1);
                            }
                            analyzer.putTypeMethodInvocationExpression(operandMethodInvocation, leafInstanceofExpression);
                            analyzer.putTypeMethodInvocationSubclass(operandMethodInvocation, leafInstanceofExpression.getCheckType().getType());
                        }
                    }
                }
                complexExpressionMap.put(typeCheckExpression, analyzer);
            }
        }

        for (PsiVariable typeVariable : typeVariableCounterMap.keySet()) {
            if (isValidTypeVariable(typeVariable, typeCheckExpressions)) {
                for (PsiExpression complexExpression : complexExpressionMap.keySet()) {
                    IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
                    for (PsiReferenceExpression analyzerTypeVariable : analyzer.getTargetVariables()) {
                        if (typeVariable.equals(analyzerTypeVariable.resolve())) {
                            if (typeVariableCounterMap.get(typeVariable) == typeCheckExpressions.size()) {
                                typeCheckElimination.addRemainingIfStatementExpression(
                                        analyzer.getCompleteExpression(),
                                        analyzer.getRemainingExpression(analyzer.getTypeVariableExpression(analyzerTypeVariable))
                                );
                            }
                            List<PsiReferenceExpression> staticFieldReferences = analyzer.getTypeVariableStaticField(analyzerTypeVariable);
                            if (staticFieldReferences != null) {
                                List<PsiField> staticFields = staticFieldReferences.stream()
                                        .map(reference -> (PsiField) reference.resolve())
                                        .collect(Collectors.toList());
                                typeCheckElimination.addStaticType(analyzer.getCompleteExpression(), staticFields);
                            }
                            List<PsiType> subclassTypes = analyzer.getTypeVariableSubclass(analyzerTypeVariable);
                            if (subclassTypes != null) {
                                typeCheckElimination.addSubclassType(analyzer.getCompleteExpression(), subclassTypes);
                            }
                        }
                    }
                }
                if (typeVariable != null) {
                    if (typeVariable instanceof PsiField) {
                        for (PsiField field : fields) {
                            if (field.equals(typeVariable)) {
                                typeCheckElimination.setTypeField(field);
                                for (PsiMethod method : methods) {
                                    PsiElement resolvedField = MethodDeclarationUtility.isSetter(method);
                                    if (field.equals(resolvedField)) {
                                        typeCheckElimination.setTypeFieldSetterMethod(method);
                                    }
                                    PsiReferenceExpression fieldInstruction = (PsiReferenceExpression) MethodDeclarationUtility.isGetter(method);
                                    if (fieldInstruction != null && field.equals(fieldInstruction.resolve())) {
                                        typeCheckElimination.setTypeFieldGetterMethod(method);
                                    }
                                }
                                break;
                            }
                        }
                    } else if (typeVariable instanceof PsiParameter) {
                        PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                        for (PsiParameter parameter : parameters) {
                            if (parameter.equals(typeVariable)) {
                                typeCheckElimination.setTypeLocalVariable(parameter);
                                break;
                            }
                        }
                    } else {
                        StatementExtractor statementExtractor = new StatementExtractor();
                        List<PsiElement> variableDeclarationFragments = new ArrayList<>();
                        List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(getTypeCheckMethod().getBody());
                        for (PsiStatement statement : variableDeclarationStatements) {
                            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                            List<PsiElement> fragments = Arrays.asList(variableDeclarationStatement.getDeclaredElements());
                            variableDeclarationFragments.addAll(fragments);
                        }
                        for (PsiElement fragment : variableDeclarationFragments) {
                            if (fragment.equals(typeVariable)) {
                                typeCheckElimination.setTypeLocalVariable(typeVariable);
                                break;
                            }
                        }
                        List<PsiStatement> enhancedForStatements = statementExtractor.getEnhancedForStatements(getTypeCheckMethod().getBody());
                        for (PsiStatement eFStatement : enhancedForStatements) {
                            PsiForeachStatement enhancedForStatement = (PsiForeachStatement) eFStatement;
                            PsiParameter formalParameter = enhancedForStatement.getIterationParameter();
                            if (formalParameter.equals(typeVariable)) {
                                typeCheckElimination.setTypeLocalVariable(formalParameter);
                                break;
                            }
                        }
                    }
                }
            }
        }
        for (PsiMethodCallExpression typeMethodInvocation : typeMethodInvocationCounterMap.keySet()) {
            if (isValidTypeMethodInvocation(typeMethodInvocation, typeCheckExpressions)) {
                for (PsiExpression complexExpression : complexExpressionMap.keySet()) {
                    IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
                    for (PsiMethodCallExpression analyzerTypeMethodInvocation : analyzer.getTargetMethodInvocations()) {
                        PsiMethod resolvedMethod = analyzerTypeMethodInvocation.resolveMethod();
                        if (resolvedMethod != null && resolvedMethod.equals(typeMethodInvocation.resolveMethod())) {
                            if (typeMethodInvocationCounterMap.get(typeMethodInvocation) == typeCheckExpressions.size()) {
                                typeCheckElimination.addRemainingIfStatementExpression(analyzer.getCompleteExpression(),
                                        analyzer.getRemainingExpression(analyzer.getTypeMethodInvocationExpression(analyzerTypeMethodInvocation)));
                            }
                            List<PsiReferenceExpression> staticFieldReferences = analyzer.getTypeMethodInvocationStaticField(analyzerTypeMethodInvocation);
                            if (staticFieldReferences != null) {
                                List<PsiField> staticFields = staticFieldReferences.stream()
                                        .map(reference -> (PsiField) reference.resolve())
                                        .collect(Collectors.toList());
                                typeCheckElimination.addStaticType(analyzer.getCompleteExpression(), staticFields);
                            }
                            List<PsiType> subclassTypes = analyzer.getTypeMethodInvocationSubclass(analyzerTypeMethodInvocation);
                            if (subclassTypes != null) {
                                typeCheckElimination.addSubclassType(analyzer.getCompleteExpression(), subclassTypes);
                            }
                        }
                    }
                }
                PsiExpression invoker = typeMethodInvocation.getMethodExpression().getQualifierExpression();
                PsiMethod typeMethodInvocationBinding = typeMethodInvocation.resolveMethod();
                if (typeMethodInvocationBinding != null && invoker != null
                        && !getTypeDeclaration().equals(typeMethodInvocationBinding.getContainingClass())
                        && !(invoker instanceof PsiThisExpression)) {
                    typeCheckElimination.setTypeMethodInvocation(typeMethodInvocation);
                }
            }
        }
        processTypeCheckCodeFragmentBranches();
    }

    private void processTypeCheckCodeFragmentBranches() {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<ArrayList<PsiStatement>> allTypeCheckStatements = typeCheckElimination.getTypeCheckStatements();
        if (!typeCheckElimination.getDefaultCaseStatements().isEmpty()) {
            allTypeCheckStatements.add(typeCheckElimination.getDefaultCaseStatements());
        }
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiVariable> variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = new ArrayList<>();
        List<PsiStatement> variableDeclarationStatementsInsideTypeCheckMethod = statementExtractor.getVariableDeclarationStatements(getTypeCheckMethod().getBody());
        for (PsiStatement statement : variableDeclarationStatementsInsideTypeCheckMethod) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
            List<PsiVariable> declaredVariables = Arrays.stream(fragments)
                    .map(element -> (PsiVariable) element)
                    .collect(Collectors.toList());
            variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.addAll(declaredVariables);
        }
        for (ArrayList<PsiStatement> typeCheckStatementList : allTypeCheckStatements) {
            for (PsiStatement statement : typeCheckStatementList) {
                List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(statement);
                for (PsiStatement statement2 : variableDeclarationStatements) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement2;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    List<PsiVariable> declaredVariables = Arrays.stream(fragments)
                            .map(element -> (PsiVariable) element)
                            .collect(Collectors.toList());
                    variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(declaredVariables);
                }
            }
        }
        List<PsiStatement> enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment = new ArrayList<>();
        for (PsiStatement statementInBlock : getTypeCheckMethod().getBody().getStatements()) {
            enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.addAll(statementExtractor.getEnhancedForStatements(statementInBlock));
        }
        for (ArrayList<PsiStatement> typeCheckStatementList : allTypeCheckStatements) {
            for (PsiStatement statement : typeCheckStatementList) {
                enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment.removeAll(statementExtractor.getEnhancedForStatements(statement));
            }
        }
        for (ArrayList<PsiStatement> typeCheckStatementList : allTypeCheckStatements) {
            for (PsiStatement statement : typeCheckStatementList) {
                //checking for methods of the Source class invoked inside the type-checking branches
                List<PsiExpression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
                for (PsiExpression expression : methodInvocations) {
                    if (expression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                        PsiMethod resolvedMethod = methodInvocation.resolveMethod();
                        if (resolvedMethod == null) continue;
                        PsiExpression invoker = methodInvocation.getMethodExpression().getQualifierExpression();
                        if (getTypeDeclaration().equals(resolvedMethod.getContainingClass())) {
                            for (PsiMethod method : methods) {
                                if (method.equals(resolvedMethod)) {
                                    typeCheckElimination.addAccessedMethod(method);
                                }
                            }
                        } else if (invoker == null || invoker instanceof PsiThisExpression) {
                            PsiClass psiClass = resolvedMethod.getContainingClass();
                            PsiClass superclassTypeBinding = getTypeDeclaration().getSuperClass();
                            while (superclassTypeBinding != null && !superclassTypeBinding.equals(psiClass)) {
                                superclassTypeBinding = superclassTypeBinding.getSuperClass();
                            }
                            if (psiClass != null && psiClass.equals(superclassTypeBinding))
                                typeCheckElimination.addSuperAccessedMethod(resolvedMethod);
                        }
                    }
                }
                //checking for Source class fields or parameters of the type-checking method accessed inside the type-checking branches
                List<PsiExpression> variableInstructions = expressionExtractor.getVariableInstructions(statement);
                for (PsiExpression variableInstruction : variableInstructions) {
                    PsiReferenceExpression simpleName = (PsiReferenceExpression) variableInstruction;
                    PsiElement variableInstructionBinding = simpleName.resolve();
                    if (variableInstructionBinding instanceof PsiVariable) {
                        PsiVariable variableInstructionVariableBinding = (PsiVariable) variableInstructionBinding;
                        if (variableInstructionVariableBinding instanceof PsiField) {
                            PsiField variableInstructionFieldBinding = (PsiField) variableInstructionVariableBinding;
                            if (variableInstructionFieldBinding.getContainingClass() != null) {
                                if (getTypeDeclaration().equals(variableInstructionFieldBinding.getContainingClass())) {
                                    for (PsiField field : fields) {
                                        if (field.equals(variableInstructionFieldBinding)) {
                                            boolean isAssigned = false;
                                            if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                                PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                                PsiExpression leftHandSide = assignment.getLExpression();
                                                PsiReferenceExpression leftHandSideName = null;
                                                if (leftHandSide instanceof PsiReferenceExpression) {
                                                    leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                                }
                                                if (leftHandSideName != null && leftHandSideName.equals(simpleName)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedField(field);
                                                    if (!assignment.getOperationTokenType().equals(JavaTokenType.EQ))
                                                        typeCheckElimination.addAccessedField(field);
                                                }
                                            } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedField(field);
                                                typeCheckElimination.addAccessedField(field);
                                            } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                                PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                                IElementType operator = prefixExpression.getOperationTokenType();
                                                if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedField(field);
                                                    typeCheckElimination.addAccessedField(field);
                                                }
                                            }
                                            if (!isAssigned)
                                                typeCheckElimination.addAccessedField(field);
                                        }
                                    }
                                } else {
                                    PsiClass superclassTypeBinding = getTypeDeclaration().getSuperClass();
                                    while (superclassTypeBinding != null && !superclassTypeBinding.equals(variableInstructionFieldBinding.getContainingClass())) {
                                        superclassTypeBinding = superclassTypeBinding.getSuperClass();
                                    }
                                    if (variableInstructionFieldBinding.getContainingClass().equals(superclassTypeBinding)) {
                                        boolean isAssigned = false;
                                        if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                            PsiExpression leftHandSide = assignment.getLExpression();
                                            PsiReferenceExpression leftHandSideName = null;
                                            if (leftHandSide instanceof PsiReferenceExpression) {
                                                leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                            }
                                            if (leftHandSideName != null && leftHandSideName.equals(simpleName)) {
                                                isAssigned = true;
                                                typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionFieldBinding, null);
                                                if (!assignment.getOperationTokenType().equals(JavaTokenType.EQ))
                                                    typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionFieldBinding, null);
                                            }
                                        } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                            isAssigned = true;
                                            typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionFieldBinding, null);
                                            typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionFieldBinding, null);
                                        } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                            IElementType operator = prefixExpression.getOperationTokenType();
                                            if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                isAssigned = true;
                                                typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionFieldBinding, null);
                                                typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionFieldBinding, null);
                                            }
                                        }
                                        if (!isAssigned)
                                            typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionFieldBinding, null);
                                    }
                                }
                            }
                        } else if (variableInstructionVariableBinding instanceof PsiParameter) {
                            PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                            for (PsiParameter parameter : parameters) {
                                if (parameter.equals(variableInstructionVariableBinding)) {
                                    boolean isAssigned = false;
                                    if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                        PsiExpression leftHandSide = assignment.getLExpression();
                                        if (leftHandSide instanceof PsiReferenceExpression) {
                                            PsiReferenceExpression leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                            if (leftHandSideName.equals(simpleName)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedParameter(parameter);
                                            }
                                        }
                                    } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                        isAssigned = true;
                                        typeCheckElimination.addAssignedParameter(parameter);
                                    } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                        PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                        IElementType operator = prefixExpression.getOperationTokenType();
                                        if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedParameter(parameter);
                                        }
                                    }
                                    if (!isAssigned)
                                        typeCheckElimination.addAccessedParameter(parameter);
                                    break;
                                }
                            }
                        }
                        //checking for local variables accessed inside the type-checking code branches, but declared outside them
                        else {
                            for (PsiVariable fragment : variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
                                if (fragment.equals(variableInstructionVariableBinding)) {
                                    boolean isAssigned = false;
                                    if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                        PsiExpression leftHandSide = assignment.getLExpression();
                                        if (leftHandSide instanceof PsiReferenceExpression) {
                                            PsiReferenceExpression leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                            if (leftHandSideName.equals(simpleName)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedLocalVariable(fragment);
                                            }
                                        }
                                    } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                        isAssigned = true;
                                        typeCheckElimination.addAssignedLocalVariable(fragment);
                                    } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                        PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                        IElementType operator = prefixExpression.getOperationTokenType();
                                        if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedLocalVariable(fragment);
                                        }
                                    }
                                    if (!isAssigned)
                                        typeCheckElimination.addAccessedLocalVariable(fragment);
                                    break;
                                }
                            }
                            for (PsiStatement eFStatement : enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
                                PsiForeachStatement enhancedForStatement = (PsiForeachStatement) eFStatement;
                                PsiParameter formalParameter = enhancedForStatement.getIterationParameter();
                                if (formalParameter.equals(variableInstructionVariableBinding)) {
                                    boolean isAssigned = false;
                                    if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                        PsiExpression leftHandSide = assignment.getLExpression();
                                        if (leftHandSide instanceof PsiReferenceExpression) {
                                            PsiReferenceExpression leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                            if (leftHandSideName.equals(simpleName)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                            }
                                        }
                                    } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                        isAssigned = true;
                                        typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                    } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                        PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                        IElementType operator = prefixExpression.getOperationTokenType();
                                        if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                        }
                                    }
                                    if (!isAssigned)
                                        typeCheckElimination.addAccessedLocalVariable(formalParameter);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        processRemainingIfStatementExpressions(
                variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment,
                enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment
        );
    }

    private void processRemainingIfStatementExpressions(
            List<PsiVariable> variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment,
            List<PsiStatement> enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        for (PsiExpression complexExpression : complexExpressionMap.keySet()) {
            DefaultMutableTreeNode root = typeCheckElimination.getRemainingIfStatementExpression(complexExpression);
            if (root != null) {
                DefaultMutableTreeNode leaf = root.getFirstLeaf();
                while (leaf != null) {
                    PsiExpression leafExpression = (PsiExpression) leaf.getUserObject();
                    //checking for methods of the Source class invoked inside the type-checking branches
                    List<PsiExpression> methodInvocations = expressionExtractor.getMethodInvocations(leafExpression);
                    for (PsiExpression expression : methodInvocations) {
                        if (expression instanceof PsiMethodCallExpression) {
                            PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                            PsiMethod resolvedMethod = methodInvocation.resolveMethod();
                            PsiExpression invoker = methodInvocation.getMethodExpression().getQualifierExpression();
                            if (resolvedMethod == null) continue;
                            if (getTypeDeclaration().equals(resolvedMethod.getContainingClass())) {
                                for (PsiMethod method : methods) {
                                    if (method.equals(resolvedMethod)) {
                                        typeCheckElimination.addAccessedMethod(method);
                                    }
                                }
                            } else if (invoker == null || (invoker instanceof PsiThisExpression)) {
                                PsiClass superclassTypeBinding = getTypeDeclaration().getSuperClass();
                                while (superclassTypeBinding != null && !superclassTypeBinding.equals(resolvedMethod.getContainingClass())) {
                                    superclassTypeBinding = superclassTypeBinding.getContainingClass();
                                }
                                if (resolvedMethod.getContainingClass().equals(superclassTypeBinding))
                                    typeCheckElimination.addSuperAccessedMethod(resolvedMethod);
                            }
                        }
                    }
                    //checking for Source class fields or parameters of the type-checking method accessed inside the type-checking branches
                    List<PsiExpression> variableInstructions = expressionExtractor.getVariableInstructions(leafExpression);
                    for (PsiExpression variableInstruction : variableInstructions) {
                        PsiReferenceExpression simpleName = (PsiReferenceExpression) variableInstruction;
                        PsiElement variableInstructionBinding = simpleName.resolve();
                        if (variableInstructionBinding instanceof PsiVariable) {
                            PsiVariable variableInstructionVariableBinding = (PsiVariable) variableInstructionBinding;
                            if (variableInstructionVariableBinding instanceof PsiField) {
                                PsiField variableInstructionField = (PsiField) variableInstructionVariableBinding;
                                if (variableInstructionField.getContainingClass() != null) {
                                    if (getTypeDeclaration().equals(variableInstructionField.getContainingClass())) {
                                        for (PsiField field : fields) {
                                            if (field.equals(variableInstructionVariableBinding)) {
                                                boolean isAssigned = false;
                                                if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                                    PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                                    PsiExpression leftHandSide = assignment.getLExpression();
                                                    PsiReferenceExpression leftHandSideName = null;
                                                    if (leftHandSide instanceof PsiReferenceExpression) {
                                                        leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                                    }
                                                    if (leftHandSideName != null && leftHandSideName.equals(simpleName)) {
                                                        isAssigned = true;
                                                        typeCheckElimination.addAssignedField(field);
                                                        if (!assignment.getOperationTokenType().equals(JavaTokenType.EQ))
                                                            typeCheckElimination.addAccessedField(field);
                                                    }
                                                } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedField(field);
                                                    typeCheckElimination.addAccessedField(field);
                                                } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                                    PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                                    IElementType operator = prefixExpression.getOperationTokenType();
                                                    if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                        isAssigned = true;
                                                        typeCheckElimination.addAssignedField(field);
                                                        typeCheckElimination.addAccessedField(field);
                                                    }
                                                }
                                                if (!isAssigned)
                                                    typeCheckElimination.addAccessedField(field);
                                            }

                                        }
                                    } else {
                                        PsiClass superclassTypeBinding = getTypeDeclaration().getSuperClass();
                                        while (superclassTypeBinding != null && !superclassTypeBinding.equals(variableInstructionField.getContainingClass())) {
                                            superclassTypeBinding = superclassTypeBinding.getSuperClass();
                                        }
                                        if (variableInstructionField.getContainingClass().equals(superclassTypeBinding)) {
                                            boolean isAssigned = false;
                                            if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                                PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                                PsiExpression leftHandSide = assignment.getLExpression();
                                                PsiReferenceExpression leftHandSideName = null;
                                                if (leftHandSide instanceof PsiReferenceExpression) {
                                                    leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                                }
                                                if (leftHandSideName != null && leftHandSideName.equals(simpleName)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionField, null);
                                                    if (!assignment.getOperationTokenType().equals(JavaTokenType.EQ))
                                                        typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionField, null);
                                                }
                                            } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                                isAssigned = true;
                                                typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionField, null);
                                                typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionField, null);
                                            } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                                PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                                IElementType operator = prefixExpression.getOperationTokenType();
                                                if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addSuperAssignedFieldBinding(variableInstructionField, null);
                                                    typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionField, null);
                                                }
                                            }
                                            if (!isAssigned)
                                                typeCheckElimination.addSuperAccessedFieldBinding(variableInstructionField, null);
                                        }
                                    }
                                }
                            } else if (variableInstructionVariableBinding instanceof PsiParameter) {
                                PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                                for (PsiParameter parameter : parameters) {
                                    if (parameter.equals(variableInstructionVariableBinding)) {
                                        boolean isAssigned = false;
                                        if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                            PsiExpression leftHandSide = assignment.getLExpression();
                                            if (leftHandSide instanceof PsiReferenceExpression) {
                                                if (leftHandSide.equals(simpleName)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedParameter(parameter);
                                                }
                                            }
                                        } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedParameter(parameter);
                                        } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                            IElementType operator = prefixExpression.getOperationTokenType();
                                            if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedParameter(parameter);
                                            }
                                        }
                                        if (!isAssigned)
                                            typeCheckElimination.addAccessedParameter(parameter);
                                        break;
                                    }
                                }
                            }
                            //checking for local variables accessed inside the type-checking code branches, but declared outside them
                            else {
                                for (PsiVariable variable : variableDeclarationFragmentsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
                                    if (variable.equals(variableInstructionVariableBinding)) {
                                        boolean isAssigned = false;
                                        if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                            PsiExpression leftHandSide = assignment.getLExpression();
                                            if (leftHandSide instanceof PsiReferenceExpression) {
                                                if (leftHandSide.equals(simpleName)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedLocalVariable(variable);
                                                }
                                            }
                                        } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedLocalVariable(variable);
                                        } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                            IElementType operator = prefixExpression.getOperationTokenType();
                                            if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedLocalVariable(variable);
                                            }
                                        }
                                        if (!isAssigned)
                                            typeCheckElimination.addAccessedLocalVariable(variable);
                                        break;
                                    }
                                }
                                for (PsiStatement eFStatement : enhancedForStatementsInsideTypeCheckMethodApartFromTypeCheckCodeFragment) {
                                    PsiForeachStatement enhancedForStatement = (PsiForeachStatement) eFStatement;
                                    PsiParameter formalParameter = enhancedForStatement.getIterationParameter();
                                    if (formalParameter.equals(variableInstructionVariableBinding)) {
                                        boolean isAssigned = false;
                                        if (simpleName.getParent() instanceof PsiAssignmentExpression) {
                                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) simpleName.getParent();
                                            PsiExpression leftHandSide = assignment.getLExpression();
                                            if (leftHandSide instanceof PsiReferenceExpression) {
                                                PsiReferenceExpression leftHandSideName = (PsiReferenceExpression) leftHandSide;
                                                if (leftHandSideName.equals(simpleName)) {
                                                    isAssigned = true;
                                                    typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                                }
                                            }
                                        } else if (simpleName.getParent() instanceof PsiPostfixExpression) {
                                            isAssigned = true;
                                            typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                        } else if (simpleName.getParent() instanceof PsiPrefixExpression) {
                                            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) simpleName.getParent();
                                            IElementType operator = prefixExpression.getOperationTokenType();
                                            if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                                                isAssigned = true;
                                                typeCheckElimination.addAssignedLocalVariable(formalParameter);
                                            }
                                        }
                                        if (!isAssigned)
                                            typeCheckElimination.addAccessedLocalVariable(formalParameter);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    leaf = leaf.getNextLeaf();
                }
            }
        }
    }

    private PsiExpression extractOperand(PsiExpression operand) {
        if (operand instanceof PsiLiteralExpression) {
            return operand;
        } else if (operand instanceof PsiReferenceExpression) {
            return operand;
        } else if (operand instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) operand;
            for (PsiMethod method : methods) {
                PsiExpression fieldInstruction = MethodDeclarationUtility.isGetter(method);
                if (fieldInstruction != null && method.equals(methodInvocation.resolveMethod())) {
                    return fieldInstruction;
                }
                PsiMethodCallExpression delegateMethodInvocation = MethodDeclarationUtility.isDelegate(method);
                if (delegateMethodInvocation != null && method.equals(methodInvocation.resolveMethod())) {
                    return delegateMethodInvocation;
                }
            }
            return methodInvocation;
        }
        return null;
    }

    private boolean isValidTypeVariable(PsiVariable typeVariable, Set<PsiExpression> typeCheckExpressions) {
        int validTypeCheckExpressions = 0;
        int typeVariableCounter = 0;
        for (PsiExpression complexExpression : complexExpressionMap.keySet()) {
            IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
            for (PsiReferenceExpression analyzerTypeVariable : analyzer.getTargetVariables()) {
                if (typeVariable.equals(analyzerTypeVariable.resolve())) {
                    List<PsiReferenceExpression> staticFields = analyzer.getTypeVariableStaticField(analyzerTypeVariable);
                    if (staticFields != null && staticFields.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
                        validTypeCheckExpressions++;
                        typeVariableCounter++;
                    }
                    if (staticFields != null && analyzer.getNumberOfConditionalOperatorNodes() == staticFields.size() - 1 &&
                            staticFields.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
                        validTypeCheckExpressions++;
                        typeVariableCounter += staticFields.size();
                    }
                    List<PsiType> subclasses = analyzer.getTypeVariableSubclass(analyzerTypeVariable);
                    if (subclasses != null && subclasses.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
                        validTypeCheckExpressions++;
                        typeVariableCounter++;
                    }
                    if (subclasses != null && analyzer.getNumberOfConditionalOperatorNodes() == subclasses.size() - 1 &&
                            subclasses.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
                        validTypeCheckExpressions++;
                        typeVariableCounter += subclasses.size();
                    }
                }
            }
        }
        return validTypeCheckExpressions == typeCheckExpressions.size() &&
                typeVariableCounter == typeVariableCounterMap.get(typeVariable);
    }

    private boolean isValidTypeMethodInvocation(PsiMethodCallExpression typeMethodInvocation, Set<PsiExpression> typeCheckExpressions) {
        int validTypeCheckExpressions = 0;
        int typeMethodInvocationCounter = 0;
        for (PsiExpression complexExpression : complexExpressionMap.keySet()) {
            IfStatementExpressionAnalyzer analyzer = complexExpressionMap.get(complexExpression);
            for (PsiMethodCallExpression analyzerTypeMethodInvocation : analyzer.getTargetMethodInvocations()) {
                PsiMethod resolvedMethod = analyzerTypeMethodInvocation.resolveMethod();
                if (resolvedMethod != null && resolvedMethod.equals(typeMethodInvocation.resolveMethod())) {
                    List<PsiReferenceExpression> staticFields = analyzer.getTypeMethodInvocationStaticField(analyzerTypeMethodInvocation);
                    if (staticFields != null && staticFields.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
                        validTypeCheckExpressions++;
                        typeMethodInvocationCounter++;
                    }
                    if (staticFields != null && analyzer.getNumberOfConditionalOperatorNodes() == staticFields.size() - 1 &&
                            staticFields.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
                        validTypeCheckExpressions++;
                        typeMethodInvocationCounter += staticFields.size();
                    }
                    List<PsiType> subclasses = analyzer.getTypeMethodInvocationSubclass(analyzerTypeMethodInvocation);
                    if (subclasses != null && subclasses.size() == 1 && analyzer.allParentNodesAreConditionalAndOperators()) {
                        validTypeCheckExpressions++;
                        typeMethodInvocationCounter++;
                    }
                    if (subclasses != null && analyzer.getNumberOfConditionalOperatorNodes() == subclasses.size() - 1 &&
                            subclasses.size() > 1 && analyzer.allParentNodesAreConditionalOrOperators()) {
                        validTypeCheckExpressions++;
                        typeMethodInvocationCounter += subclasses.size();
                    }
                }
            }
        }
        return validTypeCheckExpressions == typeCheckExpressions.size()
                && typeMethodInvocationCounter == typeMethodInvocationCounterMap.get(typeMethodInvocation);
    }

    private PsiMethodCallExpression containsTypeMethodInvocationKey(PsiMethodCallExpression methodInvocation) {
        for (PsiMethodCallExpression keyMethodInvocation : typeMethodInvocationCounterMap.keySet()) {
            PsiMethod resolvedMethod = keyMethodInvocation.resolveMethod();
            if (resolvedMethod != null && resolvedMethod.equals(methodInvocation.resolveMethod()))
                return keyMethodInvocation;
        }
        return null;
    }

    private PsiMethod getTypeCheckMethod() {
        return (PsiMethod) typeCheckMethod.getElement();
    }

    private PsiClass getTypeDeclaration() {
        return (PsiClass) typeDeclaration.getElement();
    }
}
