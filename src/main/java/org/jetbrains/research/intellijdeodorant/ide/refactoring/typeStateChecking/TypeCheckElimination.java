package org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.CompositeStatementObject;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;
import org.jetbrains.research.intellijdeodorant.core.ast.util.StatementExtractor;
import org.jetbrains.research.intellijdeodorant.inheritance.InheritanceTree;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class TypeCheckElimination implements Comparable<TypeCheckElimination> {
    private final Map<PsiExpression, ArrayList<PsiStatement>> typeCheckMap;
    private final ArrayList<PsiStatement> defaultCaseStatements;
    private final Map<PsiExpression, List<PsiField>> staticFieldMap;
    private final Map<PsiExpression, List<PsiType>> subclassTypeMap;
    private SmartPsiElementPointer<PsiElement> typeField;
    private SmartPsiElementPointer<PsiElement> typeFieldGetterMethod;
    private SmartPsiElementPointer<PsiElement> typeFieldSetterMethod;
    private SmartPsiElementPointer<PsiElement> typeCheckCodeFragment;
    private CompositeStatementObject typeCheckCompositeStatement;
    private SmartPsiElementPointer<PsiElement> typeCheckMethod;
    private SmartPsiElementPointer<PsiElement> typeCheckClass;
    private final LinkedHashSet<PsiField> additionalStaticFields;
    private final LinkedHashSet<PsiField> accessedFields;
    private final LinkedHashSet<PsiField> assignedFields;
    private final LinkedHashMap<PsiField, PsiMethod> superAccessedFieldMap;
    private final LinkedHashMap<PsiField, PsiMethod> superAccessedFieldBindingMap;
    private final LinkedHashMap<PsiField, PsiMethod> superAssignedFieldMap;
    private final LinkedHashMap<PsiField, PsiMethod> superAssignedFieldBindingMap;
    private final LinkedHashSet<PsiParameter> accessedParameters;
    private final LinkedHashSet<PsiParameter> assignedParameters;
    private final LinkedHashSet<PsiVariable> accessedLocalVariables;
    private final LinkedHashSet<PsiVariable> assignedLocalVariables;
    private final LinkedHashSet<PsiMethod> accessedMethods;
    private final LinkedHashSet<PsiMethod> superAccessedMethods;
    private SmartPsiElementPointer<PsiElement> typeLocalVariable;
    private SmartPsiElementPointer<PsiExpression> typeMethodInvocation;
    private SmartPsiElementPointer<PsiElement> foreignTypeField;
    private InheritanceTree existingInheritanceTree;
    private InheritanceTree inheritanceTreeMatchingWithStaticTypes;
    private final Map<PsiElement, String> staticFieldSubclassTypeMap;
    private final Map<PsiExpression, DefaultMutableTreeNode> remainingIfStatementExpressionMap;
    private String abstractMethodName;
    private volatile int hashCode = 0;
    private int groupSizeAtClassLevel;
    private double averageNumberOfStatements;

    public TypeCheckElimination() {
        this.typeCheckMap = new LinkedHashMap<>();
        this.defaultCaseStatements = new ArrayList<>();
        this.staticFieldMap = new LinkedHashMap<>();
        this.subclassTypeMap = new LinkedHashMap<>();
        this.typeField = null;
        this.typeFieldGetterMethod = null;
        this.typeFieldSetterMethod = null;
        this.typeCheckCodeFragment = null;
        this.typeCheckMethod = null;
        this.typeCheckClass = null;
        this.additionalStaticFields = new LinkedHashSet<>();
        this.accessedFields = new LinkedHashSet<>();
        this.assignedFields = new LinkedHashSet<>();
        this.superAccessedFieldMap = new LinkedHashMap<>();
        this.superAccessedFieldBindingMap = new LinkedHashMap<>();
        this.superAssignedFieldMap = new LinkedHashMap<>();
        this.superAssignedFieldBindingMap = new LinkedHashMap<>();
        this.accessedParameters = new LinkedHashSet<>();
        this.assignedParameters = new LinkedHashSet<>();
        this.accessedLocalVariables = new LinkedHashSet<>();
        this.assignedLocalVariables = new LinkedHashSet<>();
        this.accessedMethods = new LinkedHashSet<>();
        this.superAccessedMethods = new LinkedHashSet<>();
        this.typeLocalVariable = null;
        this.typeMethodInvocation = null;
        this.foreignTypeField = null;
        this.existingInheritanceTree = null;
        this.inheritanceTreeMatchingWithStaticTypes = null;
        this.staticFieldSubclassTypeMap = new LinkedHashMap<>();
        this.remainingIfStatementExpressionMap = new LinkedHashMap<>();
        this.abstractMethodName = null;
    }

    public void addTypeCheck(PsiExpression expression, PsiStatement statement) {
        if (typeCheckMap.containsKey(expression)) {
            ArrayList<PsiStatement> statements = typeCheckMap.get(expression);
            statements.add(statement);
        } else {
            ArrayList<PsiStatement> statements = new ArrayList<>();
            statements.add(statement);
            typeCheckMap.put(expression, statements);
        }
    }

    public void addEmptyTypeCheck(PsiExpression expression) {
        if (!typeCheckMap.containsKey(expression)) {
            ArrayList<PsiStatement> statements = new ArrayList<>();
            typeCheckMap.put(expression, statements);
        }
    }

    public boolean containsTypeCheckExpression(PsiExpression expression) {
        return typeCheckMap.containsKey(expression);
    }

    public void addDefaultCaseStatement(PsiStatement statement) {
        defaultCaseStatements.add(statement);
    }

    public void addStaticType(PsiExpression expression, List<PsiField> simpleNameGroup) {
        staticFieldMap.put(expression, simpleNameGroup);
    }

    public void addSubclassType(PsiExpression expression, List<PsiType> subclassTypeGroup) {
        subclassTypeMap.put(expression, subclassTypeGroup);
    }

    public void addRemainingIfStatementExpression(PsiExpression expression, DefaultMutableTreeNode root) {
        remainingIfStatementExpressionMap.put(expression, root);
    }

    public void addAdditionalStaticField(PsiField staticField) {
        additionalStaticFields.add(staticField);
    }

    public void addAccessedField(PsiField fragment) {
        accessedFields.add(fragment);
    }

    public void addAssignedField(PsiField fragment) {
        assignedFields.add(fragment);
    }

    public void addSuperAccessedField(PsiField fragment, PsiMethod method) {
        superAccessedFieldMap.put(fragment, method);
    }

    public void addSuperAccessedFieldBinding(PsiField variableBinding, PsiMethod methodBinding) {
        superAccessedFieldBindingMap.put(variableBinding, methodBinding);
    }

    public PsiMethod getGetterMethodBindingOfSuperAccessedField(PsiField variableBinding) {
        return superAccessedFieldBindingMap.get(variableBinding);
    }

    public Set<PsiField> getSuperAccessedFields() {
        return superAccessedFieldMap.keySet();
    }

    public Set<PsiField> getSuperAccessedFieldBindings() {
        return superAccessedFieldBindingMap.keySet();
    }

    public void addSuperAssignedField(PsiField fragment, PsiMethod method) {
        superAssignedFieldMap.put(fragment, method);
    }

    public void addSuperAssignedFieldBinding(PsiField variableBinding, PsiMethod methodBinding) {
        superAssignedFieldBindingMap.put(variableBinding, methodBinding);
    }

    public PsiMethod getSetterMethodBindingOfSuperAssignedField(PsiField variableBinding) {
        return superAssignedFieldBindingMap.get(variableBinding);
    }

    public Set<PsiField> getSuperAssignedFields() {
        return superAssignedFieldMap.keySet();
    }

    public Set<PsiField> getSuperAssignedFieldBindings() {
        return superAssignedFieldBindingMap.keySet();
    }

    public void addAccessedLocalVariable(PsiVariable fragment) {
        accessedLocalVariables.add(fragment);
    }

    public void addAssignedLocalVariable(PsiVariable fragment) {
        assignedLocalVariables.add(fragment);
    }

    public void addAccessedParameter(PsiParameter parameter) {
        accessedParameters.add(parameter);
    }

    public void addAssignedParameter(PsiParameter parameter) {
        assignedParameters.add(parameter);
    }

    public void addAccessedMethod(PsiMethod method) {
        accessedMethods.add(method);
    }

    public void addSuperAccessedMethod(PsiMethod method) {
        superAccessedMethods.add(method);
    }

    public LinkedHashSet<PsiVariable> getAccessedLocalVariables() {
        return accessedLocalVariables;
    }

    public Set<PsiField> getAccessedFields() {
        return accessedFields;
    }

    public Set<PsiField> getAssignedFields() {
        return assignedFields;
    }

    public Set<PsiParameter> getAccessedParameters() {
        return accessedParameters;
    }

    public Set<PsiMethod> getAccessedMethods() {
        return accessedMethods;
    }

    public Set<PsiMethod> getSuperAccessedMethods() {
        return superAccessedMethods;
    }

    public Set<PsiExpression> getTypeCheckExpressions() {
        return typeCheckMap.keySet();
    }

    public ArrayList<PsiStatement> getTypeCheckStatements(PsiExpression expression) {
        return typeCheckMap.get(expression);
    }

    public List<ArrayList<PsiStatement>> getTypeCheckStatements() {
        return new ArrayList<>(typeCheckMap.values());
    }

    public ArrayList<PsiStatement> getDefaultCaseStatements() {
        return defaultCaseStatements;
    }

    public List<PsiField> getStaticFields(PsiExpression expression) {
        return staticFieldMap.get(expression);
    }

    public List<PsiField> getStaticFields() {
        ArrayList<PsiField> staticFields = new ArrayList<>();
        for (PsiExpression expression : typeCheckMap.keySet()) {
            List<PsiField> simpleNameGroup = staticFieldMap.get(expression);
            if (simpleNameGroup != null) {
                staticFields.addAll(simpleNameGroup);
            }
        }
        return staticFields;
    }

    public Set<PsiField> getAdditionalStaticFields() {
        return additionalStaticFields;
    }

    public DefaultMutableTreeNode getRemainingIfStatementExpression(PsiExpression expression) {
        return remainingIfStatementExpressionMap.get(expression);
    }

    public PsiExpression getExpressionCorrespondingToTypeCheckStatementList(ArrayList<PsiStatement> statements) {
        for (PsiExpression expression : typeCheckMap.keySet()) {
            if (statements.equals(typeCheckMap.get(expression)))
                return expression;
        }
        return null;
    }

    public PsiField getTypeField() {
        return typeField == null ? null : (PsiField) typeField.getElement();
    }

    public void setTypeField(PsiField typeField) {
        this.typeField = toPointer(typeField);
    }

    public PsiMethod getTypeFieldGetterMethod() {
        return typeFieldGetterMethod == null ? null : (PsiMethod) typeFieldGetterMethod.getElement();
    }

    public void setTypeFieldGetterMethod(PsiMethod typeFieldGetterMethod) {
        this.typeFieldGetterMethod = toPointer(typeFieldGetterMethod);
    }

    public PsiMethod getTypeFieldSetterMethod() {
        return typeFieldSetterMethod == null ? null : (PsiMethod) typeFieldSetterMethod.getElement();
    }

    public void setTypeFieldSetterMethod(PsiMethod typeFieldSetterMethod) {
        this.typeFieldSetterMethod = toPointer(typeFieldSetterMethod);
    }

    public PsiStatement getTypeCheckCodeFragment() {
        return (PsiStatement) typeCheckCodeFragment.getElement();
    }

    public void setTypeCheckCodeFragment(PsiStatement typeCheckCodeFragment) {
        this.typeCheckCodeFragment = toPointer(typeCheckCodeFragment);
    }

    public CompositeStatementObject getTypeCheckCompositeStatement() {
        return typeCheckCompositeStatement;
    }

    public void setTypeCheckCompositeStatement(CompositeStatementObject typeCheckCompositeStatement) {
        this.typeCheckCompositeStatement = typeCheckCompositeStatement;
    }

    public PsiMethod getTypeCheckMethod() {
        return (PsiMethod) typeCheckMethod.getElement();
    }

    public void setTypeCheckMethod(PsiMethod typeCheckMethod) {
        this.typeCheckMethod = toPointer(typeCheckMethod);
        this.abstractMethodName = typeCheckMethod.getName();
    }

    public PsiClass getTypeCheckClass() {
        return (PsiClass) typeCheckClass.getElement();
    }

    public void setTypeCheckClass(PsiClass typeCheckClass) {
        this.typeCheckClass = toPointer(typeCheckClass);
    }

    public PsiVariable getTypeLocalVariable() {
        return typeLocalVariable == null ? null : (PsiVariable) typeLocalVariable.getElement();
    }

    public void setTypeLocalVariable(PsiVariable typeLocalVariable) {
        this.typeLocalVariable = toPointer(typeLocalVariable);
    }

    public PsiMethodCallExpression getTypeMethodInvocation() {
        return typeMethodInvocation == null ? null : (PsiMethodCallExpression) typeMethodInvocation.getElement();
    }

    public void setTypeMethodInvocation(PsiMethodCallExpression typeMethodInvocation) {
        this.typeMethodInvocation = toPointer(typeMethodInvocation);
    }

    public PsiField getForeignTypeField() {
        return foreignTypeField == null ? null : (PsiField) foreignTypeField.getElement();
    }

    public void setForeignTypeField(PsiField foreignTypeField) {
        this.foreignTypeField = toPointer(foreignTypeField);
    }

    public InheritanceTree getExistingInheritanceTree() {
        return existingInheritanceTree;
    }

    public void setExistingInheritanceTree(InheritanceTree existingInheritanceTree) {
        this.existingInheritanceTree = existingInheritanceTree;
    }

    public InheritanceTree getInheritanceTreeMatchingWithStaticTypes() {
        return inheritanceTreeMatchingWithStaticTypes;
    }

    public void setInheritanceTreeMatchingWithStaticTypes(InheritanceTree inheritanceTree) {
        this.inheritanceTreeMatchingWithStaticTypes = inheritanceTree;
    }

    public void putStaticFieldSubclassTypeMapping(PsiField staticField, String subclassType) {
        staticFieldSubclassTypeMap.put(staticField, subclassType);
    }

    public boolean allTypeCheckingsContainStaticFieldOrSubclassType() {
        return (typeCheckMap.keySet().size() > 1 || (typeCheckMap.keySet().size() == 1 && !defaultCaseStatements.isEmpty())) &&
                (typeCheckMap.keySet().size() == (staticFieldMap.keySet().size() + subclassTypeMap.keySet().size()));
    }

    public boolean isApplicable() {
        return !containsLocalVariableAssignment() && !containsBranchingStatement() && !containsSuperMethodInvocation()
                && !containsSuperFieldAccess() && !isSubclassTypeAnInterface()
                && !returnStatementAfterTypeCheckCodeFragment() && !typeCheckClassPartOfExistingInheritanceTree();
    }

    private boolean typeCheckClassPartOfExistingInheritanceTree() {
        Collection<List<PsiType>> subTypeCollection = subclassTypeMap.values();
        for (List<PsiType> subTypes : subTypeCollection) {
            for (PsiType subType : subTypes) {
                if (subType.equals(PsiTypesUtil.getClassType(getTypeCheckClass()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSubclassTypeAnInterface() {
        for (List<PsiType> subTypes : subclassTypeMap.values()) {
            for (PsiType subType : subTypes) {
                if (!(subType instanceof PsiClassType)) {
                    continue;
                }
                PsiClass resolvedClass = ((PsiClassType) subType).resolve();
                if (resolvedClass == null) {
                    continue;
                }
                if (resolvedClass.isInterface()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean returnStatementAfterTypeCheckCodeFragment() {
        //check if the type-check code fragment contains return statements having an expression
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiStatement> allReturnStatementsWithinTypeCheckCodeFragment = statementExtractor.getReturnStatements(getTypeCheckCodeFragment());
        List<PsiReturnStatement> returnStatementsHavingExpressionWithinTypeCheckCodeFragment = new ArrayList<>();
        for (PsiStatement statement : allReturnStatementsWithinTypeCheckCodeFragment) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            if (returnStatement.getReturnValue() != null)
                returnStatementsHavingExpressionWithinTypeCheckCodeFragment.add(returnStatement);
        }
        if (returnStatementsHavingExpressionWithinTypeCheckCodeFragment.isEmpty())
            return false;
        //get all return statements having an expression within method body
        List<PsiStatement> allReturnStatementsWithinTypeCheckMethod = new ArrayList<>();
        PsiCodeBlock psiCodeBlock = getTypeCheckMethod().getBody();
        if (psiCodeBlock != null) {
            for (PsiStatement statementInBlock : psiCodeBlock.getStatements()) {
                allReturnStatementsWithinTypeCheckMethod.addAll(statementExtractor.getReturnStatements(statementInBlock));
            }
        }
        List<PsiReturnStatement> returnStatementsHavingExpressionWithinTypeCheckMethod = new ArrayList<>();
        for (PsiStatement statement : allReturnStatementsWithinTypeCheckMethod) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            if (returnStatement.getReturnValue() != null)
                returnStatementsHavingExpressionWithinTypeCheckMethod.add(returnStatement);
        }
        List<PsiReturnStatement> returnStatementsHavingExpressionOutsideTypeCheckMethod = new ArrayList<>(returnStatementsHavingExpressionWithinTypeCheckMethod);
        returnStatementsHavingExpressionOutsideTypeCheckMethod.removeAll(returnStatementsHavingExpressionWithinTypeCheckCodeFragment);
        for (PsiReturnStatement returnStatement : returnStatementsHavingExpressionOutsideTypeCheckMethod) {
            if (returnStatement.getTextOffset() > getTypeCheckCodeFragment().getTextOffset() + getTypeCheckCodeFragment().getTextLength())
                return true;
        }
        return false;
    }

    private boolean containsLocalVariableAssignment() {
        PsiVariable returnedVariableDeclaration = getTypeCheckMethodReturnedVariable();
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        StatementExtractor statementExtractor = new StatementExtractor();
        List<ArrayList<PsiStatement>> allTypeCheckStatements = getTypeCheckStatements();
        if (!getDefaultCaseStatements().isEmpty()) {
            allTypeCheckStatements.add(getDefaultCaseStatements());
        }
        for (ArrayList<PsiStatement> typeCheckStatementList : allTypeCheckStatements) {
            List<PsiVariable> variableDeclarationFragmentsInsideBranch = new ArrayList<>();
            for (PsiStatement statement : typeCheckStatementList) {
                List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(statement);
                for (PsiStatement statement2 : variableDeclarationStatements) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement2;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    List<PsiVariable> declaredVariables = Arrays.stream(fragments)
                            .map(element -> (PsiVariable) element)
                            .collect(Collectors.toList());
                    variableDeclarationFragmentsInsideBranch.addAll(declaredVariables);
                }
            }
            for (PsiStatement statement : typeCheckStatementList) {
                List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
                for (PsiExpression expression : assignments) {
                    PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                    PsiExpression leftHandSide = assignment.getLExpression();
                    PsiExpression leftHandSideName = null;
                    if (leftHandSide instanceof PsiReferenceExpression) {
                        leftHandSideName = leftHandSide;
                    }
                    if (leftHandSideName != null) {
                        PsiElement resolvedElement = ((PsiReferenceExpression) leftHandSideName).resolve();
                        if (resolvedElement instanceof PsiVariable) {
                            PsiVariable leftHandSideVariableBinding = (PsiVariable) resolvedElement;
                            if (!(leftHandSideVariableBinding instanceof PsiField)) {
                                boolean variableIsDeclaredInsideBranch = false;
                                for (PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
                                    if (fragment.equals(leftHandSideVariableBinding)) {
                                        variableIsDeclaredInsideBranch = true;
                                        break;
                                    }
                                }
                                if (!variableIsDeclaredInsideBranch) {
                                    if (returnedVariableDeclaration == null) {
                                        return true;
                                    } else if (!returnedVariableDeclaration.equals(leftHandSideVariableBinding)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
                for (PsiExpression expression : postfixExpressions) {
                    PsiPostfixExpression postfix = (PsiPostfixExpression) expression;
                    PsiExpression operand = postfix.getOperand();
                    PsiReferenceExpression operandName = null;
                    if (operand instanceof PsiReferenceExpression) {
                        operandName = (PsiReferenceExpression) operand;
                    }
                    if (operandName != null) {
                        PsiElement operandBinding = operandName.resolve();
                        if (operandBinding instanceof PsiVariable) {
                            PsiVariable operandVariableBinding = (PsiVariable) operandBinding;
                            if (!(operandVariableBinding instanceof PsiField)) {
                                boolean variableIsDeclaredInsideBranch = false;
                                for (PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
                                    if (fragment.equals(operandVariableBinding)) {
                                        variableIsDeclaredInsideBranch = true;
                                        break;
                                    }
                                }
                                if (!variableIsDeclaredInsideBranch) {
                                    if (returnedVariableDeclaration == null) {
                                        return true;
                                    } else if (!returnedVariableDeclaration.equals(operandVariableBinding)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
                for (PsiExpression expression : prefixExpressions) {
                    PsiPrefixExpression prefix = (PsiPrefixExpression) expression;
                    IElementType operator = prefix.getOperationTokenType();
                    if (operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
                        PsiExpression operand = prefix.getOperand();
                        PsiReferenceExpression operandName = null;
                        if (operand instanceof PsiReferenceExpression) {
                            operandName = (PsiReferenceExpression) operand;
                        }

                        if (operandName != null) {
                            PsiElement operandBinding = operandName.resolve();
                            if (operandBinding instanceof PsiVariable) {
                                PsiVariable operandVariableBinding = (PsiVariable) operandBinding;
                                if (!(operandVariableBinding instanceof PsiField)) {
                                    boolean variableIsDeclaredInsideBranch = false;
                                    for (PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
                                        if (fragment.equals(operandVariableBinding)) {
                                            variableIsDeclaredInsideBranch = true;
                                            break;
                                        }
                                    }
                                    if (!variableIsDeclaredInsideBranch) {
                                        if (returnedVariableDeclaration == null) {
                                            return true;
                                        } else if (!returnedVariableDeclaration.equals(operandVariableBinding)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean containsBranchingStatement() {
        List<PsiStatement> statementList = new ArrayList<>();
        StatementExtractor statementExtractor = new StatementExtractor();
        List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
        if (!defaultCaseStatements.isEmpty())
            typeCheckStatements.add(defaultCaseStatements);
        for (ArrayList<PsiStatement> statements : typeCheckStatements) {
            for (PsiStatement statement : statements) {
                statementList.addAll(statementExtractor.getBreakStatements(statement));
                statementList.addAll(statementExtractor.getContinueStatements(statement));
                List<PsiStatement> returnStatements = statementExtractor.getReturnStatements(statement);
                for (PsiStatement statement2 : returnStatements) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement2;
                    if (returnStatement.getReturnValue() == null)
                        statementList.add(returnStatement);
                }

                List<PsiStatement> forStatements = statementExtractor.getForStatements(statement);
                for (PsiStatement forStatement : forStatements) {
                    statementList.removeAll(statementExtractor.getBreakStatements(forStatement));
                    statementList.removeAll(statementExtractor.getContinueStatements(forStatement));
                    statementList.removeAll(statementExtractor.getReturnStatements(forStatement));
                }
                List<PsiStatement> whileStatements = statementExtractor.getWhileStatements(statement);
                for (PsiStatement whileStatement : whileStatements) {
                    statementList.removeAll(statementExtractor.getBreakStatements(whileStatement));
                    statementList.removeAll(statementExtractor.getContinueStatements(whileStatement));
                    statementList.removeAll(statementExtractor.getReturnStatements(whileStatement));
                }
                List<PsiStatement> doStatements = statementExtractor.getDoStatements(statement);
                for (PsiStatement doStatement : doStatements) {
                    statementList.removeAll(statementExtractor.getBreakStatements(doStatement));
                    statementList.removeAll(statementExtractor.getContinueStatements(doStatement));
                    statementList.removeAll(statementExtractor.getReturnStatements(doStatement));
                }
                List<PsiStatement> enchancedForStatements = statementExtractor.getEnhancedForStatements(statement);
                for (PsiStatement enchancedForStatement : enchancedForStatements) {
                    statementList.removeAll(statementExtractor.getBreakStatements(enchancedForStatement));
                    statementList.removeAll(statementExtractor.getContinueStatements(enchancedForStatement));
                    statementList.removeAll(statementExtractor.getReturnStatements(enchancedForStatement));
                }
                List<PsiStatement> switchStatements = statementExtractor.getSwitchStatements(statement);
                for (PsiStatement switchStatement : switchStatements) {
                    statementList.removeAll(statementExtractor.getBreakStatements(switchStatement));
                    statementList.removeAll(statementExtractor.getContinueStatements(switchStatement));
                    statementList.removeAll(statementExtractor.getReturnStatements(switchStatement));
                }
            }
        }

        return !statementList.isEmpty();
    }

    private boolean containsSuperMethodInvocation() {
        List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
        if (!defaultCaseStatements.isEmpty())
            typeCheckStatements.add(defaultCaseStatements);

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        for (ArrayList<PsiStatement> statements : typeCheckStatements) {
            for (PsiStatement statement : statements) {
                List<PsiExpression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(statement);
                if (!superMethodInvocations.isEmpty())
                    return true;
            }
        }
        return false;
    }

    private boolean containsSuperFieldAccess() {
        List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
        if (!defaultCaseStatements.isEmpty())
            typeCheckStatements.add(defaultCaseStatements);

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        for (ArrayList<PsiStatement> statements : typeCheckStatements) {
            for (PsiStatement statement : statements) {
                List<PsiExpression> superFieldAccesses = expressionExtractor.getSuperFieldAccesses(statement);
                if (!superFieldAccesses.isEmpty())
                    return true;
            }
        }
        return false;
    }

    public PsiType getTypeCheckMethodReturnType() {
        return getTypeCheckMethod().getReturnType();
    }

    public PsiParameter[] getTypeCheckMethodParameters() {
        return getTypeCheckMethod().getParameterList().getParameters();
    }

    private Map<PsiReturnStatement, PsiVariable> getTypeCheckMethodReturnedVariableMap() {
        Map<PsiReturnStatement, PsiVariable> map = new LinkedHashMap<>();
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiStatement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(getTypeCheckCodeFragment());
        List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(getTypeCheckMethod().getBody());
        for (PsiStatement statement : typeCheckCodeFragmentReturnStatements) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            if (returnStatement.getReturnValue() instanceof PsiReferenceExpression) {
                PsiReferenceExpression returnExpression = (PsiReferenceExpression) returnStatement.getReturnValue();
                PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                for (PsiVariable parameter : parameters) {
                    if (parameter.equals(returnExpression.resolve())) {
                        map.put(returnStatement, parameter);
                    }
                }
                for (PsiStatement vdStatement : variableDeclarationStatements) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) vdStatement;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    for (PsiElement fragment : fragments) {
                        if (fragment.equals(returnExpression.resolve())) {
                            map.put(returnStatement, (PsiVariable) fragment);
                        }
                    }
                }
            }
        }
        return map;
    }

    public boolean returnedVariableReturnedInBranches() {
        Map<PsiReturnStatement, PsiVariable> map = getTypeCheckMethodReturnedVariableMap();
        int returnedInBranchCounter = 0;
        for (PsiReturnStatement key : map.keySet()) {
            for (PsiExpression expression : typeCheckMap.keySet()) {
                ArrayList<PsiStatement> branchStatements = typeCheckMap.get(expression);
                if (branchStatements.contains(key)) {
                    returnedInBranchCounter++;
                }
            }
            if (defaultCaseStatements.contains(key)) {
                returnedInBranchCounter++;
            }
        }
        return map.size() == returnedInBranchCounter;
    }

    public boolean returnedVariableDeclaredAndReturnedInBranches() {
        Map<PsiReturnStatement, PsiVariable> map = getTypeCheckMethodReturnedVariableMap();
        int returnedInBranchCounter = 0;
        int declaredInBranchCounter = 0;
        for (PsiReturnStatement key : map.keySet()) {
            for (PsiExpression expression : typeCheckMap.keySet()) {
                ArrayList<PsiStatement> branchStatements = typeCheckMap.get(expression);
                if (branchStatements.contains(key)) {
                    returnedInBranchCounter++;
                }
                for (PsiStatement statement : branchStatements) {
                    if (statement instanceof PsiDeclarationStatement) {
                        PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                        PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                        for (PsiElement fragment : fragments) {
                            if (fragment.equals(map.get(key))) {
                                declaredInBranchCounter++;
                            }
                        }
                    }
                }
            }
            if (defaultCaseStatements.contains(key)) {
                returnedInBranchCounter++;
            }
            for (PsiStatement statement : defaultCaseStatements) {
                if (statement instanceof PsiDeclarationStatement) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    for (PsiElement fragment : fragments) {
                        if (fragment.equals(map.get(key))) {
                            declaredInBranchCounter++;
                        }
                    }
                }
            }
        }
        return map.size() == returnedInBranchCounter && map.size() == declaredInBranchCounter;
    }

    public PsiVariable getTypeCheckMethodReturnedVariable() {
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiStatement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(getTypeCheckCodeFragment());
        if (!typeCheckCodeFragmentReturnStatements.isEmpty()) {
            PsiReturnStatement firstReturnStatement = (PsiReturnStatement) typeCheckCodeFragmentReturnStatements.get(0);
            if (firstReturnStatement.getReturnValue() instanceof PsiReferenceExpression) {
                PsiElement resolvedReference = ((PsiReferenceExpression) firstReturnStatement.getReturnValue()).resolve();
                PsiParameter[] parameters = getTypeCheckMethod().getParameterList().getParameters();
                for (PsiParameter parameter : parameters) {
                    if (parameter.equals(resolvedReference))
                        return parameter;
                }
                List<PsiStatement> variableDeclarationStatements = new ArrayList<>();
                if (getTypeCheckMethod().getBody() != null) {
                    PsiStatement[] psiStatements = getTypeCheckMethod().getBody().getStatements();
                    for (PsiStatement statementInBlock : psiStatements) {
                        variableDeclarationStatements.addAll(statementExtractor.getVariableDeclarationStatements(statementInBlock));
                    }
                }
                for (PsiStatement statement : variableDeclarationStatements) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    for (PsiElement fragment : fragments) {
                        if (fragment.equals(resolvedReference))
                            return (PsiVariable) fragment;
                    }
                }
            }
        } else {
            List<PsiStatement> allReturnStatements = new ArrayList<>();
            if (getTypeCheckMethod().getBody() != null) {
                PsiStatement[] psiStatements = getTypeCheckMethod().getBody().getStatements();
                for (PsiStatement statementInBlock : psiStatements) {
                    allReturnStatements.addAll(statementExtractor.getReturnStatements(statementInBlock));
                }
            }
            if (!allReturnStatements.isEmpty()) {
                PsiReturnStatement lastReturnStatement = (PsiReturnStatement) allReturnStatements.get(allReturnStatements.size() - 1);
                if (lastReturnStatement.getReturnValue() instanceof PsiReferenceExpression) {
                    PsiReferenceExpression referenceExpression = (PsiReferenceExpression) lastReturnStatement.getReturnValue();
                    for (PsiParameter assignedParameter : assignedParameters) {
                        if (assignedParameter.equals(referenceExpression.resolve()))
                            return assignedParameter;
                    }
                    for (PsiVariable assignedLocalVariable : assignedLocalVariables) {
                        if (assignedLocalVariable.equals(referenceExpression.resolve()))
                            return assignedLocalVariable;
                    }
                }
            }
        }
        return null;
    }

    public String getTypeVariableSimpleName() {
        if (getTypeField() != null) {
            return getTypeField().getName();
        } else if (getTypeLocalVariable() != null) {
            return getTypeLocalVariable().getName();
        } else if (getForeignTypeField() != null) {
            return getForeignTypeField().getName();
        } else if (getTypeMethodInvocation() != null) {
            PsiExpression typeMethodInvocationExpression = getTypeMethodInvocation().getMethodExpression().getQualifierExpression();
            PsiReferenceExpression invoker = null;
            if (typeMethodInvocationExpression instanceof PsiReferenceExpression) {
                invoker = (PsiReferenceExpression) typeMethodInvocationExpression;
            }
            if (invoker != null) {
                return invoker.getReferenceName();
            } else {
                PsiMethod resolvedMethod = getTypeMethodInvocation().resolveMethod();
                if (resolvedMethod != null)
                    return resolvedMethod.getName();
            }
        }
        return getAbstractClassName();
    }

    public String getAbstractClassName() {
        if (getTypeField() != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
            String typeFieldName = getTypeField().getName().replaceAll("_", "");
            return typeFieldName.substring(0, 1).toUpperCase() + typeFieldName.substring(1);
        } else if (getTypeLocalVariable() != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
            String typeLocalVariableName = getTypeLocalVariable().getName().replaceAll("_", "");
            return typeLocalVariableName.substring(0, 1).toUpperCase() + typeLocalVariableName.substring(1);
        } else if (getForeignTypeField() != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
            String foreignTypeFieldName = getForeignTypeField().getName().replaceAll("_", "");
            return foreignTypeFieldName.substring(0, 1).toUpperCase() + foreignTypeFieldName.substring(1);
        } else if (existingInheritanceTree != null) {
            DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
            return (String) root.getUserObject();
        } else if (inheritanceTreeMatchingWithStaticTypes != null) {
            DefaultMutableTreeNode root = inheritanceTreeMatchingWithStaticTypes.getRootNode();
            String rootClassName = (String) root.getUserObject();
            if (rootClassName.contains("."))
                return rootClassName.substring(rootClassName.lastIndexOf(".") + 1);
            else
                return rootClassName;
        }
        return null;
    }

    public String getAbstractClassType() {
        String abstractClassType = null;
        if (getTypeField() != null) {
            PsiType fieldType = getTypeField().getType();
            if (fieldType instanceof PsiClassType) {
                abstractClassType = fieldType.getCanonicalText();
            }
        } else if (getTypeLocalVariable() != null) {
            PsiType fieldType = getTypeLocalVariable().getType();
            if (fieldType instanceof PsiClassType) {
                abstractClassType = fieldType.getCanonicalText();
            }
        } else if (getForeignTypeField() != null) {
            PsiType fieldType = getForeignTypeField().getType();
            if (fieldType instanceof PsiClassType) {
                abstractClassType = fieldType.getCanonicalText();
            }
        } else if (getTypeMethodInvocation() != null) {
            PsiExpression typeMethodInvocationExpression = getTypeMethodInvocation().getMethodExpression().getQualifierExpression();
            PsiReferenceExpression invoker = null;
            if (typeMethodInvocationExpression instanceof PsiReferenceExpression) {
                invoker = (PsiReferenceExpression) typeMethodInvocationExpression;
            }
            if (invoker != null && invoker.getType() != null) {
                abstractClassType = invoker.getType().getCanonicalText();
            }
        }

        if (abstractClassType == null) {
            PsiCodeBlock typeCheckMethodBody = getTypeCheckMethod().getBody();
            if (typeCheckMethodBody != null) {
                PsiStatement[] statements = typeCheckMethodBody.getStatements();
                if (statements.length > 0 && statements[0] instanceof PsiSwitchStatement) {
                    PsiSwitchStatement switchStatement = (PsiSwitchStatement) statements[0];
                    PsiCodeBlock switchStatementBody = switchStatement.getBody();
                    if (switchStatementBody != null) {
                        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
                        List<PsiClass> superClasses = new ArrayList<>();
                        for (PsiStatement psiStatement : switchStatementBody.getStatements()) {
                            if (!(psiStatement instanceof PsiSwitchLabelStatement) && !(psiStatement instanceof PsiBreakStatement)) {
                                List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(psiStatement);
                                if (classInstanceCreations.size() == 1) {
                                    PsiNewExpression classInstanceCreation = (PsiNewExpression) classInstanceCreations.get(0);
                                    PsiJavaCodeReferenceElement classInstanceCreationType = classInstanceCreation.getClassReference();
                                    if (classInstanceCreationType != null) {
                                        PsiElement resolvedReference = classInstanceCreationType.resolve();
                                        if (resolvedReference instanceof PsiClass)
                                            superClasses.add(((PsiClass) resolvedReference).getSuperClass());
                                    }
                                }
                            }
                        }
                        if (superClasses.size() > 1) {
                            for (PsiClass psiClass : superClasses) {
                                if ("java.lang.Object".equals(psiClass.getQualifiedName()))
                                    return null;
                            }
                            if (equalTypeBindings(superClasses)) {
                                abstractClassType = superClasses.get(0).getQualifiedName();
                            } else {
                                List<PsiClass> psiClasses = new ArrayList<>();
                                for (PsiClass psiClass : superClasses) {
                                    PsiClass superClass = psiClass.getSuperClass();
                                    if (superClass != null && "java.lang.Object".equals(superClass.getQualifiedName()))
                                        psiClasses.add(psiClass);
                                    else
                                        psiClasses.add(superClass);
                                }
                                if (equalTypeBindings(psiClasses)) {
                                    abstractClassType = superClasses.get(0).getQualifiedName();
                                }
                            }
                        }
                    }
                }
            }
        }
        return abstractClassType;
    }

    private boolean equalTypeBindings(List<PsiClass> typeBindings) {
        PsiClass firstTypeBinding = typeBindings.get(0);
        for (int i = 1; i < typeBindings.size(); i++) {
            PsiClass currentTypeBinding = typeBindings.get(i);
            if (!firstTypeBinding.equals(currentTypeBinding)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getSubclassNames() {
        List<String> subclassNames = new ArrayList<>();
        for (PsiExpression expression : typeCheckMap.keySet()) {
            List<PsiField> simpleNameGroup = staticFieldMap.get(expression);
            if (simpleNameGroup != null) {
                for (PsiField simpleName : simpleNameGroup) {
                    String staticFieldName = simpleName.getName();
                    PsiType castingType = getCastingType(typeCheckMap.get(expression));
                    StringBuilder subclassName;
                    if (!staticFieldName.contains("_")) {
                        subclassName = new StringBuilder(staticFieldName.substring(0, 1).toUpperCase() +
                                staticFieldName.substring(1).toLowerCase());
                    } else {
                        subclassName = new StringBuilder();
                        StringTokenizer tokenizer = new StringTokenizer(staticFieldName, "_");
                        while (tokenizer.hasMoreTokens()) {
                            String tempName = tokenizer.nextToken().toLowerCase();
                            subclassName.append(tempName.subSequence(0, 1).toString().toUpperCase())
                                    .append(tempName.subSequence(1, tempName.length()).toString());
                        }
                    }
                    if (inheritanceTreeMatchingWithStaticTypes != null) {
                        subclassNames.add(staticFieldSubclassTypeMap.get(simpleName));
                    } else if (existingInheritanceTree != null) {
                        DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
                        DefaultMutableTreeNode leaf = root.getFirstLeaf();
                        while (leaf != null) {
                            String childClassName = (String) leaf.getUserObject();
                            if (childClassName.endsWith(subclassName.toString())) {
                                subclassNames.add(childClassName);
                                break;
                            } else if (castingType != null && castingType.getCanonicalText().equals(childClassName)) {
                                subclassNames.add(childClassName);
                                break;
                            }
                            leaf = leaf.getNextLeaf();
                        }
                    } else if (castingType != null) {
                        subclassNames.add(castingType.getCanonicalText());
                    } else {
                        subclassNames.add(subclassName.toString());
                    }
                }
            }
            List<PsiType> typeGroup = subclassTypeMap.get(expression);
            if (typeGroup != null) {
                for (PsiType type : typeGroup)
                    subclassNames.add(type.getCanonicalText());
            }
        }
        return subclassNames;
    }

    private PsiType getCastingType(ArrayList<PsiStatement> typeCheckCodeFragment) {
        List<PsiExpression> castExpressions = new ArrayList<>();
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        for (PsiStatement statement : typeCheckCodeFragment) {
            castExpressions.addAll(expressionExtractor.getCastExpressions(statement));
        }
        for (PsiExpression expression : castExpressions) {
            PsiTypeCastExpression castExpression = (PsiTypeCastExpression) expression;
            PsiExpression expressionOfCastExpression = castExpression.getOperand();
            PsiReferenceExpression psiReferenceExpression = null;
            if (expressionOfCastExpression instanceof PsiReferenceExpression) {
                psiReferenceExpression = (PsiReferenceExpression) expressionOfCastExpression;
            } else if (expressionOfCastExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expressionOfCastExpression;
                if (getTypeFieldGetterMethod() != null && getTypeFieldGetterMethod().equals(methodInvocation.resolveMethod())) {
                    psiReferenceExpression = (PsiReferenceExpression) MethodDeclarationUtility.isGetter(getTypeFieldGetterMethod());
                }
            }
            if (psiReferenceExpression != null) {
                if (getTypeField() != null) {
                    if (getTypeField().equals(psiReferenceExpression.resolve()))
                        return castExpression.getType();
                } else if (getTypeLocalVariable() != null) {
                    if (getTypeLocalVariable().equals(psiReferenceExpression.resolve()))
                        return castExpression.getType();
                } else if (getTypeMethodInvocation() != null) {
                    PsiExpression typeMethodInvocationExpression = getTypeMethodInvocation().getMethodExpression().getQualifierExpression();
                    PsiReferenceExpression invoker = null;
                    if (typeMethodInvocationExpression instanceof PsiReferenceExpression) {
                        invoker = (PsiReferenceExpression) typeMethodInvocationExpression;
                    }
                    if (invoker != null) {
                        PsiElement resolvedElement = invoker.resolve();
                        if (resolvedElement != null && resolvedElement.equals(psiReferenceExpression.resolve()))
                            return castExpression.getType();
                    }
                }
            }
        }
        return null;
    }

    public Set<PsiClassType> getThrownExceptions() {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        StatementExtractor statementExtractor = new StatementExtractor();
        Set<PsiClassType> thrownExceptions = new LinkedHashSet<>();
        for (PsiExpression key : typeCheckMap.keySet()) {
            ArrayList<PsiStatement> statements = typeCheckMap.get(key);
            for (PsiStatement typeCheckStatement : statements) {
                List<PsiExpression> methodInvocations = expressionExtractor.getMethodInvocations(typeCheckStatement);
                List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(typeCheckStatement);
                List<PsiStatement> tryStatements = statementExtractor.getTryStatements(typeCheckStatement);
                Set<PsiType> catchClauseExceptions = new LinkedHashSet<>();
                for (PsiStatement statement : tryStatements) {
                    PsiTryStatement tryStatement = (PsiTryStatement) statement;
                    PsiCatchSection[] catchClauses = tryStatement.getCatchSections();
                    for (PsiCatchSection catchClause : catchClauses) {
                        PsiType exceptionType = catchClause.getCatchType();
                        if (exceptionType instanceof PsiDisjunctionType) {
                            catchClauseExceptions.addAll(((PsiDisjunctionType) exceptionType).getDisjunctions());
                        } else {
                            catchClauseExceptions.add(exceptionType);
                        }
                    }
                }
                for (PsiExpression expression : methodInvocations) {
                    if (expression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                        PsiMethod resolvedMethod = methodInvocation.resolveMethod();
                        if (resolvedMethod != null) {
                            PsiClassType[] referencedTypes = resolvedMethod.getThrowsList().getReferencedTypes();
                            for (PsiClassType psiClassType : referencedTypes) {
                                if (!catchClauseExceptions.contains(psiClassType))
                                    thrownExceptions.add(psiClassType);
                            }
                        }
                    }
                }
                for (PsiExpression expression : classInstanceCreations) {
                    PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
                    PsiMethod methodBinding = classInstanceCreation.resolveMethod();
                    if (methodBinding == null) { // in case of implicit constructor
                        continue;
                    }
                    PsiClassType[] typeBindings = methodBinding.getThrowsList().getReferencedTypes();
                    for (PsiClassType typeBinding : typeBindings) {
                        if (!catchClauseExceptions.contains(typeBinding))
                            thrownExceptions.add(typeBinding);
                    }
                }
            }
        }
        return thrownExceptions;
    }

    public boolean allTypeCheckBranchesAreEmpty() {
        for (PsiExpression key : typeCheckMap.keySet()) {
            ArrayList<PsiStatement> statements = typeCheckMap.get(key);
            if (!statements.isEmpty())
                return false;
        }
        return true;
    }

    public boolean isTypeCheckMethodStateSetter() {
        InheritanceTree tree = null;
        if (existingInheritanceTree != null)
            tree = existingInheritanceTree;
        else if (inheritanceTreeMatchingWithStaticTypes != null)
            tree = inheritanceTreeMatchingWithStaticTypes;
        if (tree != null) {
            DefaultMutableTreeNode root = tree.getRootNode();
            DefaultMutableTreeNode leaf = root.getFirstLeaf();
            List<String> subclassNames = new ArrayList<>();
            while (leaf != null) {
                subclassNames.add((String) leaf.getUserObject());
                leaf = leaf.getNextLeaf();
            }
            PsiCodeBlock typeCheckMethodBody = getTypeCheckMethod().getBody();
            if (typeCheckMethodBody != null && typeCheckMethodBody.getStatements().length > 0
                    && typeCheckMethodBody.getStatements()[0] instanceof PsiSwitchStatement) {
                PsiStatement[] statements = typeCheckMethodBody.getStatements();
                PsiSwitchStatement switchStatement = (PsiSwitchStatement) statements[0];
                PsiCodeBlock switchStatementBody = switchStatement.getBody();
                if (switchStatementBody != null) {
                    ExpressionExtractor expressionExtractor = new ExpressionExtractor();
                    int matchCounter = 0;
                    for (PsiStatement psiStatement : switchStatementBody.getStatements()) {
                        if (!(psiStatement instanceof PsiSwitchLabelStatement) && !(psiStatement instanceof PsiBreakStatement)) {
                            List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(psiStatement);
                            if (classInstanceCreations.size() == 1) {
                                PsiNewExpression classInstanceCreation = (PsiNewExpression) classInstanceCreations.get(0);
                                if (classInstanceCreation.getClassReference() != null) {
                                    String classInstanceCreationType = classInstanceCreation.getClassReference().getQualifiedName();
                                    if (subclassNames.contains(classInstanceCreationType)) {
                                        matchCounter++;
                                    }
                                }
                            }
                        }
                    }
                    return matchCounter == subclassNames.size();
                }
            }
        }
        return false;
    }

    public boolean typeCheckCodeFragmentContainsReturnStatement() {
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiStatement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(getTypeCheckCodeFragment());
        return !typeCheckCodeFragmentReturnStatements.isEmpty();
    }

    public String getAbstractMethodName() {
        return abstractMethodName;
    }

    public void setAbstractMethodName(String methodName) {
        abstractMethodName = methodName;
    }

    public double getAverageNumberOfStatements() {
        if (averageNumberOfStatements == 0) {
            List<ArrayList<PsiStatement>> typeCheckStatements = new ArrayList<>(getTypeCheckStatements());
            ArrayList<PsiStatement> defaultCaseStatements = getDefaultCaseStatements();
            if (!defaultCaseStatements.isEmpty())
                typeCheckStatements.add(defaultCaseStatements);
            StatementExtractor statementExtractor = new StatementExtractor();
            int numberOfCases = typeCheckStatements.size();
            int totalNumberOfStatements = 0;
            for (ArrayList<PsiStatement> statements : typeCheckStatements) {
                for (PsiStatement statement : statements) {
                    totalNumberOfStatements += statementExtractor.getTotalNumberOfStatements(statement);
                }
            }
            averageNumberOfStatements = (double) totalNumberOfStatements / (double) numberOfCases;
        }
        return averageNumberOfStatements;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof TypeCheckElimination) {
            TypeCheckElimination typeCheckElimination = (TypeCheckElimination) o;
            return getTypeCheckClass().equals(typeCheckElimination.getTypeCheckClass()) &&
                    getTypeCheckMethod().equals(typeCheckElimination.getTypeCheckMethod()) &&
                    getTypeCheckCodeFragment().equals(typeCheckElimination.getTypeCheckCodeFragment());
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + getTypeCheckClass().hashCode();
            result = 37 * result + getTypeCheckMethod().hashCode();
            result = 37 * result + getTypeCheckCodeFragment().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return PsiUtils.calculateSignature(getTypeCheckMethod());
    }

    public int getGroupSizeAtClassLevel() {
        return groupSizeAtClassLevel;
    }

    public void setGroupSizeAtClassLevel(int groupSizeAtClassLevel) {
        this.groupSizeAtClassLevel = groupSizeAtClassLevel;
    }

    public int compareTo(TypeCheckElimination other) {
        int groupSizeAtClassLevel1 = this.getGroupSizeAtClassLevel();
        int groupSizeAtClassLevel2 = other.getGroupSizeAtClassLevel();
        double averageNumberOfStatements1 = this.getAverageNumberOfStatements();
        double averageNumberOfStatements2 = other.getAverageNumberOfStatements();
        String refactoringName1 = this.toString();
        String refactoringName2 = other.toString();

        if (groupSizeAtClassLevel1 > groupSizeAtClassLevel2)
            return -1;
        else if (groupSizeAtClassLevel1 < groupSizeAtClassLevel2)
            return 1;

        if (averageNumberOfStatements1 > averageNumberOfStatements2)
            return -1;
        else if (averageNumberOfStatements1 < averageNumberOfStatements2)
            return 1;

        return refactoringName1.compareTo(refactoringName2);
    }

    public int getTotalCaseStatementsCount() {
        return typeCheckMap.size();
    }
}
