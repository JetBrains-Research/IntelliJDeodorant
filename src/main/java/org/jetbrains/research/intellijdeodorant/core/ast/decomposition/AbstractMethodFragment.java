package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.research.intellijdeodorant.core.ast.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.jetbrains.research.intellijdeodorant.core.ast.ASTReader.getExaminedProject;
import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.resolveMethod;

public abstract class AbstractMethodFragment {
    private final AbstractMethodFragment parent;

    private final List<MethodInvocationObject> methodInvocationList;
    private final List<SuperMethodInvocationObject> superMethodInvocationList;
    private final List<ConstructorInvocationObject> constructorInvocationList;
    private final List<FieldInstructionObject> fieldInstructionList;
    private final List<SuperFieldInstructionObject> superFieldInstructionList;
    private final List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private final List<LocalVariableInstructionObject> localVariableInstructionList;
    private final List<CreationObject> creationList;
    private final List<LiteralObject> literalList;
    private final List<AnonymousClassDeclarationObject> anonymousClassDeclarationList;
    private final Set<String> exceptionsInThrowStatements;
    private final Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughFields;
    private final Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughParameters;
    private final Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughLocalVariables;
    private final List<MethodInvocationObject> nonDistinctInvokedMethodsThroughThisReference;
    private final List<MethodInvocationObject> nonDistinctInvokedStaticMethods;
    private final List<AbstractVariable> nonDistinctDefinedFieldsThroughFields;
    private final List<AbstractVariable> nonDistinctUsedFieldsThroughFields;
    private final List<AbstractVariable> nonDistinctDefinedFieldsThroughParameters;
    private final List<AbstractVariable> nonDistinctUsedFieldsThroughParameters;
    private final List<AbstractVariable> nonDistinctDefinedFieldsThroughLocalVariables;
    private final List<AbstractVariable> nonDistinctUsedFieldsThroughLocalVariables;
    private final List<PlainVariable> nonDistinctDefinedFieldsThroughThisReference;
    private final List<PlainVariable> nonDistinctUsedFieldsThroughThisReference;

    private final Set<PlainVariable> declaredLocalVariables;
    private final Set<PlainVariable> definedLocalVariables;
    private final Set<PlainVariable> usedLocalVariables;
    private final Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations;
    private final Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> parametersPassedAsArgumentsInSuperMethodInvocations;
    private final Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> parametersPassedAsArgumentsInConstructorInvocations;
    private final Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> variablesAssignedWithClassInstanceCreations;

    AbstractMethodFragment(AbstractMethodFragment parent) {
        this.parent = parent;
        this.methodInvocationList = new ArrayList<>();
        this.superMethodInvocationList = new ArrayList<>();
        this.constructorInvocationList = new ArrayList<>();
        this.fieldInstructionList = new ArrayList<>();
        this.superFieldInstructionList = new ArrayList<>();
        this.localVariableDeclarationList = new ArrayList<>();
        this.localVariableInstructionList = new ArrayList<>();
        this.creationList = new ArrayList<>();
        this.literalList = new ArrayList<>();
        this.anonymousClassDeclarationList = new ArrayList<>();
        this.exceptionsInThrowStatements = new LinkedHashSet<>();

        this.nonDistinctInvokedMethodsThroughFields = new LinkedHashMap<>();
        this.nonDistinctInvokedMethodsThroughParameters = new LinkedHashMap<>();
        this.nonDistinctInvokedMethodsThroughLocalVariables = new LinkedHashMap<>();
        this.nonDistinctInvokedMethodsThroughThisReference = new ArrayList<>();
        this.nonDistinctInvokedStaticMethods = new ArrayList<>();
        this.nonDistinctDefinedFieldsThroughFields = new ArrayList<>();
        this.nonDistinctUsedFieldsThroughFields = new ArrayList<>();
        this.nonDistinctDefinedFieldsThroughParameters = new ArrayList<>();
        this.nonDistinctUsedFieldsThroughParameters = new ArrayList<>();
        this.nonDistinctDefinedFieldsThroughLocalVariables = new ArrayList<>();
        this.nonDistinctUsedFieldsThroughLocalVariables = new ArrayList<>();
        this.nonDistinctDefinedFieldsThroughThisReference = new ArrayList<>();
        this.nonDistinctUsedFieldsThroughThisReference = new ArrayList<>();

        this.declaredLocalVariables = new LinkedHashSet<>();
        this.definedLocalVariables = new LinkedHashSet<>();
        this.usedLocalVariables = new LinkedHashSet<>();
        this.parametersPassedAsArgumentsInMethodInvocations = new LinkedHashMap<>();
        this.parametersPassedAsArgumentsInSuperMethodInvocations = new LinkedHashMap<>();
        this.parametersPassedAsArgumentsInConstructorInvocations = new LinkedHashMap<>();
        this.variablesAssignedWithClassInstanceCreations = new LinkedHashMap<>();
    }

    public AbstractMethodFragment getParent() {
        return this.parent;
    }

