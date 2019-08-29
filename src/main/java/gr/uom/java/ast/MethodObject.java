package gr.uom.java.ast;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.decomposition.AbstractStatement;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.decomposition.StatementObject;
import gr.uom.java.ast.decomposition.cfg.AbstractVariable;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.MethodDeclarationUtility;

import java.util.*;

public class MethodObject implements AbstractMethodDeclaration {

    private TypeObject returnType;
    private boolean _abstract;
    private boolean _static;
    private boolean _synchronized;
    private boolean _native;
    private final ConstructorObject constructorObject;
    private boolean testAnnotation;
    private volatile int hashCode = 0;
    private final PsiMethod psiMethod;

    public MethodObject(PsiMethod psiMethod, ConstructorObject co) {
        this.constructorObject = co;
        this._abstract = false;
        this._static = false;
        this._synchronized = false;
        this._native = false;
        this.testAnnotation = false;
        this.psiMethod = psiMethod;
    }

    public PsiMethod getPsiMethod() {
        return this.psiMethod;
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

    public boolean hasTestAnnotation() {
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
        return new MethodInvocationObject(TypeObject.extractTypeObject(this.constructorObject.className), this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public SuperMethodInvocationObject generateSuperMethodInvocation() {
        return new SuperMethodInvocationObject(TypeObject.extractTypeObject(this.constructorObject.className), this.constructorObject.name, this.returnType, this.constructorObject.getParameterTypeList());
    }

    public FieldInstructionObject isGetter() {
        if (getMethodBody() != null) {
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                PsiStatement statement = statementObject.getStatement();
                if (statement instanceof PsiReturnStatement) {
                    PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
                    if ((returnStatement instanceof PsiReferenceExpression)
                            && statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 0 &&
                            statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 0 && this.constructorObject.parameterList.size() == 0) {
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
                PsiStatement statement = statementObject.getStatement();
                if (statement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
                    if (expressionStatement.getExpression() instanceof PsiAssignmentExpression && statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 0 &&
                            statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 1 && this.constructorObject.parameterList.size() == 1) {
                        PsiAssignmentExpression assignment = (PsiAssignmentExpression) expressionStatement.getExpression();
                        if ((assignment.getLExpression() instanceof PsiReferenceExpression || assignment.getLExpression() instanceof PsiReferenceExpression) && assignment.getRExpression() instanceof PsiReferenceExpression)
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
                if (statementObject.getFieldInstructions().size() == 1 && statementObject.getMethodInvocations().size() == 1 &&
                        statementObject.getLocalVariableDeclarations().size() == 0 && statementObject.getLocalVariableInstructions().size() == 1 && this.constructorObject.parameterList.size() == 1) {
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
            PsiElement parentClass = methodDeclaration.getParent();
            List<AbstractStatement> abstractStatements = getMethodBody().getCompositeStatement().getStatements();
            if (abstractStatements.size() == 1 && abstractStatements.get(0) instanceof StatementObject) {
                StatementObject statementObject = (StatementObject) abstractStatements.get(0);
                PsiStatement statement = statementObject.getStatement();
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
                    PsiExpression methodInvocationExpression = methodInvocation.getMethodExpression();
                    List<MethodInvocationObject> methodInvocations = statementObject.getMethodInvocations();
                    if (methodInvocationExpression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression previousChainedMethodInvocation = (PsiMethodCallExpression) methodInvocationExpression;
                        List<PsiMethod> parentClassMethods = new ArrayList<>();
                        if (parentClass instanceof PsiClass) {
                            PsiMethod[] parentClassMethodArray = ((PsiClass) parentClass).getMethods();
                            parentClassMethods.addAll(Arrays.asList(parentClassMethodArray));
                        }
                        boolean isDelegationChain = false;
                        boolean foundInParentClass = false;
                        for (PsiMethod parentClassMethod : parentClassMethods) {
                            if (parentClassMethod.getReference().equals(previousChainedMethodInvocation.getReference())) {
                                foundInParentClass = true;
                                PsiElement getterField = MethodDeclarationUtility.isGetter(parentClassMethod);
                                if (getterField == null)
                                    isDelegationChain = true;
                                break;
                            }
                        }
                        if (!isDelegationChain && foundInParentClass) {
                            for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                                if (methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
                                    return methodInvocationObject;
                                }
                            }
                        }
                    } else if (methodInvocationExpression instanceof PsiReferenceExpression) {
                        PsiReferenceExpression fieldAccess = (PsiReferenceExpression) methodInvocationExpression;
                        PsiElement variableBinding = fieldAccess.resolve();
                        if (variableBinding == null) return null;
                        if (variableBinding.getClass().equals(parentClass.getClass()))
                        //|| parentClass.getClass().is(variableBinding.getClass()))
                        {
                            for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                                if (methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
                                    return methodInvocationObject;
                                }
                            }
                        }
                    } else if (methodInvocationExpression instanceof PsiThisExpression) {
                        for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                            if (methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
                                return methodInvocationObject;
                            }
                        }
                    } else if (methodInvocationExpression == null) {
                        for (MethodInvocationObject methodInvocationObject : methodInvocations) {
                            if (methodInvocationObject.getMethodInvocation().equals(methodInvocation)) {
                                return methodInvocationObject;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    //TODO: Fix it
    public boolean validTargetObject(ClassObject sourceClass, ClassObject targetClass) {
        ASTInformation targetClassBinding = targetClass.getAbstractTypeDeclaration();
        List<LocalVariableInstructionObject> localVariableInstructions = getLocalVariableInstructions();
        for (LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
            if (localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
                for (LocalVariableDeclarationObject variableDeclaration : getLocalVariableDeclarations()) {
                    if (variableDeclaration.getVariableDeclaration().equals(
                            localVariableInstruction.getSimpleName().resolve()))
                        return false;
                }
            }
        }
        for (LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
            if (localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
                ListIterator<ParameterObject> parameterIterator = getParameterListIterator();
                while (parameterIterator.hasNext()) {
                    ParameterObject parameter = parameterIterator.next();
                    if (localVariableInstruction.getName().equals(parameter.getName()) && parameter.getType().getArrayDimension() == 0)
                        return true;
                }
            }
        }
        List<FieldInstructionObject> fieldInstructions = getFieldInstructions();
        for (FieldInstructionObject fieldInstruction : fieldInstructions) {
            PsiElement fieldTypeBinding = fieldInstruction.getSimpleName();
            if (fieldTypeBinding != null && fieldInstruction.getType() != null &&
                    fieldInstruction.getType().getClassType() != null && fieldInstruction.getType().getClassType().equals(targetClass.getName())
                    || targetClassBinding != null && fieldTypeBinding != null && fieldTypeBinding.equals(targetClassBinding.recoverASTNode())) {
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
                PsiClass classInstanceCreation = classInstanceCreationObject.getClassInstanceCreation();
                PsiMethod[] arguments = classInstanceCreation.getConstructors();
                for (PsiMethod argument : arguments) {
                    if ((Arrays.asList(argument.getBody().getStatements()).contains(this))
                            || (argument instanceof PsiThisExpression))
                        return true;
                }
            }
        }
        return false;
    }

    public boolean containsNullCheckForTargetObject(ClassObject targetClass) {
        List<LiteralObject> literals = getLiterals();
        for (LiteralObject literal : literals) {
            if (literal != null && (LiteralType.NULL).equals(literal.getLiteralType())) {
                PsiExpression nullLiteral = literal.getLiteral();
                if (nullLiteral instanceof PsiBinaryExpression) {
                    PsiBinaryExpression infixExpression = (PsiBinaryExpression) nullLiteral.getParent();
                    PsiExpression leftOperand = infixExpression.getLOperand();
                    if (leftOperand.getOriginalElement().getClass().getName().equals(targetClass.getName()) && infixExpression.getOperationSign().equals(JavaTokenType.EQEQ)) {
                        if (leftOperand instanceof PsiReferenceExpression) {
                            if (PsiTreeUtil.findChildrenOfType(leftOperand, PsiField.class).size() > 0) return true;
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

    public ListIterator<CommentObject> getCommentListIterator() {
        return constructorObject.getCommentListIterator();
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

    public List<ConstructorInvocationObject> getConstructorInvocations() {
        return constructorObject.getConstructorInvocations();
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

    public List<LiteralObject> getLiterals() {
        return constructorObject.getLiterals();
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return constructorObject.getAnonymousClassDeclarations();
    }

    public Set<PsiExpression> getExceptionsInThrowStatements() {
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

    public Map<PlainVariable, LinkedHashSet<ConstructorInvocationObject>> getParametersPassedAsArgumentsInConstructorInvocations() {
        return constructorObject.getParametersPassedAsArgumentsInConstructorInvocations();
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
        /*this.constructorObject.getParameterTypeList().equals(mio.getParameterTypeList());*/
    }

    public boolean equals(SuperMethodInvocationObject smio) {
        return this.getClassName().equals(smio.getOriginClassName()) && this.getName().equals(smio.getMethodName()) &&
                this.returnType.equalsClassType(smio.getReturnType()) && equalParameterTypes(this.constructorObject.getParameterTypeList(), smio.getParameterTypeList());
        /*this.constructorObject.getParameterTypeList().equals(smio.getParameterTypeList());*/
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
}