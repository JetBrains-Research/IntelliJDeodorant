package gr.uom.java.ast.decomposition;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import gr.uom.java.ast.Access;
import gr.uom.java.ast.AnonymousClassDeclarationObject;
import gr.uom.java.ast.ArrayCreationObject;
import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.ConstructorInvocationObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LiteralObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.SuperFieldInstructionObject;
import gr.uom.java.ast.SuperMethodInvocationObject;
import gr.uom.java.ast.TypeObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.*;

public abstract class AbstractMethodFragment {
    private AbstractMethodFragment parent;

    private List<MethodInvocationObject> methodInvocationList;
    private List<SuperMethodInvocationObject> superMethodInvocationList;
    private List<ConstructorInvocationObject> constructorInvocationList;
    private List<FieldInstructionObject> fieldInstructionList;
    private List<SuperFieldInstructionObject> superFieldInstructionList;
    private List<LocalVariableDeclarationObject> localVariableDeclarationList;
    private List<LocalVariableInstructionObject> localVariableInstructionList;
    private List<CreationObject> creationList;
    private List<LiteralObject> literalList;
    private List<AnonymousClassDeclarationObject> anonymousClassDeclarationList;
    private Set<PsiExpression> exceptionsInThrowStatements;
    private Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughFields;
    private Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughParameters;
    private Map<AbstractVariable, ArrayList<MethodInvocationObject>> nonDistinctInvokedMethodsThroughLocalVariables;
    private List<MethodInvocationObject> nonDistinctInvokedMethodsThroughThisReference;
    private List<MethodInvocationObject> nonDistinctInvokedStaticMethods;
    private List<AbstractVariable> nonDistinctDefinedFieldsThroughFields;
    private List<AbstractVariable> nonDistinctUsedFieldsThroughFields;
    private List<AbstractVariable> nonDistinctDefinedFieldsThroughParameters;
    private List<AbstractVariable> nonDistinctUsedFieldsThroughParameters;
    private List<AbstractVariable> nonDistinctDefinedFieldsThroughLocalVariables;
    private List<AbstractVariable> nonDistinctUsedFieldsThroughLocalVariables;
    private List<PlainVariable> nonDistinctDefinedFieldsThroughThisReference;
    private List<PlainVariable> nonDistinctUsedFieldsThroughThisReference;

    private Set<PlainVariable> declaredLocalVariables;
    private Set<PlainVariable> definedLocalVariables;
    private Set<PlainVariable> usedLocalVariables;
    private Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> parametersPassedAsArgumentsInMethodInvocations;
    private Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> parametersPassedAsArgumentsInSuperMethodInvocations;
    private Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> parametersPassedAsArgumentsInConstructorInvocations;
    private Map<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>> variablesAssignedWithClassInstanceCreations;

