package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.javadoc.PsiDocMethodOrFieldRef;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;
import org.jetbrains.research.intellijdeodorant.core.ast.util.StatementExtractor;
import org.jetbrains.research.intellijdeodorant.inheritance.CompleteInheritanceDetection;
import org.jetbrains.research.intellijdeodorant.inheritance.InheritanceTree;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typestatechecking.TypeCheckElimination;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typestatechecking.TypeCheckEliminationGroup;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.stream.Collectors;

public class SystemObject {
    private static final String TYPE_STATE_CHECKING_INDICATOR_KEY = "type.state.checking.identification.indicator";

    private final List<ClassObject> classList;
    //Map that has as key the classname and as value
    //the position of className in the classNameList
    private final Map<String, Integer> classNameMap;
    private final Map<MethodInvocationObject, FieldInstructionObject> getterMap;
    private final Map<MethodInvocationObject, FieldInstructionObject> setterMap;
    private final Map<MethodInvocationObject, FieldInstructionObject> collectionAdderMap;
    private final Map<MethodInvocationObject, MethodInvocationObject> delegateMap;

    public SystemObject() {
        this.classList = new ArrayList<>();
        this.classNameMap = new HashMap<>();
        this.getterMap = new LinkedHashMap<>();
        this.setterMap = new LinkedHashMap<>();
        this.collectionAdderMap = new LinkedHashMap<>();
        this.delegateMap = new LinkedHashMap<>();
    }

    public void addClass(ClassObject c) {
        classNameMap.put(c.getName(), classList.size());
        classList.add(c);
    }

    public void addClasses(List<ClassObject> classObjects) {
        for (ClassObject classObject : classObjects)
            addClass(classObject);
    }

    public void replaceClass(ClassObject c) {
        int position = getPositionInClassList(c.getName());
        if (position != -1) {
            classList.set(position, c);
        } else {
            addClass(c);
        }
    }

    public void removeClass(ClassObject c) {
        int position = getPositionInClassList(c.getName());
        if (position != -1) {
            for (int i = position + 1; i < classList.size(); i++) {
                ClassObject classObject = classList.get(i);
                classNameMap.put(classObject.getName(), classNameMap.get(classObject.getName()) - 1);
            }
            classNameMap.remove(c.getName());
            classList.remove(c);
        }
    }

    public void addGetter(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
        getterMap.put(methodInvocation, fieldInstruction);
    }

    public void addSetter(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
        setterMap.put(methodInvocation, fieldInstruction);
    }

    public void addCollectionAdder(MethodInvocationObject methodInvocation, FieldInstructionObject fieldInstruction) {
        collectionAdderMap.put(methodInvocation, fieldInstruction);
    }

    public void addDelegate(MethodInvocationObject methodInvocation, MethodInvocationObject delegation) {
        delegateMap.put(methodInvocation, delegation);
    }

    public FieldInstructionObject containsGetter(MethodInvocationObject methodInvocation) {
        return getterMap.get(methodInvocation);
    }

    public FieldInstructionObject containsSetter(MethodInvocationObject methodInvocation) {
        return setterMap.get(methodInvocation);
    }

    public FieldInstructionObject containsCollectionAdder(MethodInvocationObject methodInvocation) {
        return collectionAdderMap.get(methodInvocation);
    }

    public MethodInvocationObject containsDelegate(MethodInvocationObject methodInvocation) {
        return delegateMap.get(methodInvocation);
    }

    public MethodObject getMethod(MethodInvocationObject mio) {
        ClassObject classObject = getClassObject(mio.getOriginClassName());
        if (classObject != null)
            return classObject.getMethod(mio);
        return null;
    }

    public MethodObject getMethod(SuperMethodInvocationObject smio) {
        ClassObject classObject = getClassObject(smio.getOriginClassName());
        if (classObject != null)
            return classObject.getMethod(smio);
        return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, ClassObject excludedClass) {
        for (ClassObject classObject : classList) {
            if (!excludedClass.equals(classObject) && classObject.containsMethodInvocation(methodInvocation))
                return true;
        }
        return false;
    }