    void processVariables(List<PsiExpression> variableInstructions, List<PsiExpression> assignments,
                          List<PsiExpression> postfixExpressions, List<PsiExpression> prefixExpressions) {
        for (PsiExpression variableInstruction : variableInstructions) {
            if (variableInstruction instanceof PsiReferenceExpression) {
                PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) variableInstruction;
                PsiElement resolvedReference = psiReferenceExpression.resolve();
                PsiExpression qualifierExpression = psiReferenceExpression.getQualifierExpression();
                PsiElement resolvedElement = null;
                if (qualifierExpression instanceof PsiReferenceExpression) {
                    resolvedElement = ((PsiReferenceExpression) qualifierExpression).resolve();
                }

                if (resolvedReference instanceof PsiField) {
                    PsiField psiField = (PsiField) resolvedReference;
                    if (psiField.getContainingClass() != null) {
                        String originClassName = PsiUtil.getMemberQualifiedName(psiField.getContainingClass());
                        String fieldType = psiField.getType().getCanonicalText();
                        TypeObject typeObject = TypeObject.extractTypeObject(fieldType);
                        String fieldName = psiField.getName();
                        if (originClassName != null && !originClassName.equals("")) {
                            if (variableInstruction instanceof PsiSuperExpression) {
                                SuperFieldInstructionObject superFieldInstruction = new SuperFieldInstructionObject(originClassName, typeObject, fieldName);
                                superFieldInstruction.setSimpleName(resolvedReference);
                                if ((psiField.hasModifier(JvmModifier.STATIC)))
                                    superFieldInstruction.setStatic(true);
                                addSuperFieldInstruction(superFieldInstruction);
                            } else {
                                FieldInstructionObject fieldInstruction = new FieldInstructionObject(originClassName, typeObject, fieldName, psiField);
                                if ((psiField.hasModifier(JvmModifier.STATIC)))
                                    fieldInstruction.setStatic(true);
                                addFieldInstruction(fieldInstruction);
                                Set<PsiAssignmentExpression> fieldAssignments = getMatchingAssignments(psiField, assignments);
                                Set<PsiPostfixExpression> fieldPostfixAssignments = getMatchingPostfixAssignments(psiField, postfixExpressions);
                                Set<PsiPrefixExpression> fieldPrefixAssignments = getMatchingPrefixAssignments(psiField, prefixExpressions);
                                AbstractVariable variable;

                                if (resolvedElement instanceof PsiVariable) {
                                    variable = MethodDeclarationUtility.createVariable((PsiVariable) resolvedElement, new PlainVariable(psiField));
                                } else {
                                    variable = MethodDeclarationUtility.createVariable(psiField, null);
                                }

                                if (!fieldAssignments.isEmpty()) {
                                    handleDefinedField(variable);
                                    for (PsiAssignmentExpression assignment : fieldAssignments) {
                                        PsiJavaToken operator = assignment.getOperationSign();
                                        if (!JavaTokenType.EQ.equals(operator.getTokenType()))
                                            handleUsedField(variable);
                                    }
                                }
                                if (!fieldPostfixAssignments.isEmpty()) {
                                    handleDefinedField(variable);
                                    handleUsedField(variable);
                                }
                                if (!fieldPrefixAssignments.isEmpty()) {
                                    handleDefinedField(variable);
                                    handleUsedField(variable);
                                }
                                if (fieldAssignments.isEmpty() && fieldPostfixAssignments.isEmpty() && fieldPrefixAssignments.isEmpty()) {
                                    handleUsedField(variable);
                                }
                            }
                        }
                    }
                } else if (resolvedReference instanceof PsiVariable) {
                    PsiVariable resolvedVariable = (PsiVariable) resolvedReference;
                    String variableName = resolvedVariable.getName();
                    String variableType = resolvedVariable.getType().getCanonicalText();
                    TypeObject localVariableType = TypeObject.extractTypeObject(variableType);
                    PlainVariable variable = new PlainVariable(resolvedVariable);
                    LocalVariableInstructionObject localVariable = new LocalVariableInstructionObject(localVariableType, variableName);
                    localVariable.setReference(psiReferenceExpression);
                    addLocalVariableInstruction(localVariable);
                    Set<PsiAssignmentExpression> localVariableAssignments = getMatchingAssignments(resolvedVariable, assignments);
                    Set<PsiPostfixExpression> localVariablePostfixAssignments =
                            getMatchingPostfixAssignments(resolvedVariable, postfixExpressions);
                    Set<PsiPrefixExpression> localVariablePrefixAssignments =
                            getMatchingPrefixAssignments(resolvedVariable, prefixExpressions);
                    if (!localVariableAssignments.isEmpty()) {
                        addDefinedLocalVariable(variable);
                        for (PsiAssignmentExpression assignment : localVariableAssignments) {
                            PsiJavaToken operator = assignment.getOperationSign();
                            if (!JavaTokenType.EQ.equals(operator.getTokenType()))
                                addUsedLocalVariable(variable);
                        }
                    }
                    if (!localVariablePostfixAssignments.isEmpty()) {
                        addDefinedLocalVariable(variable);
                        addUsedLocalVariable(variable);
                    }
                    if (!localVariablePrefixAssignments.isEmpty()) {
                        addDefinedLocalVariable(variable);
                        addUsedLocalVariable(variable);
                    }
                    if (localVariableAssignments.isEmpty() && localVariablePostfixAssignments.isEmpty()
                            && localVariablePrefixAssignments.isEmpty()) {
                        addUsedLocalVariable(variable);
                    }
                }
            }
        }
    }

    void processLocalVariableDeclaration(PsiStatement statement) {
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            for (PsiElement element : declaredElements) {
                if (element instanceof PsiLocalVariable) {
                    PsiLocalVariable declaredVariable = (PsiLocalVariable) element;
                    String variableType = declaredVariable.getType().getCanonicalText();
                    TypeObject localVariableType = TypeObject.extractTypeObject(variableType);
                    LocalVariableDeclarationObject localVariable =
                            new LocalVariableDeclarationObject(localVariableType, declaredVariable.getName());
                    localVariable.setVariableDeclaration(declaredVariable);
                    addLocalVariableDeclaration(localVariable);
                    addDeclaredLocalVariable(new PlainVariable(declaredVariable));
                }
            }
        }
    }

    private void addFieldInstruction(FieldInstructionObject fieldInstruction) {
        fieldInstructionList.add(fieldInstruction);
        if (parent != null) {
            parent.addFieldInstruction(fieldInstruction);
        }
    }

    private void addSuperFieldInstruction(SuperFieldInstructionObject superFieldInstruction) {
        superFieldInstructionList.add(superFieldInstruction);
        if (parent != null) {
            parent.addSuperFieldInstruction(superFieldInstruction);
        }
    }

    private void addLocalVariableDeclaration(LocalVariableDeclarationObject localVariable) {
        localVariableDeclarationList.add(localVariable);
        if (parent != null) {
            parent.addLocalVariableDeclaration(localVariable);
        }
    }

    private void addLocalVariableInstruction(LocalVariableInstructionObject localVariable) {
        localVariableInstructionList.add(localVariable);
        if (parent != null) {
            parent.addLocalVariableInstruction(localVariable);
        }
    }

    private void addDeclaredLocalVariable(PlainVariable variable) {
        declaredLocalVariables.add(variable);
        if (parent != null) {
            parent.addDeclaredLocalVariable(variable);
        }
    }

    private void addDefinedLocalVariable(PlainVariable variable) {
        definedLocalVariables.add(variable);
        if (parent != null) {
            parent.addDefinedLocalVariable(variable);
        }
    }

    private void addUsedLocalVariable(PlainVariable variable) {
        usedLocalVariables.add(variable);
        if (parent != null) {
            parent.addUsedLocalVariable(variable);
        }
    }

    void processMethodInvocations(List<PsiExpression> methodInvocations) {
        for (PsiExpression expression : methodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                PsiMethod resolveMethod = methodInvocation.resolveMethod();
                String originClassName = "";

                if (resolveMethod != null && resolveMethod.getContainingClass() != null) {
                    originClassName = resolveMethod.getContainingClass().getQualifiedName();
                }

                if (resolveMethod == null || methodInvocation.getMethodExpression().getQualifierExpression() != null) {
                    PsiMethodCallExpression methodExpression = getFirstMethodCallInAChain(methodInvocation);
                    String methodName = methodExpression.getMethodExpression().getReferenceName();
                    PsiReferenceExpression qualifierExpression = getFirstQualifierInAChain(methodExpression);

                    if (qualifierExpression == null) {
                        PsiMethod resolvedMethod = resolveMethod(methodInvocation);
                        if (resolvedMethod != null && resolvedMethod.getContainingClass() != null) {
                            boolean isStatic = resolvedMethod.hasModifier(JvmModifier.STATIC);
                            originClassName = resolvedMethod.getContainingClass().getQualifiedName();
                            processMethodInvocation(methodInvocation, originClassName, isStatic);
                        }
                    } else {
                        PsiElement resolvedElement = qualifierExpression.resolve();
                        if (resolvedElement instanceof PsiParameter
                                && resolvedElement.getParent() instanceof PsiParameterList) {
                            PsiType resolvedQualifierType = ((PsiParameter) resolvedElement).getType();
                            originClassName = getQualifiedNameByType(resolvedQualifierType);
                        } else if (resolvedElement instanceof PsiField) {
                            PsiType resolvedQualifierType = ((PsiField) resolvedElement).getType();
                            originClassName = getQualifiedNameByType(resolvedQualifierType);
                        }
                        if (originClassName != null && !originClassName.equals("")) {
                            processMethodInvocation(methodExpression, originClassName, false);
                        }
                    }
                } else {
                    boolean isMethodStatic = resolveMethod.hasModifier(JvmModifier.STATIC);
                    if (resolveMethod.getContainingClass() != null) {
                        originClassName = resolveMethod.getContainingClass().getQualifiedName();
                    }
                    processMethodInvocation(methodInvocation, originClassName, isMethodStatic);
                }
            }
        }
    }

    private PsiReferenceExpression getFirstQualifierInAChain(PsiExpression expression) {
        if (expression instanceof PsiMethodCallExpression) {
            PsiExpression qualifierExpression =
                    ((PsiMethodCallExpression) expression).getMethodExpression().getQualifierExpression();
            return getFirstQualifierInAChain(qualifierExpression);
        } else if (expression instanceof PsiReferenceExpression) {
            PsiExpression qualifierExpression = ((PsiReferenceExpression) expression).getQualifierExpression();
            if (qualifierExpression instanceof PsiReferenceExpression) {
                return getFirstQualifierInAChain(qualifierExpression);
            } else {
                return (PsiReferenceExpression) expression;
            }
        }
        return null;
    }

    private String getQualifiedNameByType(PsiType classReferenceType) {
        if (classReferenceType instanceof PsiClassReferenceType) {
            PsiClass resolvedClass = ((PsiClassReferenceType) classReferenceType).resolve();
            if (resolvedClass != null) {
                return resolvedClass.getQualifiedName();
            }
        }

        return "";
    }

    private void processMethodInvocation(PsiMethodCallExpression methodInvocation, String originClassName, boolean isMethodStatic) {
        TypeObject originClassTypeObject = TypeObject.extractTypeObject(originClassName);
        String methodInvocationName = methodInvocation.getMethodExpression().getReferenceName();

        String canonicalText = "UNK";

        PsiMethod resolvedMethod = methodInvocation.resolveMethod();

        ArrayList<TypeObject> typeObjects = new ArrayList<>();
        if (resolvedMethod != null) {
            for (PsiParameter parameter : resolvedMethod.getParameterList().getParameters()) {
                typeObjects.add(TypeObject.extractTypeObject(parameter.getType().getCanonicalText()));
            }

            PsiType methodReturnType = resolvedMethod.getReturnType();
            if (methodReturnType != null) {
                canonicalText = methodReturnType.getCanonicalText();
            }
        }
        TypeObject returnType = TypeObject.extractTypeObject(canonicalText);

        MethodInvocationObject methodInvocationObject = new MethodInvocationObject(originClassTypeObject, methodInvocationName, returnType, typeObjects);
        methodInvocationObject.setMethodInvocation(methodInvocation);
        methodInvocationObject.setStatic(isMethodStatic);
        addMethodInvocation(methodInvocationObject);

        AbstractVariable invoker = MethodDeclarationUtility
                .processMethodInvocationExpression(getFirstQualifierInAChain(methodInvocation));

        if (invoker != null) {
            PlainVariable initialVariable = invoker.getInitialVariable();
            if (initialVariable.isField()) {
                addNonDistinctInvokedMethodThroughField(invoker, methodInvocationObject);
            } else if (initialVariable.isParameter()) {
                addNonDistinctInvokedMethodThroughParameter(invoker, methodInvocationObject);
            } else {
                addNonDistinctInvokedMethodThroughLocalVariable(invoker, methodInvocationObject);
            }
        } else {
            if (methodInvocationObject.isStatic())
                addStaticallyInvokedMethod(methodInvocationObject);
            else {
                PsiExpression qualifier = methodInvocation.getMethodExpression().getQualifierExpression();
                if (qualifier == null || qualifier instanceof PsiThisExpression) {
                    addNonDistinctInvokedMethodThroughThisReference(methodInvocationObject);
                }
            }
        }
    }

    private PsiMethodCallExpression getFirstMethodCallInAChain(PsiMethodCallExpression methodCallExpression) {
        PsiExpression qualifierExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        if (qualifierExpression instanceof PsiMethodCallExpression) {
            return getFirstMethodCallInAChain((PsiMethodCallExpression) qualifierExpression);
        } else {
            return methodCallExpression;
        }
    }

    private void addMethodInvocation(MethodInvocationObject methodInvocationObject) {
        methodInvocationList.add(methodInvocationObject);
        if (parent != null) {
            parent.addMethodInvocation(methodInvocationObject);
        }
    }

    private void addSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocationObject) {
        superMethodInvocationList.add(superMethodInvocationObject);
        if (parent != null) {
            parent.addSuperMethodInvocation(superMethodInvocationObject);
        }
    }

    private void addConstructorInvocation(ConstructorInvocationObject constructorInvocationObject) {
        constructorInvocationList.add(constructorInvocationObject);
        if (parent != null) {
            parent.addConstructorInvocation(constructorInvocationObject);
        }
    }

    void processClassInstanceCreations(List<PsiExpression> classInstanceCreations) {
        for (PsiExpression classInstanceCreationExpression : classInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) classInstanceCreationExpression;
            if (classInstanceCreation != null && classInstanceCreation.getClassOrAnonymousClassReference() != null) {
                PsiJavaCodeReferenceElement referenceElement = classInstanceCreation.getClassOrAnonymousClassReference();
                PsiClass psiClass = (PsiClass) referenceElement.resolve();
                if (psiClass == null) continue;
                TypeObject typeObject = null;
                if (psiClass.getQualifiedName() != null) {
                    typeObject = TypeObject.extractTypeObject(psiClass.getQualifiedName());
                }
                ClassInstanceCreationObject creationObject = new ClassInstanceCreationObject(typeObject);
                creationObject.setClassInstanceCreation(classInstanceCreation);
                if (psiClass.getTypeParameterList() != null) {
                    for (PsiTypeParameter parameterType : psiClass.getTypeParameterList().getTypeParameters()) {
                        String qualifiedParameterName = parameterType.getQualifiedName() == null ? "java.lang.Object" : parameterType.getQualifiedName();
                        TypeObject parameterTypeObject = TypeObject.extractTypeObject(qualifiedParameterName);
                        creationObject.addParameter(parameterTypeObject);
                    }
                }
                PsiAnonymousClass anonymous = classInstanceCreation.getAnonymousClass();
                if (anonymous != null) {
                    final AnonymousClassDeclarationObject anonymousClassObject = new AnonymousClassDeclarationObject();
                    if (anonymous.getName() != null) {
                        anonymousClassObject.setName(anonymous.getName());
                    }
                    anonymousClassObject.setAnonymousClassDeclaration(anonymous);

                    PsiField[] fields = anonymous.getFields();
                    PsiMethod[] methods = anonymous.getMethods();

                    for (PsiField psiField : fields) {
                        TypeObject fieldType = TypeObject.extractTypeObject(psiField.getType().getCanonicalText());
                        fieldType.setArrayDimension(fieldType.getArrayDimension());
                        FieldObject fieldObject = new FieldObject(fieldType, psiField.getName(), psiField);
                        fieldObject.setClassName(anonymousClassObject.getName());
                        if ((psiField.hasModifier(JvmModifier.PUBLIC)))
                            fieldObject.setAccess(Access.PUBLIC);
                        else if (psiField.hasModifier(JvmModifier.PROTECTED))
                            fieldObject.setAccess(Access.PROTECTED);
                        else if (psiField.hasModifier(JvmModifier.PRIVATE))
                            fieldObject.setAccess(Access.PRIVATE);
                        else
                            fieldObject.setAccess(Access.NONE);

                        if (psiField.hasModifier(JvmModifier.STATIC))
                            fieldObject.setStatic(true);

                        anonymousClassObject.addField(fieldObject);
                    }

                    for (PsiMethod psiMethod : methods) {
                        final ConstructorObject constructorObject = new ConstructorObject();
                        constructorObject.setMethodDeclaration(psiMethod);
                        constructorObject.setName(psiMethod.getName());
                        constructorObject.setClassName(anonymousClassObject.getName());

                        if ((psiMethod.hasModifier(JvmModifier.PUBLIC)))
                            constructorObject.setAccess(Access.PUBLIC);
                        else if ((psiMethod.hasModifier(JvmModifier.PROTECTED)))
                            constructorObject.setAccess(Access.PROTECTED);
                        else if ((psiMethod.hasModifier(JvmModifier.PRIVATE)))
                            constructorObject.setAccess(Access.PRIVATE);
                        else
                            constructorObject.setAccess(Access.NONE);

                        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
                        for (PsiParameter parameter : parameters) {
                            TypeObject parameterType = TypeObject.extractTypeObject(parameter.getType().getCanonicalText());
                            parameterType.setArrayDimension(parameterType.getArrayDimension());
                            if (parameter.isVarArgs()) {
                                parameterType.setArrayDimension(1);
                            }
                            ParameterObject parameterObject = new ParameterObject(parameterType, parameter.getName(), parameter.isVarArgs());
                            parameterObject.setSingleVariableDeclaration(parameter);
                            constructorObject.addParameter(parameterObject);
                        }

                        PsiCodeBlock methodBody = psiMethod.getBody();
                        if (methodBody != null) {
                            MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
                            constructorObject.setMethodBody(methodBodyObject);
                        }

                        MethodObject methodObject = new MethodObject(psiMethod, constructorObject);
                        PsiModifierList extendedModifiers = psiMethod.getModifierList();
                        PsiAnnotation[] annotations = extendedModifiers.getAnnotations();
                        for (PsiAnnotation psiAnnotation : annotations) {
                            if (Objects.equals(psiAnnotation.getQualifiedName(), "Test")) {
                                methodObject.setTestAnnotation(true);
                                break;
                            }
                        }
                        PsiType returnType = psiMethod.getReturnType();
                        TypeObject returnTypeObject = null;
                        if (returnType != null) {
                            returnTypeObject = TypeObject.extractTypeObject(returnType.getCanonicalText());
                        }
                        methodObject.setReturnType(returnTypeObject);

                        if ((psiMethod.hasModifier(JvmModifier.ABSTRACT)))
                            methodObject.setAbstract(true);
                        if ((psiMethod.hasModifier(JvmModifier.STATIC)))
                            methodObject.setStatic(true);
                        if ((psiMethod.hasModifier(JvmModifier.SYNCHRONIZED)))
                            methodObject.setSynchronized(true);
                        if ((psiMethod.hasModifier(JvmModifier.NATIVE)))
                            methodObject.setNative(true);

                        anonymousClassObject.addMethod(methodObject);
                    }
                    addAnonymousClassDeclaration(anonymousClassObject);
                }

                PlainVariable variable = null;
                if (classInstanceCreation.getParent() instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) classInstanceCreation.getParent();
                    if (classInstanceCreation.equals(assignmentExpression.getRExpression())) {
                        if (assignmentExpression.getLExpression() instanceof PsiReferenceExpression) {
                            PsiReferenceExpression psiExpression = (PsiReferenceExpression) assignmentExpression.getLExpression();
                            PsiElement resolvedElement = psiExpression.resolve();
                            if (resolvedElement instanceof PsiVariable) {
                                variable = new PlainVariable((PsiVariable) resolvedElement);
                            }
                        }
                    }
                } else if (classInstanceCreation.getParent() instanceof PsiDeclarationStatement) {
                    PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) classInstanceCreation.getParent();
                    PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
                    for (PsiElement element : declaredElements) {
                        if (classInstanceCreation.equals(element)) {
                            if (element instanceof PsiVariable) {
                                variable = new PlainVariable((PsiVariable) element);
                            }
                        }
                    }
                }
                if (variable != null) {
                    addVariableAssignedWithClassInstanceCreation(variable, creationObject);
                }
                addCreation(creationObject);
            }
        }
    }

    void processArrayCreations(List<PsiExpression> arrayCreations) {
        for (PsiExpression arrayCreationExpression : arrayCreations) {
            if (!(arrayCreationExpression instanceof PsiNewExpression)) continue;
            PsiNewExpression arrayCreation = (PsiNewExpression) arrayCreationExpression;
            TypeObject typeObject = null;
            if (arrayCreation.getType() != null) {
                typeObject = TypeObject.extractTypeObject(arrayCreation.getType().getCanonicalText());
            }
            ArrayCreationObject creationObject = new ArrayCreationObject(typeObject);
            creationObject.setArrayCreation(arrayCreation);
            addCreation(creationObject);
        }
    }

    private void addCreation(CreationObject creationObject) {
        creationList.add(creationObject);
        if (parent != null) {
            parent.addCreation(creationObject);
        }
    }

    void processLiterals(List<PsiExpression> literals) {
        for (PsiExpression literal : literals) {
            LiteralObject literalObject = new LiteralObject(literal);
            addLiteral(literalObject);
        }
    }

    private void addLiteral(LiteralObject literalObject) {
        literalList.add(literalObject);
        if (parent != null) {
            parent.addLiteral(literalObject);
        }
    }

    private void addAnonymousClassDeclaration(AnonymousClassDeclarationObject anonymousClassObject) {
        anonymousClassDeclarationList.add(anonymousClassObject);
        if (parent != null) {
            parent.addAnonymousClassDeclaration(anonymousClassObject);
        }
    }

    void processThrowStatement(PsiThrowStatement throwStatement) {
        PsiExpression expression = throwStatement.getException();
        if (expression instanceof PsiNewExpression) {
            PsiNewExpression newExpression = (PsiNewExpression) expression;
            if (newExpression.getArrayDimensions().length == 0) {
                PsiMethod constructorCall = newExpression.resolveConstructor();
                if (constructorCall != null) {
                    addExceptionInThrowStatement(PsiUtil.getMemberQualifiedName(constructorCall));
                }
            }
        }
    }

    private void addExceptionInThrowStatement(String exception) {
        exceptionsInThrowStatements.add(exception);
        if (parent != null) {
            parent.addExceptionInThrowStatement(exception);
        }
    }

    private void addNonDistinctInvokedMethodThroughField(AbstractVariable field, MethodInvocationObject methodInvocation) {
        if (nonDistinctInvokedMethodsThroughFields.containsKey(field)) {
            ArrayList<MethodInvocationObject> methodInvocations = nonDistinctInvokedMethodsThroughFields.get(field);
            methodInvocations.add(methodInvocation);
        } else {
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<>();
            methodInvocations.add(methodInvocation);
            nonDistinctInvokedMethodsThroughFields.put(field, methodInvocations);
        }
        if (parent != null) {
            parent.addNonDistinctInvokedMethodThroughField(field, methodInvocation);
        }
    }

    private void addNonDistinctInvokedMethodThroughParameter(AbstractVariable parameter, MethodInvocationObject methodInvocation) {
        if (nonDistinctInvokedMethodsThroughParameters.containsKey(parameter)) {
            ArrayList<MethodInvocationObject> methodInvocations = nonDistinctInvokedMethodsThroughParameters.get(parameter);
            methodInvocations.add(methodInvocation);
        } else {
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<>();
            methodInvocations.add(methodInvocation);
            nonDistinctInvokedMethodsThroughParameters.put(parameter, methodInvocations);
        }
        if (parent != null) {
            parent.addNonDistinctInvokedMethodThroughParameter(parameter, methodInvocation);
        }
    }

    private void addNonDistinctInvokedMethodThroughLocalVariable(AbstractVariable localVariable, MethodInvocationObject methodInvocation) {
        if (nonDistinctInvokedMethodsThroughLocalVariables.containsKey(localVariable)) {
            ArrayList<MethodInvocationObject> methodInvocations = nonDistinctInvokedMethodsThroughLocalVariables.get(localVariable);
            methodInvocations.add(methodInvocation);
        } else {
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<>();
            methodInvocations.add(methodInvocation);
            nonDistinctInvokedMethodsThroughLocalVariables.put(localVariable, methodInvocations);
        }
        if (parent != null) {
            parent.addNonDistinctInvokedMethodThroughLocalVariable(localVariable, methodInvocation);
        }
    }

    private void addNonDistinctInvokedMethodThroughThisReference(MethodInvocationObject methodInvocation) {
        nonDistinctInvokedMethodsThroughThisReference.add(methodInvocation);
        if (parent != null) {
            parent.addNonDistinctInvokedMethodThroughThisReference(methodInvocation);
        }
    }

    private void addStaticallyInvokedMethod(MethodInvocationObject methodInvocation) {
        nonDistinctInvokedStaticMethods.add(methodInvocation);
        if (parent != null) {
            parent.addStaticallyInvokedMethod(methodInvocation);
        }
    }

    private void addParameterPassedAsArgumentInMethodInvocation(PlainVariable parameter, MethodInvocationObject methodInvocation) {
        if (parametersPassedAsArgumentsInMethodInvocations.containsKey(parameter)) {
            LinkedHashSet<MethodInvocationObject> methodInvocations = parametersPassedAsArgumentsInMethodInvocations.get(parameter);
            methodInvocations.add(methodInvocation);
        } else {
            LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<>();
            methodInvocations.add(methodInvocation);
            parametersPassedAsArgumentsInMethodInvocations.put(parameter, methodInvocations);
        }
        if (parent != null) {
            parent.addParameterPassedAsArgumentInMethodInvocation(parameter, methodInvocation);
        }
    }

    private void addParameterPassedAsArgumentInSuperMethodInvocation(PlainVariable parameter, SuperMethodInvocationObject methodInvocation) {
        if (parametersPassedAsArgumentsInSuperMethodInvocations.containsKey(parameter)) {
            LinkedHashSet<SuperMethodInvocationObject> methodInvocations = parametersPassedAsArgumentsInSuperMethodInvocations.get(parameter);
            methodInvocations.add(methodInvocation);
        } else {
            LinkedHashSet<SuperMethodInvocationObject> methodInvocations = new LinkedHashSet<>();
            methodInvocations.add(methodInvocation);
            parametersPassedAsArgumentsInSuperMethodInvocations.put(parameter, methodInvocations);
        }
        if (parent != null) {
            parent.addParameterPassedAsArgumentInSuperMethodInvocation(parameter, methodInvocation);
        }
    }

    private void addParameterPassedAsArgumentInConstructorInvocation(PlainVariable parameter, ConstructorInvocationObject constructorInvocation) {
        if (parametersPassedAsArgumentsInConstructorInvocations.containsKey(parameter)) {
            LinkedHashSet<ConstructorInvocationObject> constructorInvocations = parametersPassedAsArgumentsInConstructorInvocations.get(parameter);
            constructorInvocations.add(constructorInvocation);
        } else {
            LinkedHashSet<ConstructorInvocationObject> constructorInvocations = new LinkedHashSet<>();
            constructorInvocations.add(constructorInvocation);
            parametersPassedAsArgumentsInConstructorInvocations.put(parameter, constructorInvocations);
        }
        if (parent != null) {
            parent.addParameterPassedAsArgumentInConstructorInvocation(parameter, constructorInvocation);
        }
    }

    private void addVariableAssignedWithClassInstanceCreation(PlainVariable variable, ClassInstanceCreationObject classInstanceCreation) {
        if (variablesAssignedWithClassInstanceCreations.containsKey(variable)) {
            LinkedHashSet<ClassInstanceCreationObject> classInstanceCreations = variablesAssignedWithClassInstanceCreations.get(variable);
            classInstanceCreations.add(classInstanceCreation);
        } else {
            LinkedHashSet<ClassInstanceCreationObject> classInstanceCreations = new LinkedHashSet<>();
            classInstanceCreations.add(classInstanceCreation);
            variablesAssignedWithClassInstanceCreations.put(variable, classInstanceCreations);
        }
        if (parent != null) {
            parent.addVariableAssignedWithClassInstanceCreation(variable, classInstanceCreation);
        }
    }

    private Set<PsiAssignmentExpression> getMatchingAssignments(PsiVariable variable, List<PsiExpression> assignments) {
        Set<PsiAssignmentExpression> matchingAssignments = new LinkedHashSet<>();
        for (PsiExpression expression : assignments) {
            if (expression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                PsiExpression leftHandSide = assignment.getLExpression();
                if (leftHandSide instanceof PsiReferenceExpression) {
                    PsiReferenceExpression referenceExpression = (PsiReferenceExpression) leftHandSide;
                    PsiElement resolvedElement = referenceExpression.resolve();
                    if (variable.equals(resolvedElement)) {
                        matchingAssignments.add(assignment);
                    }
                }
            }
        }
        return matchingAssignments;
    }

    private Set<PsiPostfixExpression> getMatchingPostfixAssignments(PsiVariable variable, List<PsiExpression> postfixExpressions) {
        Set<PsiPostfixExpression> matchingPostfixAssignments = new LinkedHashSet<>();
        for (PsiExpression expression : postfixExpressions) {
            if (expression instanceof PsiPostfixExpression) {
                PsiPostfixExpression postfixExpression = (PsiPostfixExpression) expression;
                PsiElement element = postfixExpression.getOperand().getLastChild();
                if (element != null && element.equals(variable)) {
                    matchingPostfixAssignments.add(postfixExpression);
                }
            }
        }
        return matchingPostfixAssignments;
    }

    private Set<PsiPrefixExpression> getMatchingPrefixAssignments(PsiVariable variable, List<PsiExpression> prefixExpressions) {
        Set<PsiPrefixExpression> matchingPrefixAssignments = new LinkedHashSet<>();
        for (PsiExpression expression : prefixExpressions) {
            if (expression instanceof PsiPrefixExpression) {
                PsiPrefixExpression prefixExpression = (PsiPrefixExpression) expression;
                PsiExpression operand = prefixExpression.getOperand();
                if (operand == null) continue;
                PsiElement element = operand.getLastChild();
                if (element != null && element.equals(variable) && PsiUtil.isIncrementDecrementOperation(operand)) {
                    matchingPrefixAssignments.add(prefixExpression);
                }
            }
        }
        return matchingPrefixAssignments;
    }

    private void handleDefinedField(AbstractVariable variable) {
        if (variable != null) {
            PlainVariable initialVariable = variable.getInitialVariable();
            if (variable instanceof PlainVariable) {
                nonDistinctDefinedFieldsThroughThisReference.add((PlainVariable) variable);
            } else {
                if (initialVariable.isField()) {
                    nonDistinctDefinedFieldsThroughFields.add(variable);
                } else if (initialVariable.isParameter()) {
                    nonDistinctDefinedFieldsThroughParameters.add(variable);
                } else {
                    nonDistinctDefinedFieldsThroughLocalVariables.add(variable);
                }
            }
            if (parent != null) {
                parent.handleDefinedField(variable);
            }
        }
    }

    private void handleUsedField(AbstractVariable variable) {
        if (variable != null) {
            PlainVariable initialVariable = variable.getInitialVariable();
            if (variable instanceof PlainVariable) {
                if (!nonDistinctUsedFieldsThroughThisReference.contains(variable)) {
                    nonDistinctUsedFieldsThroughThisReference.add((PlainVariable) variable);
                }
            } else {
                if (initialVariable.isField()) {
                    if (!nonDistinctUsedFieldsThroughFields.contains(variable)) {
                        nonDistinctUsedFieldsThroughFields.add(variable);
                    }
                } else if (initialVariable.isParameter()) {
                    nonDistinctUsedFieldsThroughParameters.add(variable);
                } else {
                    nonDistinctUsedFieldsThroughLocalVariables.add(variable);
                }
            }
            if (parent != null) {
                parent.handleUsedField(variable);
            }
        }
    }

    public List<FieldInstructionObject> getFieldInstructions() {
        return fieldInstructionList;
    }

    public List<SuperFieldInstructionObject> getSuperFieldInstructions() {
        return superFieldInstructionList;
    }

    public List<LocalVariableDeclarationObject> getLocalVariableDeclarations() {
        return localVariableDeclarationList;
    }

    public List<LocalVariableInstructionObject> getLocalVariableInstructions() {
        return localVariableInstructionList;
    }

    public List<MethodInvocationObject> getMethodInvocations() {
        return methodInvocationList;
    }

    public List<SuperMethodInvocationObject> getSuperMethodInvocations() {
        return superMethodInvocationList;
    }

    public List<ConstructorInvocationObject> getConstructorInvocations() {
        return constructorInvocationList;
    }

    public List<CreationObject> getCreations() {
        return creationList;
    }

    List<ClassInstanceCreationObject> getClassInstanceCreations() {
        List<ClassInstanceCreationObject> classInstanceCreations = new ArrayList<>();
        for (CreationObject creation : creationList) {
            if (creation instanceof ClassInstanceCreationObject) {
                classInstanceCreations.add((ClassInstanceCreationObject) creation);
            }
        }
        return classInstanceCreations;
    }

    List<ArrayCreationObject> getArrayCreations() {
        List<ArrayCreationObject> arrayCreations = new ArrayList<>();
        for (CreationObject creation : creationList) {
            if (creation instanceof ArrayCreationObject) {
                arrayCreations.add((ArrayCreationObject) creation);
            }
        }
        return arrayCreations;
    }

    public List<LiteralObject> getLiterals() {
        return literalList;
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return anonymousClassDeclarationList;
    }

    public Set<String> getExceptionsInThrowStatements() {
        return exceptionsInThrowStatements;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
        return methodInvocationList.contains(methodInvocation);
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction) {
        return fieldInstructionList.contains(fieldInstruction);
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        return superMethodInvocationList.contains(superMethodInvocation);
    }

    public boolean containsLocalVariableDeclaration(LocalVariableDeclarationObject lvdo) {
        return localVariableDeclarationList.contains(lvdo);
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields() {
        Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughFields =
                new LinkedHashMap<>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughFields.keySet()) {
            invokedMethodsThroughFields.put(key, new LinkedHashSet<>(nonDistinctInvokedMethodsThroughFields.get(key)));
        }
        return invokedMethodsThroughFields;
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
        Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters =
                new LinkedHashMap<>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughParameters.keySet()) {
            invokedMethodsThroughParameters.put(key, new LinkedHashSet<>(nonDistinctInvokedMethodsThroughParameters.get(key)));
        }
        return invokedMethodsThroughParameters;
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields() {
        return nonDistinctInvokedMethodsThroughFields;
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters() {
        return nonDistinctInvokedMethodsThroughParameters;
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables() {
        Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughLocalVariables =
                new LinkedHashMap<>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughLocalVariables.keySet()) {
            invokedMethodsThroughLocalVariables.put(key, new LinkedHashSet<>(nonDistinctInvokedMethodsThroughLocalVariables.get(key)));
        }
        return invokedMethodsThroughLocalVariables;
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughLocalVariables() {
        return nonDistinctInvokedMethodsThroughLocalVariables;
    }

    public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
        return new LinkedHashSet<>(nonDistinctInvokedMethodsThroughThisReference);
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference() {
        return nonDistinctInvokedMethodsThroughThisReference;
    }

    public Set<MethodInvocationObject> getInvokedStaticMethods() {
        return new LinkedHashSet<>(nonDistinctInvokedStaticMethods);
    }

    List<MethodInvocationObject> getNonDistinctInvokedStaticMethods() {
        return nonDistinctInvokedStaticMethods;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughFields() {
        return new LinkedHashSet<>(nonDistinctDefinedFieldsThroughFields);
    }

    public Set<AbstractVariable> getUsedFieldsThroughFields() {
        return new LinkedHashSet<>(nonDistinctUsedFieldsThroughFields);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
        return nonDistinctDefinedFieldsThroughFields;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
        return nonDistinctUsedFieldsThroughFields;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
        return new LinkedHashSet<>(nonDistinctDefinedFieldsThroughParameters);
    }

    public Set<AbstractVariable> getUsedFieldsThroughParameters() {
        return new LinkedHashSet<>(nonDistinctUsedFieldsThroughParameters);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
        return nonDistinctDefinedFieldsThroughParameters;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
        return nonDistinctUsedFieldsThroughParameters;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
        return new LinkedHashSet<>(nonDistinctDefinedFieldsThroughLocalVariables);
    }

    public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
        return new LinkedHashSet<>(nonDistinctUsedFieldsThroughLocalVariables);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughLocalVariables() {
        return nonDistinctDefinedFieldsThroughLocalVariables;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughLocalVariables() {
        return nonDistinctUsedFieldsThroughLocalVariables;
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
        return new LinkedHashSet<>(nonDistinctDefinedFieldsThroughThisReference);
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference() {
        return nonDistinctDefinedFieldsThroughThisReference;
    }

    public Set<PlainVariable> getUsedFieldsThroughThisReference() {
        return new LinkedHashSet<>(nonDistinctUsedFieldsThroughThisReference);
    }

    public List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference() {
        return nonDistinctUsedFieldsThroughThisReference;
    }

    public Set<PlainVariable> getDeclaredLocalVariables() {
        return declaredLocalVariables;
    }

    public Set<PlainVariable> getDefinedLocalVariables() {
        return definedLocalVariables;
    }

    public Set<PlainVariable> getUsedLocalVariables() {
        return usedLocalVariables;
    }

    public Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations() {
        return parametersPassedAsArgumentsInMethodInvocations;
    }

    public Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations() {
        return parametersPassedAsArgumentsInSuperMethodInvocations;
    }

    public Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> getParametersPassedAsArgumentsInConstructorInvocations() {
        return parametersPassedAsArgumentsInConstructorInvocations;
    }

    public Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> getVariablesAssignedWithClassInstanceCreations() {
        return variablesAssignedWithClassInstanceCreations;
    }
}
