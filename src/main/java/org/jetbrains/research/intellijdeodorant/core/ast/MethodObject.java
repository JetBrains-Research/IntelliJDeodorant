package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.research.intellijdeodorant.core.ast.association.Association;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.StatementObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class MethodObject implements AbstractMethodDeclaration {
    private TypeObject returnType;
    private boolean _abstract;
    private boolean _static;
    private boolean _synchronized;
    private boolean _native;
    private final ConstructorObject constructorObject;
    private boolean testAnnotation;
    private volatile int hashCode = 0;
    private final SmartPsiElementPointer<PsiElement> psiMethod;

    public MethodObject(PsiMethod psiMethod, ConstructorObject co) {
        this.constructorObject = co;
        this._abstract = false;
        this._static = false;
        this._synchronized = false;
        this._native = false;
        this.testAnnotation = false;
        this.psiMethod = toPointer(psiMethod);
    }

    private PsiMethod getPsiMethod() {
        return (PsiMethod) this.psiMethod.getElement();
    }

    public void setReturnType(TypeObject returnType) {
        this.returnType = returnType;
    }

    public TypeObject getReturnType() {
        return returnType;
    }

    public void setAbstract(boolean abstr) {
        this._abstract = abstr;
    }

    public boolean isAbstract() {
        return this._abstract;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean isSynchronized() {
        return this._synchronized;
    }

    public void setSynchronized(boolean s) {
        this._synchronized = s;
    }

    public boolean isNative() {
        return this._native;
    }

    public void setNative(boolean n) {
        this._native = n;
    }

    public String getName() {
        return constructorObject.getName();
    }

    boolean hasTestAnnotation() {
        return testAnnotation;
    }

    public void setTestAnnotation(boolean testAnnotation) {
        this.testAnnotation = testAnnotation;
    }

    public Set<String> getExceptionsInJavaDocThrows() {
        return constructorObject.getExceptionsInJavaDocThrows();
    }

    public Access getAccess() {
        return constructorObject.getAccess();
    }

    public PsiMethod getMethodDeclaration() {
        return constructorObject.getMethodDeclaration();
    }

    public MethodBodyObject getMethodBody() {
        return constructorObject.getMethodBody();
    }

    public MethodInvocationObject generateMethodInvocation() {
        return new MethodInvocationObject(TypeObject.extractTypeObject(this.constructorObject.className),
                this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public SuperMethodInvocationObject generateSuperMethodInvocation() {
        return new SuperMethodInvocationObject(TypeObject.extractTypeObject(this.constructorObject.className),
                this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public FieldInstructionObject isGetter() {
        if (getMethodBody() != null) {
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                PsiElement statement = statementObject.getStatement();
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    if ((returnStatement.getReturnValue() instanceof PsiReferenceExpression)
                            && statementObject.getFieldInstructions().size() == 1
                            && statementObject.getMethodInvocations().size() == 0
                            && statementObject.getLocalVariableDeclarations().size() == 0
                            && statementObject.getLocalVariableInstructions().size() == 0
                            && this.constructorObject.parameterList.size() == 0) {
                        return statementObject.getFieldInstructions().get(0);
                    }
                }
            }
        }
        return null;
    }

    public FieldInstructionObject isSetter() {
        if (getMethodBody() != null) {
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                PsiElement statement = statementObject.getStatement();
                if (statement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
                    if (expressionStatement.getExpression() instanceof PsiAssignmentExpression
                            && statementObject.getFieldInstructions().size() == 1
                            && statementObject.getMethodInvocations().size() == 0
                            && statementObject.getLocalVariableDeclarations().size() == 0
                            && statementObject.getLocalVariableInstructions().size() == 1
                            && this.constructorObject.parameterList.size() == 1) {
                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) expressionStatement.getExpression();
                        if (assignment.getLExpression() instanceof PsiReferenceExpression
                                && assignment.getOperationSign().getTokenType() == JavaTokenType.EQ)
                            return statementObject.getFieldInstructions().get(0);
                    }
                }
            }
        }
        return null;
    }

    public FieldInstructionObject isCollectionAdder() {
        if (getMethodBody() != null) {
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                if (statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 1
                        && statementObject.getLocalVariableDeclarations().size() == 0
                        && statementObject.getLocalVariableInstructions().size() == 1
                        && this.constructorObject.parameterList.size() == 1) {
                    String methodName = statementObject.getMethodInvocations().get(0).getMethodName();
                    String originClassName = statementObject.getMethodInvocations().get(0).getOriginClassName();
                    List<String> acceptableOriginClassNames = new ArrayList<>();
                    acceptableOriginClassNames.add("java.util.Collection");
                    acceptableOriginClassNames.add("java.util.AbstractCollection");
                    acceptableOriginClassNames.add("java.util.List");
                    acceptableOriginClassNames.add("java.util.AbstractList");
                    acceptableOriginClassNames.add("java.util.ArrayList");
                    acceptableOriginClassNames.add("java.util.LinkedList");
                    acceptableOriginClassNames.add("java.util.Set");
                    acceptableOriginClassNames.add("java.util.AbstractSet");
                    acceptableOriginClassNames.add("java.util.HashSet");
                    acceptableOriginClassNames.add("java.util.LinkedHashSet");
                    acceptableOriginClassNames.add("java.util.SortedSet");
                    acceptableOriginClassNames.add("java.util.TreeSet");
                    acceptableOriginClassNames.add("java.util.Vector");
                    if (methodName.equals("add") || methodName.equals("addElement") || methodName.equals("addAll")) {
                        if (acceptableOriginClassNames.contains(originClassName))
                            return statementObject.getFieldInstructions().get(0);
                    }
                }
            }
        }
        return null;
    }

    public MethodInvocationObject isDelegate() {
        if (getMethodBody() != null) {
            PsiMethod methodDeclaration = getMethodDeclaration();
            PsiClass parentClass = methodDeclaration.getContainingClass();
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                PsiElement statement = statementObject.getStatement();
                PsiMethodCallExpression methodInvocation = null;
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    if (returnStatement.getReturnValue() instanceof PsiMethodCallExpression) {
                        methodInvocation = (PsiMethodCallExpression) returnStatement.getReturnValue();
                    }
                } else if (statement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
                    if (expressionStatement.getExpression() instanceof PsiMethodCallExpression) {
                        methodInvocation = (PsiMethodCallExpression) expressionStatement.getExpression();
                    }
                }
                if (methodInvocation != null) {
                    PsiExpression methodQualifierExpression = methodInvocation.getMethodExpression().getQualifierExpression();
                    List<MethodInvocationObject> methodInvocations = statementObject.getMethodInvocations();

                    PsiElement resolvedElement = null;
                    if (methodQualifierExpression instanceof PsiReferenceExpression) {
                        PsiReferenceExpression reference = (PsiReferenceExpression) methodQualifierExpression;
                        resolvedElement = reference.resolve();
                    }

                    if (resolvedElement instanceof PsiMethod) {
                        PsiMethod previousChainedMethodInvocation = (PsiMethod) resolvedElement;
                        List<PsiMethod> parentClassMethods = new ArrayList<>();
                        if (parentClass != null) {
                            parentClassMethods.addAll(Arrays.asList(parentClass.getMethods()));
                        }
                        boolean isDelegationChain = false;
                        boolean foundInParentClass = false;
                        for (PsiMethod parentClassMethod : parentClassMethods) {
                            if (PsiManager.getInstance(psiMethod.getProject())
                                    .areElementsEquivalent(parentClassMethod, previousChainedMethodInvocation)) {
                                foundInParentClass = true;
                                PsiElement getterField = MethodDeclarationUtility.isGetter(parentClassMethod);
                                if (getterField == null)
                                    isDelegationChain = true;
                                break;
                            }
                        }
                        if (!isDelegationChain && foundInParentClass) {
                            for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                                if (methodQualifierExpression.equals(methodInvocation.getMethodExpression())) {
                                    return methodInvocationObject;
                                }
                            }
                        }
                    } else if (resolvedElement instanceof PsiField) {
                        PsiField resolvedField = (PsiField) resolvedElement;

                        if (parentClass != null && resolvedField.getContainingClass() != null) {
                            if (parentClass.equals(resolvedField.getContainingClass()) ||
                                    parentClass.isInheritor(resolvedField.getContainingClass(), true)) {
                                for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                                    if (methodInvocation.equals(methodInvocationObject.getMethodInvocation())) {
                                        return methodInvocationObject;
                                    }
                                }
                            }
                        }
                    } else if (resolvedElement != null) {
                        if (resolvedElement instanceof PsiParameter) {
                            for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                                if (methodInvocation.equals(methodInvocationObject.getMethodInvocation())) {
                                    return methodInvocationObject;
                                }
                            }
                        }
                    } else if (methodQualifierExpression instanceof PsiThisExpression) {
                        for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                            if (methodInvocation.equals(methodInvocationObject.getMethodInvocation())) {
                                return methodInvocationObject;
                            }
                        }
                    } else if (methodQualifierExpression == null) {
                        for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                            if (methodInvocation.equals(methodInvocationObject.getMethodInvocation())) {
                                return methodInvocationObject;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean validTargetObject(ClassObject sourceClass, ClassObject targetClass) {
        String targetClassType = targetClass.getPsiType();
        List<LocalVariableInstructionObject> localVariableInstructions = getLocalVariableInstructions();
        for (LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
            if (localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
                for (LocalVariableDeclarationObject variableDeclaration : getLocalVariableDeclarations()) {
                    if (variableDeclaration.getVariableDeclaration().equals(localVariableInstruction.getReference().resolve()))
                        return false;
                }
            }
        }

        List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
        if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
            StatementObject statementObject = (StatementObject) abstractStatements.get(0);
            PsiElement statement = statementObject.getStatement();
            if (statement instanceof PsiReturnStatement) {
                PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                if (returnStatement.getReturnValue() instanceof PsiMethodCallExpression)
                    return false;
            }
        }

        for (LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
            if (localVariableInstruction.getType().getClassType().equals(targetClass.getPsiClass().getName())) {
                ListIterator<ParameterObject> parameterIterator = getParameterListIterator();
                while (parameterIterator.hasNext()) {
                    ParameterObject parameter = parameterIterator.next();
                    if (localVariableInstruction.getName().equals(parameter.getName()) && parameter.getType().getArrayDimension() == 0)
                        return true;
                }
            }
        }

        Collection<PsiReferenceExpression> fieldAccessed = PsiTreeUtil.findChildrenOfType(getPsiMethod(), PsiReferenceExpression.class);
        for (PsiReferenceExpression referenceExpression : fieldAccessed) {
            if (!(referenceExpression.resolve() instanceof PsiField)) continue;
            PsiField psiField = (PsiField) referenceExpression.resolve();
            if (psiField != null && psiField.getType().getCanonicalText().equals(targetClass.getPsiClass().getName())) {
                return true;
            }
        }

        PsiParameter[] parameters = getPsiMethod().getParameterList().getParameters();
        for (PsiParameter psiParameter : parameters) {
            if (psiParameter.getType().getCanonicalText().equals(targetClass.getPsiClass().getQualifiedName())) {
                return true;
            }
        }

        Collection<PsiField> elements = PsiTreeUtil.findChildrenOfType(getPsiMethod(), PsiField.class);
        for (PsiField field : elements) {
            if (field.getType().getCanonicalText().equals(targetClass.getPsiType())) {
                return true;
            }
        }

        List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
        for (FieldInstructionObject fieldInstruction : fieldInstructions) {
            String fieldTypeBinding = fieldInstruction.getType().getClassType();
            if (fieldTypeBinding != null && fieldInstruction.getType() != null &&
                    fieldInstruction.getType().getClassType() != null && fieldInstruction.getType().getClassType().equals(targetClass.getName())
                    || fieldTypeBinding != null && fieldTypeBinding.equals(targetClassType)) {
                ListIterator<FieldObject> fieldIterator = sourceClass.getFieldIterator();
                while (fieldIterator.hasNext()) {
                    FieldObject field = fieldIterator.next();
                    if (fieldInstruction.getName().equals(field.getName()) && field.getType().getArrayDimension() == 0)
                        return true;
                }
            }
        }

        List<MethodInvocationObject> methodInvocations = getMethodInvocations();
        for (MethodInvocationObject methodInvocation : methodInvocations) {
            if (methodInvocation.getOriginClassName().equals(sourceClass.getName())) {
                MethodObject invokedMethod = sourceClass.getMethod(methodInvocation);
                if (invokedMethod == null) continue;
                FieldInstructionObject fieldInstruction = invokedMethod.isGetter();
                if (fieldInstruction != null && fieldInstruction.getType().getClassType().equals(targetClass.getName()))
                    return true;
                MethodInvocationObject delegation = invokedMethod.isDelegate();
                if (delegation != null && delegation.getOriginClassName().equals(targetClass.getName()))
                    return true;
            }
        }
        return false;
    }

    public boolean oneToManyRelationshipWithTargetClass(List<Association> associations, ClassObject targetClass) {
        List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
        for (Association association : associations) {
            FieldObject fieldObject = association.getFieldObject();
            for (FieldInstructionObject fieldInstruction : fieldInstructions) {
                if (fieldObject.equals(fieldInstruction) && targetClass.getName().equals(association.getTo()) &&
                        association.isContainer())
                    return true;
            }
        }
        return false;
    }

    public boolean containsMethodCallWithThisExpressionAsArgument() {
        List<MethodInvocationObject> methodInvocations = getMethodInvocations();
        for (MethodInvocationObject methodInvocation : methodInvocations) {
            PsiMethodCallExpression invocation = methodInvocation.getMethodInvocation();
            PsiExpression[] arguments = invocation.getArgumentList().getExpressions();
            for (PsiExpression argument : arguments) {
                if (argument instanceof PsiThisExpression)
                    return true;
            }
        }
        List<CreationObject> creations = getCreations();
        for (CreationObject creation : creations) {
            if (creation instanceof ClassInstanceCreationObject) {
                ClassInstanceCreationObject classInstanceCreationObject = (ClassInstanceCreationObject) creation;
                PsiNewExpression classInstanceCreation = classInstanceCreationObject.getClassInstanceCreation();
                PsiExpressionList argumentList = classInstanceCreation.getArgumentList();
                if (argumentList != null) {
                    for (PsiExpression expression : argumentList.getExpressions()) {
                        if (expression instanceof PsiThisExpression)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean containsNullCheckForTargetObject(ClassObject targetClass) {
        List<PsiExpression> literals = getLiterals();
        for (PsiExpression literal : literals) {
            if (literal instanceof PsiLiteralExpressionImpl) {
                PsiLiteralExpressionImpl literalExpression = (PsiLiteralExpressionImpl) literal;
                if (JavaTokenType.NULL_KEYWORD.equals(literalExpression.getLiteralElementType())) {
                    if (literal.getParent() instanceof PsiBinaryExpression) {
                        PsiBinaryExpression infixExpression = (PsiBinaryExpression) literal.getParent();
                        PsiExpression leftOperand = infixExpression.getLOperand();
                        PsiElement psiElement = leftOperand.getLastChild().getOriginalElement();
                        if (psiElement.getParent() instanceof PsiReferenceExpression) {
                            PsiElement resolvedElement = ((PsiReferenceExpression) psiElement.getParent()).resolve();
                            if (resolvedElement instanceof PsiVariable && !(resolvedElement instanceof PsiLocalVariable)) {
                                PsiVariable psiVariable = (PsiVariable) resolvedElement;
                                IElementType variableType = infixExpression.getOperationSign().getTokenType();
                                if (targetClass.getName().equals(psiVariable.getType().getCanonicalText())
                                        && (JavaTokenType.EQEQ.equals(variableType) || JavaTokenType.NE.equals(variableType))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean overridesMethod() {
        PsiMethod methodBinding = getMethodDeclaration();
        return !(AnnotationUtil.findAnnotation(methodBinding, "Override") == null &&
                methodBinding.findSuperMethods().length == 0);
    }

    public String getClassName() {
        return constructorObject.getClassName();
    }

    public ListIterator<ParameterObject> getParameterListIterator() {
        return constructorObject.getParameterListIterator();
    }

    public ParameterObject getParameter(int position) {
        return constructorObject.getParameter(position);
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        return constructorObject.getMethodInvocations();
    }

    public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
        return constructorObject.getSuperMethodInvocations();
    }

    public List<FieldInstructionObject> getFieldInstructions() {
        return constructorObject.getFieldInstructions();
    }

    public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
        return constructorObject.getSuperFieldInstructions();
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
        return constructorObject.getLocalVariableDeclarations();
    }

    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
        return constructorObject.getLocalVariableInstructions();
    }

    public List<CreationObject> getCreations() {
        return constructorObject.getCreations();
    }

    public List<PsiExpression> getLiterals() {
        return constructorObject.getLiterals();
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return constructorObject.getAnonymousClassDeclarations();
    }

    public Set<String> getExceptionsInThrowStatements() {
        return constructorObject.getExceptionsInThrowStatements();
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
        return constructorObject.containsMethodInvocation(methodInvocation);
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
        return constructorObject.containsFieldInstruction(fieldInstruction);
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        return constructorObject.containsSuperMethodInvocation(superMethodInvocation);
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
        return constructorObject.getInvokedMethodsThroughFields();
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields() {
        return constructorObject.getNonDistinctInvokedMethodsThroughFields();
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
        return constructorObject.getInvokedMethodsThroughParameters();
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters() {
        return constructorObject.getNonDistinctInvokedMethodsThroughParameters();
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
        return constructorObject.getInvokedMethodsThroughLocalVariables();
    }

    public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
        return constructorObject.getInvokedMethodsThroughThisReference();
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference() {
        return constructorObject.getNonDistinctInvokedMethodsThroughThisReference();
    }

    public Set<MethodInvocationObject> getInvokedStaticMethods() {
        return constructorObject.getInvokedStaticMethods();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughFields() {
        return constructorObject.getDefinedFieldsThroughFields();
    }

    public Set<AbstractVariable> getUsedFieldsThroughFields() {
        return constructorObject.getUsedFieldsThroughFields();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
        return constructorObject.getNonDistinctDefinedFieldsThroughFields();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
        return constructorObject.getNonDistinctUsedFieldsThroughFields();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
        return constructorObject.getDefinedFieldsThroughParameters();
    }

    public Set<AbstractVariable> getUsedFieldsThroughParameters() {
        return constructorObject.getUsedFieldsThroughParameters();
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
        return constructorObject.getNonDistinctDefinedFieldsThroughParameters();
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
        return constructorObject.getNonDistinctUsedFieldsThroughParameters();
    }

    public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
        return constructorObject.getDefinedFieldsThroughLocalVariables();
    }

    public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
        return constructorObject.getUsedFieldsThroughLocalVariables();
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
        return constructorObject.getDefinedFieldsThroughThisReference();
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference() {
        return constructorObject.getNonDistinctDefinedFieldsThroughThisReference();
    }

    public Set<PlainVariable> getUsedFieldsThroughThisReference() {
        return constructorObject.getUsedFieldsThroughThisReference();
    }

    public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference() {
        return constructorObject.getNonDistinctUsedFieldsThroughThisReference();
    }

    public Set<PlainVariable> getDeclaredLocalVariables() {
        return constructorObject.getDeclaredLocalVariables();
    }

    public Set<PlainVariable> getDefinedLocalVariables() {
        return constructorObject.getDefinedLocalVariables();
    }

    public Set<PlainVariable> getUsedLocalVariables() {
        return constructorObject.getUsedLocalVariables();
    }

    public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
        return constructorObject.getParametersPassedAsArgumentsInMethodInvocations();
    }

    public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations() {
        return constructorObject.getParametersPassedAsArgumentsInSuperMethodInvocations();
    }

    public boolean containsSuperMethodInvocation() {
        return constructorObject.containsSuperMethodInvocation();
    }

    public boolean containsSuperFieldAccess() {
        return constructorObject.containsSuperFieldAccess();
    }

    public List<TypeObject> getParameterTypeList() {
        return constructorObject.getParameterTypeList();
    }

    public List<String> getParameterList() {
        return constructorObject.getParameterList();
    }

    public boolean equals(MethodInvocationObject mio) {
        return this.getClassName().equals(mio.getOriginClassName()) && this.getName().equals(mio.getMethodName()) &&
                this.returnType.equalsClassType(mio.getReturnType()) && equalParameterTypes(this.constructorObject.getParameterTypeList(), mio.getParameterTypeList());
    }

    public boolean equals(SuperMethodInvocationObject smio) {
        return this.getClassName().equals(smio.getOriginClassName()) && this.getName().equals(smio.getMethodName()) &&
                this.returnType.equalsClassType(smio.getReturnType()) && equalParameterTypes(this.constructorObject.getParameterTypeList(), smio.getParameterTypeList());
    }

    private boolean equalParameterTypes(List<TypeObject> list1, List<TypeObject> list2) {
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            TypeObject type1 = list1.get(i);
            TypeObject type2 = list2.get(i);
            if (!type1.equalsClassType(type2))
                return false;
            //array dimension comparison is skipped if at least one of the class types is a type parameter name, such as E, K, N, T, V, S, U
            if (type1.getArrayDimension() != type2.getArrayDimension() && type1.getClassType().length() != 1 && type2.getClassType().length() != 1)
                return false;
        }
        return true;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof MethodObject) {
            MethodObject methodObject = (MethodObject) o;

            return this.returnType.equals(methodObject.returnType) &&
                    this.constructorObject.equals(methodObject.constructorObject);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + returnType.hashCode();
            result = 37 * result + constructorObject.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!constructorObject.access.equals(Access.NONE))
            sb.append(constructorObject.access.toString()).append(" ");
        if (_abstract)
            sb.append("abstract").append(" ");
        if (_static)
            sb.append("static").append(" ");
        sb.append(returnType.toString()).append(" ");
        sb.append(constructorObject.name);
        sb.append("(");
        if (!constructorObject.parameterList.isEmpty()) {
            for (int i = 0; i < constructorObject.parameterList.size() - 1; i++)
                sb.append(constructorObject.parameterList.get(i).toString()).append(", ");
            sb.append(constructorObject.parameterList.get(constructorObject.parameterList.size() - 1).toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public String getSignature() {
        return constructorObject.getSignature();
    }

    public boolean containsFieldAccessOfEnclosingClass() {
        //check for field access like SegmentedTimeline.this.segmentsIncluded
        if (getPsiMethod().getBody() == null) {
            return false;
        }

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> fieldAccesses = expressionExtractor.getVariableInstructions(getPsiMethod().getBody().getStatements());
        for (PsiExpression expression : fieldAccesses) {
            PsiReferenceExpression fieldReference = (PsiReferenceExpression) expression;
            Collection<PsiElement> psiElements = PsiTreeUtil.findChildrenOfType(fieldReference, PsiThisExpression.class);

            for (PsiElement thisExpressionElement : psiElements) {
                PsiThisExpression thisExpression = (PsiThisExpression) thisExpressionElement;
                if (thisExpression.getQualifier() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