    public boolean containsFieldInstruction(FieldInstructionObject fieldInstruction, ClassObject excludedClass) {
        for (ClassObject classObject : classList) {
            if (!excludedClass.equals(classObject) && classObject.containsFieldInstruction(fieldInstruction))
                return true;
        }
        return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        for (ClassObject classObject : classList) {
            if (classObject.containsSuperMethodInvocation(superMethodInvocation))
                return true;
        }
        return false;
    }

    public ClassObject getClassObject(String className) {
        Integer pos = classNameMap.get(className);
        if (pos != null)
            return getClassObject(pos);
        else
            return null;
    }

    private ClassObject getClassObject(int pos) {
        return classList.get(pos);
    }

    public ListIterator<ClassObject> getClassListIterator() {
        return classList.listIterator();
    }

    public int getClassNumber() {
        return classList.size();
    }

    private int getPositionInClassList(String className) {
        Integer pos = classNameMap.get(className);
        if (pos != null)
            return pos;
        else
            return -1;
    }

    public Set<ClassObject> getClassObjects() {
        return new LinkedHashSet<>(classList);
    }

    public List<String> getClassNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < classList.size(); i++) {
            names.add(getClassObject(i).getName());
        }
        return names;
    }

    public List<TypeCheckEliminationGroup> generateTypeCheckEliminations(Set<ClassObject> classObjectsToBeExamined, ProgressIndicator indicator) {
        indicator.setText(IntelliJDeodorantBundle.message(TYPE_STATE_CHECKING_INDICATOR_KEY));
        indicator.setFraction(0.0);
        List<TypeCheckElimination> typeCheckEliminationResults = new ArrayList<>();
        List<TypeCheckEliminationGroup> typeCheckEliminationGroups = new ArrayList<>();
        Map<TypeCheckElimination, List<PsiField>> staticFieldMap = new LinkedHashMap<>();
        Map<Integer, ArrayList<TypeCheckElimination>> staticFieldRankMap = new TreeMap<>();
        Map<String, ArrayList<TypeCheckElimination>> inheritanceTreeMap = new LinkedHashMap<>();
        CompleteInheritanceDetection inheritanceDetection = new CompleteInheritanceDetection(this);
        for (ClassObject classObject : classObjectsToBeExamined) {
            List<TypeCheckElimination> eliminations = classObject.generateTypeCheckEliminations();
            for (TypeCheckElimination elimination : eliminations) {
                List<PsiField> staticFields = elimination.getStaticFields();
                if (!staticFields.isEmpty()) {
                    if (allStaticFieldsWithinSystemBoundary(staticFields)) {
                        inheritanceHierarchyMatchingWithStaticTypes(elimination, inheritanceDetection);
                        boolean isValid = false;
                        if (elimination.getTypeField() != null) {
                            PsiType psiType = elimination.getTypeField().getType();
                            if (validType(psiType)) {
                                isValid = true;
                            }
                        } else if (elimination.getTypeLocalVariable() != null) {
                            PsiType psiType = elimination.getTypeLocalVariable().getType();
                            if (validType(psiType)) {
                                isValid = true;
                            }
                        } else if (elimination.getTypeMethodInvocation() != null) {
                            PsiMethodCallExpression methodCallExpression = elimination.getTypeMethodInvocation();
                            PsiMethod invokedMethod = methodCallExpression.resolveMethod();
                            PsiClass containingClass = invokedMethod.getContainingClass();
                            PsiType methodReturnType = invokedMethod.getReturnType();
                            ClassObject declaringClassObject = getClassObject(containingClass.getQualifiedName());
                            if (validType(methodReturnType) && declaringClassObject != null) {
                                PsiExpression fieldInstruction = MethodDeclarationUtility.isGetter(invokedMethod);
                                if (fieldInstruction != null) {
                                    ListIterator<FieldObject> fieldIterator = declaringClassObject.getFieldIterator();
                                    while (fieldIterator.hasNext()) {
                                        FieldObject fieldObject = fieldIterator.next();
                                        PsiField declaredField = fieldObject.getVariableDeclaration();
                                        if (!(fieldInstruction instanceof PsiReferenceExpression)) {
                                            continue;
                                        }
                                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) fieldInstruction;
                                        if (declaredField.equals(referenceExpression.resolve())) {
                                            elimination.setForeignTypeField(declaredField);
                                            break;
                                        }
                                    }
                                    isValid = true;
                                } else if (invokedMethod.getBody() == null) {
                                    InheritanceTree tree = elimination.getInheritanceTreeMatchingWithStaticTypes();
                                    PsiType invokerTypeBinding = handleTypeMethodInvocation(methodCallExpression, elimination);
                                    if (invokerTypeBinding != null) {
                                        if (tree != null) {
                                            if (invokerTypeBinding.getCanonicalText().equals(tree.getRootNode().getUserObject())) {
                                                elimination.setExistingInheritanceTree(tree);
                                                if (inheritanceTreeMap.containsKey(tree.getRootNode().getUserObject())) {
                                                    ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree.getRootNode().getUserObject());
                                                    typeCheckEliminations.add(elimination);
                                                } else {
                                                    ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
                                                    typeCheckEliminations.add(elimination);
                                                    inheritanceTreeMap.put((String) tree.getRootNode().getUserObject(), typeCheckEliminations);
                                                }
                                            }
                                        } else {
                                            InheritanceTree tree2 = inheritanceDetection.getTree(invokerTypeBinding.getCanonicalText());
                                            if (tree2 != null) {
                                                elimination.setExistingInheritanceTree(tree2);
                                                if (inheritanceTreeMap.containsKey(tree2.getRootNode().getUserObject())) {
                                                    ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree2.getRootNode().getUserObject());
                                                    typeCheckEliminations.add(elimination);
                                                } else {
                                                    ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
                                                    typeCheckEliminations.add(elimination);
                                                    inheritanceTreeMap.put((String) tree2.getRootNode().getUserObject(), typeCheckEliminations);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (isValid) {
                            staticFieldMap.put(elimination, staticFields);
                            int size = staticFields.size();
                            if (staticFieldRankMap.containsKey(size)) {
                                ArrayList<TypeCheckElimination> rank = staticFieldRankMap.get(size);
                                rank.add(elimination);
                            } else {
                                ArrayList<TypeCheckElimination> rank = new ArrayList<TypeCheckElimination>();
                                rank.add(elimination);
                                staticFieldRankMap.put(size, rank);
                            }
                        }
                    }
                } else {
                    if (elimination.getTypeField() != null) {
                        PsiField typeFieldBinding = elimination.getTypeField();
                        PsiType typeFieldTypeBinding = typeFieldBinding.getType();
                        InheritanceTree tree = inheritanceDetection.getTree(typeFieldTypeBinding.getCanonicalText());
                        elimination.setExistingInheritanceTree(tree);
                    } else if (elimination.getTypeLocalVariable() != null) {
                        PsiVariable typeLocalVariableBinding = elimination.getTypeLocalVariable();
                        PsiType typeLocalVariableTypeBinding = typeLocalVariableBinding.getType();
                        InheritanceTree tree = inheritanceDetection.getTree(typeLocalVariableTypeBinding.getCanonicalText());
                        elimination.setExistingInheritanceTree(tree);
                    } else if (elimination.getTypeMethodInvocation() != null) {
                        PsiMethodCallExpression typeMethodInvocation = elimination.getTypeMethodInvocation();
                        PsiMethod typeMethodInvocationBinding = typeMethodInvocation.resolveMethod();
                        if (typeMethodInvocationBinding.getContainingClass().getQualifiedName().equals("java.lang.Object") &&
                                typeMethodInvocationBinding.getName().equals("getClass")) {
                            PsiType invokerTypeBinding = handleTypeMethodInvocation(typeMethodInvocation, elimination);
                            if (invokerTypeBinding != null) {
                                InheritanceTree tree = inheritanceDetection.getTree(invokerTypeBinding.getCanonicalText());
                                elimination.setExistingInheritanceTree(tree);
                            }
                        } else {
                            PsiType typeMethodInvocationReturnType = typeMethodInvocationBinding.getReturnType();
                            InheritanceTree tree = inheritanceDetection.getTree(typeMethodInvocationReturnType.getCanonicalText());
                            elimination.setExistingInheritanceTree(tree);
                        }
                    }
                    if (elimination.getExistingInheritanceTree() != null) {
                        InheritanceTree tree = elimination.getExistingInheritanceTree();
                        if (inheritanceTreeMap.containsKey(tree.getRootNode().getUserObject())) {
                            ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(tree.getRootNode().getUserObject());
                            typeCheckEliminations.add(elimination);
                        } else {
                            ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<>();
                            typeCheckEliminations.add(elimination);
                            inheritanceTreeMap.put((String) tree.getRootNode().getUserObject(), typeCheckEliminations);
                        }
                    }
                }
            }
            indicator.setFraction(indicator.getFraction() + 1.0 / classObjectsToBeExamined.size());
        }
        for (String rootNode : inheritanceTreeMap.keySet()) {
            ArrayList<TypeCheckElimination> typeCheckEliminations = inheritanceTreeMap.get(rootNode);
            if (typeCheckEliminations.size() > 0) {
                typeCheckEliminationResults.addAll(typeCheckEliminations);
                typeCheckEliminationGroups.add(handleGroup(typeCheckEliminations));
            }
        }
        List<TypeCheckElimination> sortedEliminations = new ArrayList<>();
        List<Integer> keyList = new ArrayList<>(staticFieldRankMap.keySet());
        ListIterator<Integer> keyListIterator = keyList.listIterator(keyList.size());
        while (keyListIterator.hasPrevious()) {
            Integer states = keyListIterator.previous();
            sortedEliminations.addAll(staticFieldRankMap.get(states));
        }

        while (!sortedEliminations.isEmpty()) {
            TypeCheckElimination selectedElimination = sortedEliminations.get(0);
            List<TypeCheckElimination> affectedEliminations = new ArrayList<>();
            affectedEliminations.add(selectedElimination);
            List<PsiField> staticFieldUnion = staticFieldMap.get(selectedElimination);
            boolean staticFieldUnionIncreased = true;
            while (staticFieldUnionIncreased) {
                staticFieldUnionIncreased = false;
                for (TypeCheckElimination elimination : sortedEliminations) {
                    List<PsiField> staticFields = staticFieldMap.get(elimination);
                    if (!affectedEliminations.contains(elimination) && nonEmptyIntersection(staticFieldUnion, staticFields)) {
                        staticFieldUnion = constructUnion(staticFieldUnion, staticFields);
                        affectedEliminations.add(elimination);
                        staticFieldUnionIncreased = true;
                    }
                }
            }
            if (affectedEliminations.size() > 1) {
                for (TypeCheckElimination elimination : affectedEliminations) {
                    List<PsiField> staticFields = staticFieldMap.get(elimination);
                    for (PsiField simpleName1 : staticFieldUnion) {
                        boolean isContained = false;
                        for (PsiField simpleName2 : staticFields) {
                            if (simpleName1.equals(simpleName2)) {
                                isContained = true;
                                break;
                            }
                        }
                        if (!isContained)
                            elimination.addAdditionalStaticField(simpleName1);
                    }
                }
            }
            ArrayList<TypeCheckElimination> typeCheckEliminations = new ArrayList<TypeCheckElimination>();
            for (TypeCheckElimination elimination : affectedEliminations) {
                if (!elimination.isTypeCheckMethodStateSetter())
                    typeCheckEliminations.add(elimination);
            }
            if (typeCheckEliminations.size() > 0) {
                typeCheckEliminationResults.addAll(typeCheckEliminations);
                typeCheckEliminationGroups.add(handleGroup(typeCheckEliminations));
            }
            sortedEliminations.removeAll(affectedEliminations);
        }
        identifySuperFieldAccessorMethods(typeCheckEliminationResults);
        indicator.setFraction(1);
        return typeCheckEliminationGroups;
    }

    private boolean validType(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            return true;
        }
        if (!(type instanceof PsiClassType)) {
            return false;
        }
        PsiClass resolvedClass = ((PsiClassType) type).resolve();
        if (resolvedClass == null) {
            return false;
        }
        return resolvedClass.isEnum() || resolvedClass.getQualifiedName().equals("java.lang.String");
    }

    private PsiType handleTypeMethodInvocation(PsiMethodCallExpression typeMethodInvocation, TypeCheckElimination elimination) {
        PsiExpression typeMethodInvocationExpression = typeMethodInvocation.getMethodExpression().getQualifierExpression();
        PsiClass typeCheckClassBinding = elimination.getTypeCheckClass();
        ClassObject typeCheckClassObject = getClassObject(typeCheckClassBinding.getQualifiedName());
        PsiReferenceExpression invoker = null;
        if (typeMethodInvocationExpression instanceof PsiReferenceExpression) {
            invoker = (PsiReferenceExpression) typeMethodInvocationExpression;
        }
        if (invoker != null) {
            PsiElement binding = invoker.resolve();
            if (binding instanceof PsiVariable) {
                PsiVariable variableBinding = (PsiVariable) binding;
                if (variableBinding instanceof PsiField) {
                    ListIterator<FieldObject> fieldIterator = typeCheckClassObject.getFieldIterator();
                    while (fieldIterator.hasNext()) {
                        FieldObject fieldObject = fieldIterator.next();
                        PsiField fragment = fieldObject.getVariableDeclaration();
                        if (variableBinding.equals(fragment)) {
                            elimination.setTypeField(fragment);
                            break;
                        }
                    }
                } else if (variableBinding instanceof PsiParameter) {
                    PsiParameter[] parameters = elimination.getTypeCheckMethodParameters();
                    for (PsiParameter parameter : parameters) {
                        if (parameter.equals(variableBinding)) {
                            elimination.setTypeLocalVariable(parameter);
                            break;
                        }
                    }
                } else {
                    StatementExtractor statementExtractor = new StatementExtractor();
                    ExpressionExtractor expressionExtractor = new ExpressionExtractor();
                    PsiCodeBlock typeCheckMethodBody = elimination.getTypeCheckMethod().getBody();
                    List<PsiVariable> variableDeclarationFragments = new ArrayList<>();
                    List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(typeCheckMethodBody);
                    for (PsiStatement statement : variableDeclarationStatements) {
                        PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                        PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                        List<PsiVariable> declaredVariables = Arrays.stream(fragments)
                                .map(element -> (PsiVariable) element)
                                .collect(Collectors.toList());
                        variableDeclarationFragments.addAll(declaredVariables);
                    }
                    for (PsiVariable fragment : variableDeclarationFragments) {
                        if (fragment.equals(variableBinding)) {
                            elimination.setTypeLocalVariable(fragment);
                            break;
                        }
                    }
                    List<PsiStatement> enhancedForStatements = statementExtractor.getEnhancedForStatements(typeCheckMethodBody);
                    for (PsiStatement eFStatement : enhancedForStatements) {
                        PsiForeachStatement enhancedForStatement = (PsiForeachStatement) eFStatement;
                        PsiParameter formalParameter = enhancedForStatement.getIterationParameter();
                        if (formalParameter.equals(variableBinding)) {
                            elimination.setTypeLocalVariable(formalParameter);
                            break;
                        }
                    }
                }
                return variableBinding.getType();
            }
        }
        return null;
    }

    private boolean nonEmptyIntersection(List<PsiField> staticFieldUnion, List<PsiField> staticFields) {
        for (PsiField simpleName1 : staticFields) {
            for (PsiField simpleName2 : staticFieldUnion) {
                if (simpleName1.equals(simpleName2))
                    return true;
            }
        }
        return false;
    }

    private List<PsiField> constructUnion(List<PsiField> staticFieldUnion, List<PsiField> staticFields) {
        List<PsiField> initialStaticFields = new ArrayList<>(staticFieldUnion);
        List<PsiField> staticFieldsToBeAdded = new ArrayList<>();
        for (PsiField simpleName1 : staticFields) {
            boolean isContained = false;
            for (PsiField simpleName2 : staticFieldUnion) {
                if (simpleName1.equals(simpleName2)) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained)
                staticFieldsToBeAdded.add(simpleName1);
        }
        initialStaticFields.addAll(staticFieldsToBeAdded);
        return initialStaticFields;
    }

    private void inheritanceHierarchyMatchingWithStaticTypes(TypeCheckElimination typeCheckElimination,
                                                             CompleteInheritanceDetection inheritanceDetection) {
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        String abstractClassType = typeCheckElimination.getAbstractClassType();
        InheritanceTree tree = null;
        if (abstractClassType != null) {
            tree = inheritanceDetection.getTree(abstractClassType);
        }
        if (tree != null) {
            DefaultMutableTreeNode rootNode = tree.getRootNode();
            DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
            List<String> inheritanceHierarchySubclassNames = new ArrayList<String>();
            while (leaf != null) {
                inheritanceHierarchySubclassNames.add((String) leaf.getUserObject());
                leaf = leaf.getNextLeaf();
            }
            int matchCounter = 0;
            for (PsiField staticField : staticFields) {
                for (String subclassName : inheritanceHierarchySubclassNames) {
                    ClassObject classObject = getClassObject(subclassName);
                    ASTInformation classDeclarationAstInformation = classObject.getAbstractTypeDeclaration();
                    PsiElement abstractTypeDeclaration = classDeclarationAstInformation.recoverASTNode();
                    if (abstractTypeDeclaration instanceof PsiClass) {
                        PsiClass typeDeclaration = (PsiClass) abstractTypeDeclaration;
                        PsiDocComment javadoc = typeDeclaration.getDocComment();
                        if (javadoc != null) {
                            PsiDocTag[] tagElements = javadoc.getTags();
                            for (PsiDocTag tagElement : tagElements) {
                                tagElement.getName();
                                if ("see".equals(tagElement.getName())) {
                                    PsiElement[] fragments = tagElement.getDataElements();
                                    for(PsiElement fragment : fragments) {
                                        if (!(fragment instanceof PsiDocMethodOrFieldRef)) {
                                            continue;
                                        }
                                        PsiReference memberRef = fragment.getReference();
                                        if (memberRef == null) {
                                            continue;
                                        }
                                        PsiElement resolvedRef = memberRef.resolve();
                                        if(staticField.equals(resolvedRef)) {
                                            typeCheckElimination.putStaticFieldSubclassTypeMapping(staticField, subclassName);
                                            matchCounter++;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (matchCounter == staticFields.size()) {
                typeCheckElimination.setInheritanceTreeMatchingWithStaticTypes(tree);
            }
        }
    }

    private boolean allStaticFieldsWithinSystemBoundary(List<PsiField> staticFields) {
        for (PsiField staticField : staticFields) {
            PsiClass declaringClassTypeBinding = staticField.getContainingClass();
            if (declaringClassTypeBinding != null) {
                if (getPositionInClassList(declaringClassTypeBinding.getQualifiedName()) == -1 && !declaringClassTypeBinding.isEnum())
                    return false;
            }
        }
        return true;
    }

    private void identifySuperFieldAccessorMethods(List<TypeCheckElimination> typeCheckEliminations) {
        for (TypeCheckElimination elimination : typeCheckEliminations) {
            Set<PsiField> superAccessedFields = elimination.getSuperAccessedFieldBindings();
            for (PsiField superAccessedField : superAccessedFields) {
                PsiClass declaringClassTypeBinding = superAccessedField.getContainingClass();
                ClassObject declaringClass = getClassObject(declaringClassTypeBinding.getQualifiedName());
                if (declaringClass != null) {
                    ListIterator<FieldObject> fieldIterator = declaringClass.getFieldIterator();
                    PsiField fieldFragment = null;
                    while (fieldIterator.hasNext()) {
                        FieldObject fieldObject = fieldIterator.next();
                        PsiField fragment = fieldObject.getVariableDeclaration();
                        if (fragment.equals(superAccessedField)) {
                            fieldFragment = fragment;
                            elimination.addSuperAccessedField(fragment, null);
                            break;
                        }
                    }
                    ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
                    while (methodIterator.hasNext()) {
                        MethodObject methodObject = methodIterator.next();
                        PsiMethod methodDeclaration = methodObject.getMethodDeclaration();
                        PsiExpression simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
                        if (simpleName instanceof PsiReferenceExpression
                                && superAccessedField.equals(((PsiReferenceExpression) simpleName).resolve())) {
                            elimination.addSuperAccessedFieldBinding(superAccessedField, methodDeclaration);
                            elimination.addSuperAccessedField(fieldFragment, methodDeclaration);
                            break;
                        }
                    }
                }
            }
            Set<PsiField> superAssignedFields = elimination.getSuperAssignedFieldBindings();
            for (PsiField superAssignedField : superAssignedFields) {
                PsiClass declaringClassTypeBinding = superAssignedField.getContainingClass();
                ClassObject declaringClass = getClassObject(declaringClassTypeBinding.getQualifiedName());
                if (declaringClass != null) {
                    ListIterator<FieldObject> fieldIterator = declaringClass.getFieldIterator();
                    PsiField fieldFragment = null;
                    while (fieldIterator.hasNext()) {
                        FieldObject fieldObject = fieldIterator.next();
                        PsiField fragment = fieldObject.getVariableDeclaration();
                        if (fragment.equals(superAssignedField)) {
                            fieldFragment = fragment;
                            elimination.addSuperAssignedField(fragment, null);
                            break;
                        }
                    }
                    ListIterator<MethodObject> methodIterator = declaringClass.getMethodIterator();
                    while (methodIterator.hasNext()) {
                        MethodObject methodObject = methodIterator.next();
                        PsiMethod methodDeclaration = methodObject.getMethodDeclaration();
                        PsiElement simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
                        if (superAssignedField.equals(simpleName)) {
                            elimination.addSuperAssignedFieldBinding(superAssignedField, methodDeclaration);
                            elimination.addSuperAssignedField(fieldFragment, methodDeclaration);
                            break;
                        }
                    }
                }
            }
        }
    }

    private TypeCheckEliminationGroup handleGroup(List<TypeCheckElimination> typeCheckEliminations) {
        TypeCheckEliminationGroup typeCheckEliminationGroup = new TypeCheckEliminationGroup();
        Map<String, ArrayList<TypeCheckElimination>> typeDeclarationMap = new HashMap<>();
        double averageNumberOfStatementsInGroupSum = 0;
        for (TypeCheckElimination elimination : typeCheckEliminations) {
            PsiClass typeCheckClass = elimination.getTypeCheckClass();
            String bindingKey = typeCheckClass.getQualifiedName();
            if (typeDeclarationMap.containsKey(bindingKey)) {
                ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(bindingKey);
                tempTypeCheckEliminations.add(elimination);
            } else {
                ArrayList<TypeCheckElimination> tempTypeCheckEliminations = new ArrayList<TypeCheckElimination>();
                tempTypeCheckEliminations.add(elimination);
                typeDeclarationMap.put(bindingKey, tempTypeCheckEliminations);
            }
            double avgNumberOfStatements = elimination.getAverageNumberOfStatements();
            averageNumberOfStatementsInGroupSum += avgNumberOfStatements;
            typeCheckEliminationGroup.addCandidate(elimination);
        }
        double averageGroupSizeAtClassLevelSum = 0;
        for (String bindingKey : typeDeclarationMap.keySet()) {
            ArrayList<TypeCheckElimination> tempTypeCheckEliminations = typeDeclarationMap.get(bindingKey);
            averageGroupSizeAtClassLevelSum += tempTypeCheckEliminations.size();
            for (TypeCheckElimination elimination : tempTypeCheckEliminations) {
                elimination.setGroupSizeAtClassLevel(tempTypeCheckEliminations.size());
            }
        }
        typeCheckEliminationGroup.setGroupSizeAtSystemLevel(typeCheckEliminations.size());
        typeCheckEliminationGroup.setAverageGroupSizeAtClassLevel(averageGroupSizeAtClassLevelSum / typeDeclarationMap.keySet().size());
        typeCheckEliminationGroup.setAverageNumberOfStatementsInGroup(averageNumberOfStatementsInGroupSum / typeCheckEliminations.size());
        return typeCheckEliminationGroup;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ClassObject classObject : classList) {
            sb.append(classObject.toString());
            sb.append("\n--------------------------------------------------------------------------------\n");
        }
        return sb.toString();
    }
}