    protected AbstractMethodFragment(AbstractMethodFragment parent) {
        this.parent = parent;
        this.methodInvocationList = new ArrayList<MethodInvocationObject>();
        this.superMethodInvocationList = new ArrayList<SuperMethodInvocationObject>();
        this.constructorInvocationList = new ArrayList<ConstructorInvocationObject>();
        this.fieldInstructionList = new ArrayList<FieldInstructionObject>();
        this.superFieldInstructionList = new ArrayList<SuperFieldInstructionObject>();
        this.localVariableDeclarationList = new ArrayList<LocalVariableDeclarationObject>();
        this.localVariableInstructionList = new ArrayList<LocalVariableInstructionObject>();
        this.creationList = new ArrayList<CreationObject>();
        this.literalList = new ArrayList<LiteralObject>();
        this.anonymousClassDeclarationList = new ArrayList<AnonymousClassDeclarationObject>();
        this.exceptionsInThrowStatements = new LinkedHashSet<PsiExpression>();

        this.nonDistinctInvokedMethodsThroughFields = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
        this.nonDistinctInvokedMethodsThroughParameters = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
        this.nonDistinctInvokedMethodsThroughLocalVariables = new LinkedHashMap<AbstractVariable, ArrayList<MethodInvocationObject>>();
        this.nonDistinctInvokedMethodsThroughThisReference = new ArrayList<MethodInvocationObject>();
        this.nonDistinctInvokedStaticMethods = new ArrayList<MethodInvocationObject>();
        this.nonDistinctDefinedFieldsThroughFields = new ArrayList<AbstractVariable>();
        this.nonDistinctUsedFieldsThroughFields = new ArrayList<AbstractVariable>();
        this.nonDistinctDefinedFieldsThroughParameters = new ArrayList<AbstractVariable>();
        this.nonDistinctUsedFieldsThroughParameters = new ArrayList<AbstractVariable>();
        this.nonDistinctDefinedFieldsThroughLocalVariables = new ArrayList<AbstractVariable>();
        this.nonDistinctUsedFieldsThroughLocalVariables = new ArrayList<AbstractVariable>();
        this.nonDistinctDefinedFieldsThroughThisReference = new ArrayList<PlainVariable>();
        this.nonDistinctUsedFieldsThroughThisReference = new ArrayList<PlainVariable>();

        this.declaredLocalVariables = new LinkedHashSet<PlainVariable>();
        this.definedLocalVariables = new LinkedHashSet<PlainVariable>();
        this.usedLocalVariables = new LinkedHashSet<PlainVariable>();
        this.parametersPassedAsArgumentsInMethodInvocations = new LinkedHashMap<PlainVariable, LinkedHashSet<MethodInvocationObject>>();
        this.parametersPassedAsArgumentsInSuperMethodInvocations = new LinkedHashMap<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>>();
        this.parametersPassedAsArgumentsInConstructorInvocations = new LinkedHashMap<PlainVariable, LinkedHashSet<ConstructorInvocationObject>>();
        this.variablesAssignedWithClassInstanceCreations = new LinkedHashMap<PlainVariable, LinkedHashSet<ClassInstanceCreationObject>>();
    }

    public AbstractMethodFragment getParent() {
        return this.parent;
    }

    protected void processVariables(List<PsiExpression> variableInstructions, List<PsiExpression> assignments,
                                    List<PsiExpression> postfixExpressions, List<PsiExpression> prefixExpressions) {
        for (PsiExpression variableInstruction : variableInstructions) {
            Collection<PsiField> fields = PsiTreeUtil.findChildrenOfType(variableInstruction, PsiField.class);
            for (PsiField psiField : fields) {
                String originClassName = psiField.getContainingClass().getName();
                String qualifiedName = psiField.getType().getCanonicalText();
                TypeObject fieldType = TypeObject.extractTypeObject(qualifiedName);
                String fieldName = psiField.getName();
                if (!originClassName.equals("")) {
                    if (variableInstruction instanceof PsiSuperExpression) {
                        SuperFieldInstructionObject superFieldInstruction = new SuperFieldInstructionObject(originClassName, fieldType, fieldName);
                        superFieldInstruction.setSimpleName(psiField);
                        if ((psiField.hasModifier(JvmModifier.STATIC)))
                            superFieldInstruction.setStatic(true);
                        addSuperFieldInstruction(superFieldInstruction);
                    } else {
                        FieldInstructionObject fieldInstruction = new FieldInstructionObject(originClassName, fieldType, fieldName);
                        fieldInstruction.setSimpleName(psiField);
                        if ((psiField.hasModifier(JvmModifier.STATIC)))
                            fieldInstruction.setStatic(true);
                        addFieldInstruction(fieldInstruction);
                        Set<PsiAssignmentExpression> fieldAssignments = getMatchingAssignments(psiField, assignments);
                        Set<PsiPostfixExpression> fieldPostfixAssignments = getMatchingPostfixAssignments(psiField, postfixExpressions);
                        Set<PsiPrefixExpression> fieldPrefixAssignments = getMatchingPrefixAssignments(psiField, prefixExpressions);
                        AbstractVariable variable = MethodDeclarationUtility.createVariable(psiField, null);
                        if (!fieldAssignments.isEmpty()) {
                            handleDefinedField(variable);
                            for (PsiAssignmentExpression assignment : fieldAssignments) {
                                PsiJavaToken operator = assignment.getOperationSign();
                                if (!operator.equals(JavaTokenType.EQ))
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

            if (variableInstruction instanceof PsiReferenceExpression) {
                final PsiReferenceExpression argumentReference = (PsiReferenceExpression) variableInstruction;
                final PsiElement simpleName = argumentReference.resolve();
                if (simpleName instanceof PsiField) {
                    if (((PsiField) simpleName).getContainingClass() != null) {
                        PsiField psiField = (PsiField) simpleName;
                        String originClassName = psiField.getContainingClass().getName();
                        String qualifiedName = psiField.getType().getCanonicalText();
                        TypeObject fieldType = TypeObject.extractTypeObject(qualifiedName);
                        String fieldName = psiField.getName();
                        if (!originClassName.equals("")) {
                            if (variableInstruction instanceof PsiSuperExpression) {
                                SuperFieldInstructionObject superFieldInstruction = new SuperFieldInstructionObject(originClassName, fieldType, fieldName);
                                superFieldInstruction.setSimpleName(simpleName);
                                if ((psiField.hasModifier(JvmModifier.STATIC)))
                                    superFieldInstruction.setStatic(true);
                                addSuperFieldInstruction(superFieldInstruction);
                            } else {
                                FieldInstructionObject fieldInstruction = new FieldInstructionObject(originClassName, fieldType, fieldName);
                                fieldInstruction.setSimpleName(simpleName);
                                if ((psiField.hasModifier(JvmModifier.STATIC)))
                                    fieldInstruction.setStatic(true);
                                addFieldInstruction(fieldInstruction);
                                Set<PsiAssignmentExpression> fieldAssignments = getMatchingAssignments(simpleName, assignments);
                                Set<PsiPostfixExpression> fieldPostfixAssignments = getMatchingPostfixAssignments(simpleName, postfixExpressions);
                                Set<PsiPrefixExpression> fieldPrefixAssignments = getMatchingPrefixAssignments(simpleName, prefixExpressions);
                                AbstractVariable variable = MethodDeclarationUtility.createVariable(simpleName, null);
                                if (!fieldAssignments.isEmpty()) {
                                    handleDefinedField(variable);
                                    for (PsiAssignmentExpression assignment : fieldAssignments) {
                                        PsiJavaToken operator = assignment.getOperationSign();
                                        if (!operator.equals(JavaTokenType.EQ))
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
                } else {
                    if (simpleName != null && simpleName.getParent() instanceof PsiLocalVariable) {
                        String variableName = ((PsiVariable) simpleName).getName();
                        String variableType = ((PsiVariable) simpleName).getType().getCanonicalText();
                        TypeObject localVariableType = TypeObject.extractTypeObject(variableType);
                        PlainVariable variable = new PlainVariable(simpleName);
                        if (simpleName.isPhysical()) {
                            LocalVariableDeclarationObject localVariable = new LocalVariableDeclarationObject(localVariableType, variableName);
                            PsiDeclarationStatement variableDeclaration = (PsiDeclarationStatement) simpleName.getParent();
                            localVariable.setVariableDeclaration(variableDeclaration);
                            addLocalVariableDeclaration(localVariable);
                            addDeclaredLocalVariable(variable);
                        } else {
                            LocalVariableInstructionObject localVariable = new LocalVariableInstructionObject(localVariableType, variableName);
                            addLocalVariableInstruction(localVariable);
                            Set<PsiAssignmentExpression> localVariableAssignments = getMatchingAssignments(simpleName, assignments);
                            Set<PsiPostfixExpression> localVariablePostfixAssignments = getMatchingPostfixAssignments(simpleName, postfixExpressions);
                            Set<PsiPrefixExpression> localVariablePrefixAssignments = getMatchingPrefixAssignments(simpleName, prefixExpressions);
                            if (!localVariableAssignments.isEmpty()) {
                                addDefinedLocalVariable(variable);
                                for (PsiAssignmentExpression assignment : localVariableAssignments) {
                                    PsiJavaToken operator = assignment.getOperationSign();
                                    if (!operator.equals(JavaTokenType.EQ))
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
                            if (localVariableAssignments.isEmpty() && localVariablePostfixAssignments.isEmpty() && localVariablePrefixAssignments.isEmpty()) {
                                addUsedLocalVariable(variable);
                            }
                        }
                    }
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

    protected void processMethodInvocations(List<PsiExpression> methodInvocations) {
        for (PsiExpression expression : methodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                PsiMethod methodBinding = methodInvocation.resolveMethod();
                String originClassName = null;
                if (methodBinding != null) {
                    originClassName = methodBinding.getContainingClass().getName();
                }
                TypeObject originClassTypeObject = TypeObject.extractTypeObject(originClassName);
                String methodInvocationName = methodBinding.getName();
                String qualifiedName = methodBinding.getName();
                PsiType returnTypeBinding = methodBinding.getReturnType();
                TypeObject returnType = TypeObject.extractTypeObject(returnTypeBinding == null ? "Object" : returnTypeBinding.getCanonicalText());
                MethodInvocationObject methodInvocationObject = new MethodInvocationObject(originClassTypeObject, methodInvocationName, returnType);
                methodInvocationObject.setMethodInvocation(methodInvocation);
                PsiParameter[] parameterTypes = methodBinding.getParameterList().getParameters();
                for (PsiParameter parameterType : parameterTypes) {
                    String qualifiedParameterName = parameterType.getName();
                    TypeObject typeObject = TypeObject.extractTypeObject(parameterType.getType().getCanonicalText());
                    methodInvocationObject.addParameter(typeObject);
                }
                JvmReferenceType[] thrownExceptionTypes = methodBinding.getThrowsTypes();
                for (JvmReferenceType thrownExceptionType : thrownExceptionTypes) {
                    methodInvocationObject.addThrownException(thrownExceptionType.getName());
                }
                if ((methodBinding.hasModifier(JvmModifier.STATIC)))
                    methodInvocationObject.setStatic(true);
                addMethodInvocation(methodInvocationObject);
                AbstractVariable invoker = MethodDeclarationUtility.processMethodInvocationExpression(methodInvocation.getMethodExpression());
                if (invoker != null) {
                    PlainVariable initialVariable = invoker.getInitialVariable();
                    if (initialVariable.isField()) {
                        //addInvokedMethodThroughField(invoker, methodInvocationObject);
                        addNonDistinctInvokedMethodThroughField(invoker, methodInvocationObject);
                    } else if (initialVariable.isParameter()) {
                        //addInvokedMethodThroughParameter(invoker, methodInvocationObject);
                        addNonDistinctInvokedMethodThroughParameter(invoker, methodInvocationObject);
                    } else {
                        //addInvokedMethodThroughLocalVariable(invoker, methodInvocationObject);
                        addNonDistinctInvokedMethodThroughLocalVariable(invoker, methodInvocationObject);
                    }
                } else {
                    if (methodInvocationObject.isStatic())
                        addStaticallyInvokedMethod(methodInvocationObject);
                    else {
                        methodInvocation.getMethodExpression();
                        if (methodInvocation.getMethodExpression() instanceof PsiThisExpression) {
                            //addInvokedMethodThroughThisReference(methodInvocationObject);
                            addNonDistinctInvokedMethodThroughThisReference(methodInvocationObject);
                        }
                    }
                }
                List<PsiExpression> arguments = Arrays.asList(methodInvocation.getArgumentList().getExpressions());
                for (PsiExpression argument : arguments) {
                    if (argument instanceof PsiReferenceExpression) {
                        PsiReferenceExpression argumentName = (PsiReferenceExpression) argument;
                    }
                }
            } else if (expression instanceof PsiSuperExpression) {
                PsiSuperExpression superMethodInvocation = (PsiSuperExpression) expression;
                PsiElement methodBinding = superMethodInvocation.getReference().getElement();
                if (!(methodBinding instanceof PsiMethod)) return;
                PsiMethod methodBinding1 = (PsiMethod) superMethodInvocation.getReference().getElement();
                String originClassName = superMethodInvocation.getClass().getName();
                TypeObject originClassTypeObject = TypeObject.extractTypeObject(originClassName);
                String methodInvocationName = superMethodInvocation.getReference().getElement().getText();
                String qualifiedName = methodBinding1.getReturnType().getCanonicalText();
                TypeObject returnType = TypeObject.extractTypeObject(qualifiedName);
                SuperMethodInvocationObject superMethodInvocationObject = new SuperMethodInvocationObject(originClassTypeObject, methodInvocationName, returnType);
                superMethodInvocationObject.setSuperMethodInvocation(superMethodInvocation);
                PsiParameter[] parameterTypes = methodBinding1.getParameterList().getParameters();
                for (PsiParameter parameterType : parameterTypes) {
                    String qualifiedParameterName = parameterType.getName();
                    TypeObject typeObject = TypeObject.extractTypeObject(qualifiedParameterName);
                    superMethodInvocationObject.addParameter(typeObject);
                }
                JvmReferenceType[] thrownExceptionTypes = methodBinding1.getThrowsTypes();
                for (JvmReferenceType thrownExceptionType : thrownExceptionTypes) {
                    superMethodInvocationObject.addThrownException(thrownExceptionType.getName());
                }
                if ((methodBinding1.hasModifier(JvmModifier.STATIC)))
                    superMethodInvocationObject.setStatic(true);
                addSuperMethodInvocation(superMethodInvocationObject);
            }
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

    protected void processClassInstanceCreations(List<PsiExpression> classInstanceCreations) {
        for (PsiExpression classInstanceCreationExpression : classInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) classInstanceCreationExpression;
            if (classInstanceCreation != null && classInstanceCreationExpression.getOriginalElement() instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) classInstanceCreationExpression.getOriginalElement();
                TypeObject typeObject = null;
                if (classInstanceCreation.getType() != null) {
                    typeObject = TypeObject.extractTypeObject(classInstanceCreation.getType().getCanonicalText());
                }
                ClassInstanceCreationObject creationObject = new ClassInstanceCreationObject(typeObject);
                creationObject.setClassInstanceCreation((PsiClass) classInstanceCreation.getOriginalElement());
                if (psiClass.getTypeParameterList() != null) {
                    for (PsiTypeParameter parameterType : psiClass.getTypeParameterList().getTypeParameters()) {
                        String qualifiedParameterName = parameterType.getQualifiedName() == null ? "Object" : parameterType.getQualifiedName();
                        TypeObject parameterTypeObject = TypeObject.extractTypeObject(qualifiedParameterName);
                        creationObject.addParameter(parameterTypeObject);
                    }
                }

                PsiAnonymousClass anonymous = classInstanceCreation.getAnonymousClass();
                if (anonymous != null) {
                    final AnonymousClassDeclarationObject anonymousClassObject = new AnonymousClassDeclarationObject();
                    // ITypeBinding anonymousTypeBinding = anonymous.resolveBinding();
                    if (anonymous.getName() != null) {
                        anonymousClassObject.setName(anonymous.getName());
                    } else {
                        String anonymousKey = anonymous.getName();
                        String formattedKey = anonymousKey.substring(1, anonymousKey.length() - 1).replaceAll("/", ".");
                        anonymousClassObject.setName(formattedKey);
                    }
                    anonymousClassObject.setAnonymousClassDeclaration(anonymous);
                    PsiField[] fields = anonymous.getFields();
                    PsiMethod[] methods = anonymous.getMethods();

                    for (PsiField psiField : fields) {
                        TypeObject typeObject2 = TypeObject.extractTypeObject(psiField.getType().getCanonicalText());
                        typeObject2.setArrayDimension(typeObject2.getArrayDimension());
                        FieldObject fieldObject = new FieldObject(typeObject2, psiField);
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
                        String methodName = psiMethod.getName();
                        final ConstructorObject constructorObject = new ConstructorObject();
                        constructorObject.setMethodDeclaration(psiMethod);
                        constructorObject.setName(methodName);
                        constructorObject.setClassName(anonymousClassObject.getName());

                        if ((psiMethod.hasModifier(JvmModifier.PUBLIC)))
                            constructorObject.setAccess(Access.PUBLIC);
                        else if ((psiMethod.hasModifier(JvmModifier.PROTECTED)))
                            constructorObject.setAccess(Access.PROTECTED);
                        else if ((psiMethod.hasModifier(JvmModifier.PRIVATE)))
                            constructorObject.setAccess(Access.PRIVATE);
                        else
                            constructorObject.setAccess(Access.NONE);

                        List<PsiParameter> parameters = Arrays.asList(psiMethod.getParameterList().getParameters());
                        for (PsiParameter parameter : parameters) {
                            TypeObject typeObject2 = TypeObject.extractTypeObject(Objects.requireNonNull(parameter.getName()));
                            typeObject2.setArrayDimension(typeObject2.getArrayDimension());
                            if (parameter.isVarArgs()) {
                                typeObject2.setArrayDimension(1);
                            }
                            ParameterObject parameterObject = new ParameterObject(typeObject2, parameter.getName(), parameter.isVarArgs());
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
                        extendedModifiers.getAnnotations();
                        PsiAnnotation[] annotations = extendedModifiers.getAnnotations();
                        for (PsiAnnotation ann : annotations) {
                            if (Objects.equals(ann.getQualifiedName(), "Test")) {
                                methodObject.setTestAnnotation(true);
                                break;
                            }
                        }
                        PsiType returnType = psiMethod.getReturnType();
                        TypeObject typeObject2 = TypeObject.extractTypeObject(returnType.getCanonicalText());
                        methodObject.setReturnType(typeObject2);
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
                    addAnomymousClassDeclaration(anonymousClassObject);

                }
            }
        }
    }

    protected void processArrayCreations(List<PsiExpression> arrayCreations) {
        for (PsiExpression arrayCreationExpression : arrayCreations) {
            PsiNewExpression arrayCreation = (PsiNewExpression) arrayCreationExpression;
            TypeObject typeObject = TypeObject.extractTypeObject(arrayCreation.getText());
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

    protected void processLiterals(List<PsiExpression> literals) {
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

    private void addAnomymousClassDeclaration(AnonymousClassDeclarationObject anonymousClassObject) {
        anonymousClassDeclarationList.add(anonymousClassObject);
        if (parent != null) {
            parent.addAnomymousClassDeclaration(anonymousClassObject);
        }
    }

    protected void processThrowStatement(PsiThrowStatement throwStatement) {
        PsiExpression expression = throwStatement.getException();
        addExceptionInThrowStatement(expression);
    }

    private void addExceptionInThrowStatement(PsiExpression exception) {
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
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<MethodInvocationObject>();
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
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<MethodInvocationObject>();
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
            ArrayList<MethodInvocationObject> methodInvocations = new ArrayList<MethodInvocationObject>();
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
            LinkedHashSet<MethodInvocationObject> methodInvocations = new LinkedHashSet<MethodInvocationObject>();
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
            LinkedHashSet<SuperMethodInvocationObject> methodInvocations = new LinkedHashSet<SuperMethodInvocationObject>();
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
            LinkedHashSet<ConstructorInvocationObject> constructorInvocations = new LinkedHashSet<ConstructorInvocationObject>();
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
            LinkedHashSet<ClassInstanceCreationObject> classInstanceCreations = new LinkedHashSet<ClassInstanceCreationObject>();
            classInstanceCreations.add(classInstanceCreation);
            variablesAssignedWithClassInstanceCreations.put(variable, classInstanceCreations);
        }
        if (parent != null) {
            parent.addVariableAssignedWithClassInstanceCreation(variable, classInstanceCreation);
        }
    }

    private Set<PsiAssignmentExpression> getMatchingAssignments(PsiElement simpleName, List<PsiExpression> assignments) {
        Set<PsiAssignmentExpression> matchingAssignments = new LinkedHashSet<PsiAssignmentExpression>();
        for (PsiExpression expression : assignments) {
            if (expression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                PsiExpression leftHandSide = assignment.getLExpression();
                if (leftHandSide.equals(simpleName)) {
                    matchingAssignments.add(assignment);
                }
            }
        }
        return matchingAssignments;
    }

    private Set<PsiPostfixExpression> getMatchingPostfixAssignments(PsiElement simpleName, List<PsiExpression> postfixExpressions) {
        Set<PsiPostfixExpression> matchingPostfixAssignments = new LinkedHashSet<PsiPostfixExpression>();
        for (PsiExpression expression : postfixExpressions) {
            if (expression instanceof PsiPostfixExpression) {
                PsiPostfixExpression postfixExpression = (PsiPostfixExpression) expression;
                PsiExpression operand = postfixExpression.getOperand();
                PsiElement operandName = operand.getOriginalElement();
                if (operandName != null && operandName.equals(simpleName)) {
                    matchingPostfixAssignments.add(postfixExpression);
                }
            }
        }
        return matchingPostfixAssignments;
    }

    private Set<PsiPrefixExpression> getMatchingPrefixAssignments(PsiElement simpleName, List<PsiExpression> prefixExpressions) {
        Set<PsiPrefixExpression> matchingPrefixAssignments = new LinkedHashSet<PsiPrefixExpression>();
        for (PsiExpression expression : prefixExpressions) {
            if (expression instanceof PsiPrefixExpression) {
                PsiPrefixExpression prefixExpression = (PsiPrefixExpression) expression;
                PsiExpression operand = prefixExpression.getOperand();
                if (operand.equals(simpleName) &&
                        (PsiUtil.isIncrementDecrementOperation(operand))) {
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
                nonDistinctUsedFieldsThroughThisReference.add((PlainVariable) variable);
            } else {
                if (initialVariable.isField()) {
                    nonDistinctUsedFieldsThroughFields.add(variable);
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

    public List<ClassInstanceCreationObject> getClassInstanceCreations() {
        List<ClassInstanceCreationObject> classInstanceCreations = new ArrayList<ClassInstanceCreationObject>();
        for (CreationObject creation : creationList) {
            if (creation instanceof ClassInstanceCreationObject) {
                classInstanceCreations.add((ClassInstanceCreationObject) creation);
            }
        }
        return classInstanceCreations;
    }

    public List<ArrayCreationObject> getArrayCreations() {
        List<ArrayCreationObject> arrayCreations = new ArrayList<ArrayCreationObject>();
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

    public Set<PsiExpression> getExceptionsInThrowStatements() {
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
                new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughFields.keySet()) {
            invokedMethodsThroughFields.put(key, new LinkedHashSet<MethodInvocationObject>(nonDistinctInvokedMethodsThroughFields.get(key)));
        }
        return invokedMethodsThroughFields;
    }

    public Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters() {
        Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> invokedMethodsThroughParameters =
                new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughParameters.keySet()) {
            invokedMethodsThroughParameters.put(key, new LinkedHashSet<MethodInvocationObject>(nonDistinctInvokedMethodsThroughParameters.get(key)));
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
                new LinkedHashMap<AbstractVariable, LinkedHashSet<MethodInvocationObject>>();
        for (AbstractVariable key : nonDistinctInvokedMethodsThroughLocalVariables.keySet()) {
            invokedMethodsThroughLocalVariables.put(key, new LinkedHashSet<MethodInvocationObject>(nonDistinctInvokedMethodsThroughLocalVariables.get(key)));
        }
        return invokedMethodsThroughLocalVariables;
    }

    public Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughLocalVariables() {
        return nonDistinctInvokedMethodsThroughLocalVariables;
    }

    public Set<MethodInvocationObject> getInvokedMethodsThroughThisReference() {
        return new LinkedHashSet<MethodInvocationObject>(nonDistinctInvokedMethodsThroughThisReference);
    }

    public List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference() {
        return nonDistinctInvokedMethodsThroughThisReference;
    }

    public Set<MethodInvocationObject> getInvokedStaticMethods() {
        return new LinkedHashSet<MethodInvocationObject>(nonDistinctInvokedStaticMethods);
    }

    public List<MethodInvocationObject> getNonDistinctInvokedStaticMethods() {
        return nonDistinctInvokedStaticMethods;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughFields() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctDefinedFieldsThroughFields);
    }

    public Set<AbstractVariable> getUsedFieldsThroughFields() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctUsedFieldsThroughFields);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields() {
        return nonDistinctDefinedFieldsThroughFields;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughFields() {
        return nonDistinctUsedFieldsThroughFields;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughParameters() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctDefinedFieldsThroughParameters);
    }

    public Set<AbstractVariable> getUsedFieldsThroughParameters() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctUsedFieldsThroughParameters);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters() {
        return nonDistinctDefinedFieldsThroughParameters;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters() {
        return nonDistinctUsedFieldsThroughParameters;
    }

    public Set<AbstractVariable> getDefinedFieldsThroughLocalVariables() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctDefinedFieldsThroughLocalVariables);
    }

    public Set<AbstractVariable> getUsedFieldsThroughLocalVariables() {
        return new LinkedHashSet<AbstractVariable>(nonDistinctUsedFieldsThroughLocalVariables);
    }

    public List<AbstractVariable> getNonDistinctDefinedFieldsThroughLocalVariables() {
        return nonDistinctDefinedFieldsThroughLocalVariables;
    }

    public List<AbstractVariable> getNonDistinctUsedFieldsThroughLocalVariables() {
        return nonDistinctUsedFieldsThroughLocalVariables;
    }

    public Set<PlainVariable> getDefinedFieldsThroughThisReference() {
        return new LinkedHashSet<PlainVariable>(nonDistinctDefinedFieldsThroughThisReference);
    }

    public List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference() {
        return nonDistinctDefinedFieldsThroughThisReference;
    }

    public Set<PlainVariable> getUsedFieldsThroughThisReference() {
        return new LinkedHashSet<PlainVariable>(nonDistinctUsedFieldsThroughThisReference);
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
