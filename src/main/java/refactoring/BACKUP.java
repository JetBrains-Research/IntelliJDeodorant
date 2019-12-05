package refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.util.MethodDeclarationUtility;
import core.ast.util.TypeVisitor;
import core.ast.util.math.AdjacencyList;
import core.ast.util.math.Edge;
import core.ast.util.math.Node;
import core.ast.util.math.TarjanAlgorithm;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;

import java.util.*;

public class BACKUP {

    public ExtractClassRefactoring(PsiFile sourceFile, PsiClass sourceTypeDeclaration,
                                   Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods, Set<PsiMethod> delegateMethods, String extractedTypeName) {
        this.sourceFile = sourceFile;
        //this.sourceCompilationUnit = sourceCompilationUnit;
        this.sourceTypeDeclaration = sourceTypeDeclaration;
        //this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
        //ICompilationUnit sourceICompilationUnit = (ICompilationUnit) sourceCompilationUnit.getJavaElement();
        //MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
        //CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
        //sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
        //this.compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
        //this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
        //this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
        this.requiredImportDeclarationsInExtractedClass = new LinkedHashSet<>();
        this.additionalArgumentsAddedToExtractedMethods = new LinkedHashMap<>();
        this.additionalParametersAddedToExtractedMethods = new LinkedHashMap<>();
        this.sourceMethodBindingsChangedWithPublicModifier = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedSetterMethod = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedGetterMethod = new LinkedHashSet<>();
        this.fieldDeclarationsChangedWithPublicModifier = new LinkedHashSet<>();
        this.memberTypeDeclarationsChangedWithPublicModifier = new LinkedHashSet<>();
        this.oldMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.newMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.oldToNewExtractedMethodDeclarationMap = new LinkedHashMap<>();
        this.extractedFieldFragments = extractedFieldFragments;
        this.extractedMethods = extractedMethods;
        this.delegateMethods = delegateMethods;
        this.extractedTypeName = extractedTypeName;
        this.leaveDelegateForPublicMethods = false;
        //this.statementRewriteMap = new LinkedHashMap<PsiStatement, ASTRewrite>();
        this.constructorFinalFieldAssignmentMap = new LinkedHashMap<>();
        this.extractedClassConstructorParameterMap = new LinkedHashMap<>();
        this.extractedFieldsWithThisExpressionInTheirInitializer = new LinkedHashSet<>();
        this.staticallyImportedMethods = new LinkedHashSet<>();
        for (PsiMethod extractedMethod : extractedMethods) {
            additionalArgumentsAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
            additionalParametersAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
        }

        this.project = sourceFile.getProject();
        this.factory = PsiElementFactory.SERVICE.getInstance(project);
        this.fileFactory = PsiFileFactory.getInstance(project);
        this.psiManager = PsiManager.getInstance(project);
    }

    public String getExtractedTypeName() {
        return extractedTypeName;
    }

    public void setExtractedTypeName(String targetTypeName) {
        this.extractedTypeName = targetTypeName;
    }

    public void setLeaveDelegateForPublicMethods(boolean leaveDelegateForPublicMethods) {
        this.leaveDelegateForPublicMethods = leaveDelegateForPublicMethods;
    }

    /*
    TODO ?
    public Set<IJavaElement> getJavaElementsToOpenInEditor() {
        return javaElementsToOpenInEditor;
    }
     */

    private String appendAccessorMethodSuffix(String accessorMethodName, PsiMethod[] methodDeclarations) {
        return appendAccessorMethodSuffix(accessorMethodName, new LinkedHashSet<>(Arrays.asList(methodDeclarations)));
    }

    private String appendAccessorMethodSuffix(String accessorMethodName, Set<PsiMethod> methodDeclarations) {
        boolean sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = false;
        for (PsiMethod methodDeclaration : methodDeclarations) {
            if (methodDeclaration.getName().equals(accessorMethodName)) {
                if (accessorMethodName.startsWith(GETTER_PREFIX)) {
                    PsiElement simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
                    if (simpleName == null) {
                        sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
                        break;
                    }
                } else if (accessorMethodName.startsWith(SETTER_PREFIX)) {
                    PsiElement simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
                    if (simpleName == null) {
                        sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor = true;
                        break;
                    }
                }
            }
        }
        if (sourceTypeContainsMethodWithAccessorNameThatIsNotPureAccessor) {
            accessorMethodName += ACCESSOR_SUFFIX;
        }
        return accessorMethodName;
    }

    public void apply() {
        for (PsiMethod method : extractedMethods) {
            if (!method.getModifierList().hasModifierProperty(PsiModifier.PRIVATE))
                delegateMethods.add(method);
        }
        removeFieldFragmentsInSourceClass(extractedFieldFragments);
        Set<PsiField> modifiedFieldsInNonExtractedMethods = new LinkedHashSet<>();
        Set<PsiField> accessedFieldsInNonExtractedMethods = new LinkedHashSet<>();
        modifyExtractedFieldAssignmentsInSourceClass(extractedFieldFragments, modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedFieldAccessesInSourceClass(extractedFieldFragments, accessedFieldsInNonExtractedMethods);
        createExtractedTypeFieldReferenceInSourceClass();

        Set<PsiType> typeBindings = new LinkedHashSet<>();
        TypeVisitor typeVisitor = new TypeVisitor(); //PsiJavaElementVisitor
        for (PsiVariable fieldFragment : extractedFieldFragments) {
            fieldFragment.getParent().accept(typeVisitor); //TODO getParent() should probably be removed OR we should visit class of this field
            typeBindings.addAll(typeVisitor.getTypeBindings());
        }
        for (PsiMethod method : extractedMethods) {
            method.accept(typeVisitor);
            typeBindings.addAll(typeVisitor.getTypeBindings());
        }
        RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass); //TODO PsiType? PsiClass? PsiImportStatementDeclaration?

        createExtractedClass(modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedMethodInvocationsInSourceClass();
        handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer();

        Set<PsiMethod> methodsToBeRemoved = new LinkedHashSet<>();
        for (PsiMethod method : extractedMethods) {
            if (delegateMethods.contains(method))
                addDelegationInExtractedMethod(method);
            else
                methodsToBeRemoved.add(method);
        }
        if (methodsToBeRemoved.size() > 0)
            removeSourceMethods(methodsToBeRemoved);
    }

    //TODO comment test
    private PsiExpression convertPsiFieldSetterCallExpression(PsiField psiField) {
        String originalFieldName = psiField.getNameIdentifier().getText();
        String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1, originalFieldName.length());
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1, extractedTypeName.length());

        if (psiField.getInitializer() == null) {
            throw new IllegalArgumentException(psiField.getText() + ": field to call setter should have initializer");
        }
        String methodCallExpressionText = SETTER_PREFIX + modifiedFieldName + "(" + psiField.getInitializer().getText() + ")";

        if (psiField.getModifierList() != null && psiField.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            methodCallExpressionText = extractedTypeName + methodCallExpressionText;
        } else {
            methodCallExpressionText = modifiedExtractedTypeName + methodCallExpressionText;
        }

        return factory.createExpressionFromText(methodCallExpressionText, sourceTypeDeclaration);
    }

    //TODO comment test
    private PsiStatement convertPsiFieldSetterCallStatement(PsiField psiField) {
        return factory.createStatementFromText(convertPsiFieldSetterCallExpression(psiField).getText() + ";", sourceTypeDeclaration);
    }

    //TODO write comment test
    private PsiExpression convertPsiFieldToGetterCallExpression(PsiField psiField) {
        return null;
    }

    /*
    //TODO comment
    private PsiMethodCallExpression convertPsiMethodToMethodInvocationExpression(String extractedTypeName, PsiMethod psiMethod) {
        String methodInvocationText = extractedTypeName;

        if (!psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            methodInvocationText = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1, extractedTypeName.length());
        }

        methodInvocationText += "." + extractedTypeName;

        return (PsiMethodCallExpression) factory.createExpressionFromText(methodInvocationText, psiMethod);
    }

     */

    private void handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer() {
        //TODO Author supposes that class name starts from capital letter, which could be not true.
        for (PsiField psiField : extractedFieldsWithThisExpressionInTheirInitializer) {
            for (PsiMethod psiMethod : sourceTypeDeclaration.getMethods()) {
                if (psiMethod.isConstructor()) {
                    PsiStatement psiStatement = convertPsiFieldSetterCallStatement(psiField);
                    psiMethod.getBody().addBefore(psiStatement, psiMethod.getBody().getRBrace());
                }
            }
        }
    }

    // TODO comment test
    private static boolean isStatic(PsiModifierListOwner psiElement) {
        return psiElement.hasModifierProperty(PsiModifier.STATIC);
    }

    private void addDelegationInExtractedMethod(PsiMethod sourceMethod) {
        PsiParameter[] sourceMethodParameters = sourceMethod.getParameterList().getParameters();
        if (sourceMethod.getBody() == null) {
            throw new IllegalArgumentException(sourceMethod.getText() + ": abstract methods should not be extracted");
        }
        PsiStatement[] sourceMethodStatements = sourceMethod.getBody().getStatements();
        for (PsiStatement statement : sourceMethodStatements) {
            statement.delete();
        }

        PsiType sourceMethodReturnType = sourceMethod.getReturnType();

        String delegationText = sourceMethod.getName();
        if (isStatic(sourceMethod)) {
            delegationText = extractedTypeName + "." + delegationText;
        } else {
            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1, extractedTypeName.length());
            delegationText = modifiedExtractedTypeName + "." + delegationText;
        }

        List<String> parameters = new ArrayList<>();
        for (PsiParameter parameter : sourceMethodParameters) {
            parameters.add(parameter.getName());
        }
        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
            if (isThisVariable(argument)) { //TODO this is just this
                parameters.add("this");
            } else {
                if (argument.isField()) {
                    parameters.add("this." + argument.getName()); //TODO getVariableName()?
                } else {
                    parameters.add(argument.getName());
                }
            }
        }

        delegationText += "(";
        boolean firstParameter = true;
        StringBuilder delegationTextBuilder = new StringBuilder(delegationText);
        for (String parameter : parameters) {
            if (!firstParameter) {
                delegationTextBuilder.append(",");
            } else {
                firstParameter = false;
            }

            delegationTextBuilder.append(parameter);
        }
        delegationTextBuilder.append(")");
        delegationText = delegationTextBuilder.toString();

        PsiStatement delegationStatement;
        if (Objects.equals(sourceMethod.getReturnType(), PsiType.VOID)) {
            delegationStatement = factory.createStatementFromText(delegationText + ";", sourceMethod);
        } else {
            delegationStatement = factory.createStatementFromText("return " + delegationText + ";", sourceMethod);
        }

        sourceMethod.getBody().addBefore(delegationStatement, sourceMethod.getBody().getRBrace());
    }

    private void removeSourceMethods(Set<PsiMethod> methods) {
        //TODO IDEA? Eclipse bug workaround: Overlapping TextEdits Exception when the x last methods of a type declaration are being removed
        for (PsiMethod method : methods) {
            method.delete();
        }
    }

    private void modifyExtractedMethodInvocationsInSourceClass() {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        for (PsiMethod methodDeclaration : contextMethods) {
            if (!extractedMethods.contains(methodDeclaration)) {
                PsiCodeBlock methodBody = methodDeclaration.getBody();
                if (methodBody != null) {
                    PsiStatement[] statements = methodBody.getStatements();
                    for (PsiStatement statement : statements) {
                        List<PsiExpression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
                        for (PsiExpression expression : methodInvocations) {
                            if (expression instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                                PsiMethod extractedMethod = methodInvocation.resolveMethod();
                                if (extractedMethod != null) {
                                    if (methodInvocation.getMethodExpression() == null || methodInvocation.getMethodExpression() instanceof PsiThisExpression) { //TODO is this correct this?
                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(extractedMethod);
                                        StringBuilder arguments = new StringBuilder();
                                        Arrays.stream(methodInvocation.getArgumentList().getExpressions()).forEach(expr -> arguments.append(expr.getText()));

                                        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
                                            if (isThisVariable(argument)) {
                                                if (isParentAnonymousClassDeclaration(methodInvocation)) {
                                                    arguments.append(", ").append(sourceTypeDeclaration.getName()).append(".this");
                                                } else {
                                                    arguments.append(", this");
                                                }
                                            } else {
                                                if (argument.isField()) {
                                                    arguments.append(", this.").append(argument.getName());
                                                } else {
                                                    arguments.append(", ").append(argument.getName());
                                                }
                                            }
                                        }

                                        String modifiedMethodCallExpressionText = extractedMethod.getName() + "(" + arguments.toString() + ")";
                                        if (isStatic(extractedMethod)) {
                                            modifiedMethodCallExpressionText = extractedTypeName + "." + modifiedMethodCallExpressionText;
                                        } else {
                                            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1, extractedTypeName.length());
                                            modifiedMethodCallExpressionText = modifiedExtractedTypeName + "." + modifiedMethodCallExpressionText;
                                        }

                                        expression.replace(factory.createExpressionFromText(modifiedMethodCallExpressionText, expression));
                                    } else {
                                        delegateMethods.add(extractedMethod);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean existsNonTransientExtractedFieldFragment() {
        for (PsiField fieldFragment : extractedFieldFragments) {
            if (fieldFragment.getModifierList() != null && fieldFragment.getModifierList().hasExplicitModifier(PsiModifier.TRANSIENT)) {
                return true;
            }
        }

        return false;
    }

    //TODO comment
    private static PsiClass implementsInterface(PsiClass psiClass, String qualifiedInterfaceName) {
        PsiClass[] implementedInterfaces = psiClass.getInterfaces();
        for (PsiClass implementedInterface : implementedInterfaces) {
            if (Objects.equals(implementedInterface.getQualifiedName(), qualifiedInterfaceName)) {
                return implementedInterface;
            }
        }

        PsiClass superclassTypeBinding = psiClass.getSuperClass();
        if (superclassTypeBinding != null) {
            return implementsInterface(superclassTypeBinding, qualifiedInterfaceName);
        }

        return null;
    }

    private PsiClass implementsSerializableInterface(PsiClass typeBinding) {
        return implementsInterface(typeBinding, "java.io.Serializable");
    }

    private PsiClass implementsCloneableInterface(PsiClass typeBinding) {
        return implementsInterface(typeBinding, "java.lang.Cloneable");
    }

    private PsiField staticFieldInitializer(PsiField fieldFragment) {
        if (fieldFragment.getInitializer() instanceof PsiReferenceExpression) {
            PsiElement initializer = ((PsiReferenceExpression) fieldFragment.getInitializer()).resolve();
            if (initializer instanceof PsiField) {
                PsiField psiField = (PsiField) initializer;
                if (isStatic(psiField)) {
                    return psiField;
                }
            }
        }

        return null;
    }

    private boolean variableBindingInExtractedFields(PsiVariable variableBinding) {
        for (PsiField variableDeclaration : extractedFieldFragments) {
            if (variableDeclaration.equals(variableBinding)) { //TODO isEquiavalentTo() ??
                return true;
            }
        }

        return false;
    }

    private void createExtractedClass(Set<PsiField> modifiedFieldsInNonExtractedMethods, Set<PsiField> accessedFieldsInNonExtractedMethods) {
        PsiElement contextContainer = sourceFile.getContainingDirectory(); //TODO parent?
        PsiFile extractedClassFile = null;

        extractedClassFile = null; //TODO name = (extractedTypeName + ".java");

        PsiPackageStatement packageStatement = PsiTreeUtil.findChildOfType(sourceFile, PsiPackageStatement.class);

        if (packageStatement != null) {
            packageStatement = factory.createPackageStatement(packageStatement.getPackageName());
        }

        //TODO name = extractedClassName
        //TODO public

        PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
        if (serializableTypeBinding != null) {
            //Set implements Serializable
            requiredImportDeclarationsInExtractedClass.add(serializableTypeBinding);
        }

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiField> finalFieldFragments = new LinkedHashSet<>();
        Set<PsiField> finalFieldFragmentsWithoutInitializer = new LinkedHashSet<>();
        for (PsiField fieldFragment : extractedFieldFragments) {
            List<PsiExpression> initializerThisExpressions = expressionExtractor.getThisExpressions(fieldFragment.getInitializer());
            PsiField extractedFieldDeclaration = null;
            if (initializerThisExpressions.isEmpty()) {
                PsiField staticFieldInitializer = staticFieldInitializer(fieldFragment);
                if (staticFieldInitializer != null && !variableBindingInExtractedFields(staticFieldInitializer)) {
                    PsiClass staticFieldDeclaringClassTypeBinding = staticFieldInitializer.getContainingClass();
                    String qualifiedName = staticFieldDeclaringClassTypeBinding.getName() + "." + staticFieldInitializer.getName();
                    //TODO add Field to the extracted class
                } else {
                    extractedFieldDeclaration = (PsiField) fieldFragment.copy();
                }
            } else {
                this.extractedFieldsWithThisExpressionInTheirInitializer.add(fieldFragment);
                extractedFieldDeclaration = factory.createField(fieldFragment.getName(), fieldFragment.getType());
            }

            //TODO useless? extractedClassRewriter.set(extractedFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, originalFieldDeclaration.getType(), null);
            extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true); //TODO test public fields

            if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.FINAL)) {
                extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                finalFieldFragments.add(fieldFragment);
                if (fieldFragment.getInitializer() == null)
                    finalFieldFragmentsWithoutInitializer.add(fieldFragment);
            }
            if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
            }
            if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.TRANSIENT)) {
                extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.TRANSIENT, true);
            }
            if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.VOLATILE)) {
                extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.VOLATILE, true);
            }

            //TODO добавить в класс в конец КСТА
        }
        for (PsiMethod constructor : constructorFinalFieldAssignmentMap.keySet()) {
            Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
            PsiMethod extractedClassConstructor = factory.createConstructor(extractedTypeName);
            extractedClassConstructor.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

            Set<PsiParameter> extractedClassConstructorParameters = extractedClassConstructorParameterMap.get(constructor);
            for (PsiParameter variableDeclaration : extractedClassConstructorParameters) {
                PsiType parameterType = variableDeclaration.getType(); //TODO check varargs
                extractedClassConstructor.getParameterList().add(factory.createParameter(variableDeclaration.getName(), parameterType));
            }
            Set<PsiType> thrownExceptionTypeBindings = new LinkedHashSet<>();
            for (PsiField fieldFragment : finalFieldAssignmentMap.keySet()) {
                PsiAssignmentExpression fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
                PsiStatement newFieldAssignment = (PsiStatement) factory.createStatementFromText(fieldAssignment.getText(), fieldAssignment);
                ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor(); //TODO migrate this class
                fieldAssignment.getRExpression().accept(thrownExceptionVisitor);
                thrownExceptionTypeBindings.addAll(thrownExceptionVisitor.getTypeBindings());
            }
            RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
            for (PsiType thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
                //TODO add throws to constructor
            }
            //TODO add constructor to the class
        }

        for (PsiField fieldFragment : extractedFieldFragments) {
            if (accessedFieldsInNonExtractedMethods.contains(fieldFragment)) {
                PsiMethod getterMethodDeclaration = createGetterMethodDeclaration(fieldFragment);
                //TODO add getterMEthodDeclaration to the class
            }
            if (modifiedFieldsInNonExtractedMethods.contains(fieldFragment) && !finalFieldFragments.contains(fieldFragment)) {
                PsiMethod setterMethodDeclaration = createSetterMethodDeclaration(fieldFragment);
                //TODO add to class setterMethodDe
            }
        }
        for (PsiMethod method : extractedMethods) {
            PsiMethod extractedMethodDeclaration = createExtractedMethodDeclaration(method);
            //TODO add to class extractedMethodDeclaration
        }
        Map<PsiMethod, Integer> levelMap = new LinkedHashMap<>();
        //create adjacency list
        AdjacencyList adjacencyList = new AdjacencyList();
        for (PsiMethod method : extractedMethods) {
            if (oldMethodInvocationsWithinExtractedMethods.containsKey(method)) {
                levelMap.put(method, -1);
                for (PsiMethodCallExpression methodInvocation : oldMethodInvocationsWithinExtractedMethods.get(method)) {
                    //exclude recursive invocations
                    if (!method.equals(methodInvocation.resolveMethod())) {
                        Node source = new Node(method.getName()); //https://help.eclipse.org/luna/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FIMethodBinding.html
                        Node target = new Node(methodInvocation.resolveMethod().getName());
                        adjacencyList.addEdge(source, target, 0);
                    }
                }
            } else
                levelMap.put(method, 0);
        }
        TarjanAlgorithm tarjan = new TarjanAlgorithm(adjacencyList);
        while (!allExtractedMethodsObtainedLevel(levelMap)) {
            for (PsiMethod method : extractedMethods) {
                if (levelMap.get(method) == -1) {
                    Set<PsiMethodCallExpression> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(method);
                    int maxLevel = 0;
                    boolean dependsOnMethodWithoutLevel = false;
                    for (PsiMethodCallExpression methodInvocation : methodInvocations) {
                        //exclude recursive invocations
                        if (!method.equals(methodInvocation.resolveMethod())) {
                            PsiMethod invokedMethod = getExtractedMethod(methodInvocation.resolveMethod());
                            int invokedMethodLevel = levelMap.get(invokedMethod);
                            if (invokedMethodLevel == -1) {
                                boolean belongToTheSameStronglyConnectedComponent = tarjan.belongToTheSameStronglyConnectedComponent(
                                        method.getName(), methodInvocation.resolveMethod().getName()); //TODO getKey()
                                if (belongToTheSameStronglyConnectedComponent) {
                                    double sourceAverageLevelOfTargets = getAverageLevelOfTargets(method.getName(), levelMap, adjacencyList); //getKey()
                                    double targetAverageLevelOfTargets = getAverageLevelOfTargets(methodInvocation.resolveMethod().getName(), levelMap, adjacencyList); //getKey()
                                    if (sourceAverageLevelOfTargets > targetAverageLevelOfTargets) {
                                        dependsOnMethodWithoutLevel = true;
                                        break;
                                    }
                                } else {
                                    dependsOnMethodWithoutLevel = true;
                                    break;
                                }
                            } else {
                                if (invokedMethodLevel > maxLevel)
                                    maxLevel = invokedMethodLevel;
                            }
                        }
                    }
                    if (!dependsOnMethodWithoutLevel) {
                        levelMap.put(method, maxLevel + 1);
                    }
                }
            }
        }
        Set<PsiMethod> sortedMethods = new LinkedHashSet<>();
        int min = 0;
        while (!levelMap.isEmpty()) {
            for (PsiMethod method : extractedMethods) {
                if (levelMap.containsKey(method)) {
                    int level = levelMap.get(method);
                    if (level == min) {
                        levelMap.remove(method);
                        if (level > 0)
                            sortedMethods.add(method);
                    }
                }
            }
            min++;
        }
        for (PsiMethod oldMethod : sortedMethods) {
            Map<PlainVariable, PsiParameter> fieldParameterMap = new LinkedHashMap<>();
            Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
            PsiParameter sourceClassParameter = null;
            boolean sourceClassParameterShouldBeFinal = false;

            Set<PsiMethodCallExpression> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
            PsiMethod newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
            List<PsiMethodCallExpression> newMethodInvocations = new ArrayList<PsiMethodCallExpression>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
            Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
            Set<PsiParameter> additionalParametersForInvokerMethod = additionalParametersAddedToExtractedMethods.get(oldMethod);
            int i = 0;
            for (PsiMethodCallExpression oldMethodInvocation : oldMethodInvocations) {
                if (oldMethodInvocation.getMethodExpression() == null || oldMethodInvocation.getMethodExpression() instanceof PsiThisExpression) {
                    //invocation without expression
                    if (!oldMethod.equals(oldMethodInvocation.resolveMethod())) {
                        //non-recursive
                        PsiMethod oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethod());
                        Set<PlainVariable> additionalArgumentsForExtractedInvokedMethod = additionalArgumentsAddedToExtractedMethods.get(oldExtractedInvokedMethod);
                        for (PlainVariable additionalArgument : additionalArgumentsForExtractedInvokedMethod) {
                            if (isParentAnonymousClassDeclaration(oldMethodInvocation.getNode())) {
                                if (isThisVariable(additionalArgument))
                                    sourceClassParameterShouldBeFinal = true;
                                else
                                    fieldParameterFinalMap.put(additionalArgument, true);
                            }
                            if (!additionalArgumentsForInvokerMethod.contains(additionalArgument)) {
                                if (isThisVariable(additionalArgument)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethod);
                                    addThisVariable(additionalArgumentsForInvokerMethod);
                                    additionalParametersForInvokerMethod.add(sourceClassParameter);
                                } else {
                                    PsiParameter fieldParameter = addParameterToMovedMethod(newMethod, additionalArgument);
                                    additionalArgumentsForInvokerMethod.add(additionalArgument);
                                    additionalParametersForInvokerMethod.add(fieldParameter);
                                    fieldParameterMap.put(additionalArgument, fieldParameter);
                                }
                            }

                            PsiMethodCallExpression newMethodInvocation = newMethodInvocations.get(i);
                            if (isThisVariable(additionalArgument)) {
                                String sourceTypeName = sourceTypeDeclaration.getName();
                                String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1, sourceTypeName.length());
                                addParameterToMethodInvocation(newMethodInvocation, modifiedSourceTypeName);
                            } else {
                                if (additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getName());
                                    addParameterToMethodInvocation(newMethodInvocation, parameterName);
                                } else {
                                    addParameterToMethodInvocation(newMethodInvocation, additionalArgument.getName());
                                }
                            }
                        }
                    }
                } else {
                    //invocation with expression
                    PsiMethod oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethod());
                    delegateMethods.add(oldExtractedInvokedMethod);
                }
                i++;
            }
            if (sourceClassParameterShouldBeFinal) {
                if (sourceClassParameter != null) {
                    sourceClassParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                } else {
                    int j = 0;
                    List<PsiParameter> additionalParametersForInvokerMethodList = new ArrayList<PsiParameter>(additionalParametersForInvokerMethod);
                    PsiParameter additionalParameter = null;
                    for (PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                        if (isThisVariable(additionalArgument)) {
                            additionalParameter = additionalParametersForInvokerMethodList.get(j);
                            break;
                        }
                        j++;
                    }
                    additionalParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                }
            }
            for (PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
                if (fieldParameterFinalMap.get(fieldName)) {
                    PsiParameter fieldParameter = fieldParameterMap.get(fieldName);
                    if (fieldParameter != null) {
                        fieldParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                    } else {
                        int j = 0;
                        List<PsiParameter> additionalParametersForInvokerMethodList = new ArrayList<PsiParameter>(additionalParametersForInvokerMethod);
                        PsiParameter additionalParameter = null;
                        for (PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            if (additionalArgument.equals(fieldName)) {
                                additionalParameter = additionalParametersForInvokerMethodList.get(j);
                                break;
                            }
                            j++;
                        }
                        additionalParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                    }
                }
            }
        }
        //handle recursive extracted method invocations
        for (PsiMethod oldMethod : oldMethodInvocationsWithinExtractedMethods.keySet()) {
            Set<PsiMethodCallExpression> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
            PsiMethod newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
            List<PsiMethodCallExpression> newMethodInvocations = new ArrayList<>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
            Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
            int i = 0;
            for (PsiMethodCallExpression oldMethodInvocation : oldMethodInvocations) {
                if (oldMethodInvocation.getMethodExpression() == null || oldMethodInvocation.getMethodExpression() instanceof PsiThisExpression) {
                    //invocation without expression
                    if (oldMethod.equals(oldMethodInvocation)) {
                        //recursive invocation
                        for (PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            PsiMethodCallExpression newMethodInvocation = newMethodInvocations.get(i);
                            if (isThisVariable(additionalArgument)) {
                                String sourceTypeName = sourceTypeDeclaration.getName();
                                String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1, sourceTypeName.length());
                                //TODO addParamtert to method call (newMethodInvocation, modifiedSourceTypeName)
                            } else {
                                if (additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getName());
                                    //TODO same for parameterName
                                } else {
                                    //TODO same for additionlArgument.getName()
                                }
                            }
                        }
                    }
                }
                i++;
            }
        }

        PsiMethod cloneMethod = createCloneMethod();
        if (cloneMethod != null) {
            //TODO add cloneMethod to the xtracted method
            PsiClass cloneableInterfaceTypeBinding = implementsCloneableInterface(sourceTypeDeclaration);
            if (cloneableInterfaceTypeBinding != null) {
                PsiType cloneableInterfaceType = RefactoringUtility.generateTypeFromTypeBinding(cloneableInterfaceTypeBinding);
                extractedClassImplementedInterfacesRewrite.insertLast(cloneableInterfaceType, null);
                Set<PsiClass> typeBindings = new LinkedHashSet<>();
                typeBindings.add(cloneableInterfaceTypeBinding);
                RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
            } else {
                String cloneableType = "Cloneable";
                //TODO add implements Cloneable;
            }
        }

        for (PsiClass typeBinding : requiredImportDeclarationsInExtractedClass) {
            addImportDeclaration(typeBinding);
        }
        for (PsiMethod methodBinding : staticallyImportedMethods) {
            addStaticImportDeclaration(methodBinding);
        }

        //TODO compile result
    }

    private PsiMethod createCloneMethod() {
        // check if source class contains clone method
        PsiMethod cloneMethodBinding = null; //TODO
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        for (PsiMethod method : methodDeclarations) {
            if (isClone(method)) {
                cloneMethodBinding = method;
                break;
            }
        }
        if (cloneMethodBinding == null) {
            cloneMethodBinding = findCloneMethod(sourceTypeDeclaration.getSuperClass());
        }
        if (cloneMethodBinding != null) {
            PsiMethod cloneMethodDeclaration = factory.createMethod(cloneMethodBinding.getName(), RefactoringUtility.generateTypeFromTypeBinding(cloneMethodBinding.getReturnType());//TODO
            cloneMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

            PsiReferenceList thrownExceptionTypes = cloneMethodBinding.getThrowsList();
            Set<PsiReference> thrownExceptionTypeBindings = new LinkedHashSet<>();
            for (PsiReference typeBinding : thrownExceptionTypes.getReferenceElements()) {
                PsiReference type = RefactoringUtility.generateTypeFromTypeBinding(typeBinding.resolve());
                cloneMethodBinding.getThrowsList().add(type.resolve()); //TODO
                thrownExceptionTypeBindings.add(typeBinding);
            }
            RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
            PsiCodeBlock body = cloneMethodBinding.getBody();

            PsiMethodCallExpression superCloneMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText("super." + cloneMethodBinding.getName() + "()", sourceTypeDeclaration); //TODO
            PsiReturnStatement returnStatement = (PsiReturnStatement) factory.createStatementFromText("return (" + extractedTypeName + ") " + superCloneMethodInvocation.getText(), sourceTypeDeclaration); //TODO

            StringBuilder returnStatementText = new StringBuilder();

            if (thrownExceptionTypes.getReferenceElements().length == 0) {
                PsiTryStatement tryStatement; //TODO new try statement
                PsiCodeBlock tryBody; //TODO new codeblock
                returnStatementText.append(
                        "try {\n"
                                + returnStatement.getText() + "\n"
                                +"} catch (CloneNotSupportedException e) {\n"
                                + "throw new InternalError(\"Failed to implement Cloneable interface\");\n"
                                + "}");
            }

            //TODO add returnStatementText to cloneMEthodDeclaration
            return cloneMethodDeclaration;
        }
        return null;
    }

    private double getAverageLevelOfTargets(String methodBindingKey, Map<PsiMethod, Integer> levelMap, AdjacencyList adjacency) {
        Node n = new Node(methodBindingKey);
        LinkedHashSet<Edge> edges = adjacency.getAdjacent(n);
        int levelSum = 0;
        int targetSum = 0;
        for (Edge edge : edges) {
            Node target = edge.getTarget();
            for (PsiMethod methodDeclaration : levelMap.keySet()) {
                int level = levelMap.get(methodDeclaration);
                if (methodDeclaration.getName().equals(target.getName())) { //TODO is just getName ok?
                    if (level != -1) {
                        levelSum += level;
                        targetSum++;
                    }
                    break;
                }
            }
        }
        if (targetSum == 0)
            return Double.MAX_VALUE;
        else
            return (double) levelSum / (double) targetSum;
    }

    private boolean allExtractedMethodsObtainedLevel(Map<PsiMethod, Integer> levelMap) {
        for (PsiMethod method : levelMap.keySet()) {
            if (levelMap.get(method) == -1)
                return false;
        }
        return true;
    }

    private PsiMethod getExtractedMethod(PsiMethod methodBinding) {
        for (PsiMethod extractedMethod : extractedMethods) {
            if (extractedMethod.isEquivalentTo(methodBinding)) //equivalent or equal?
                return extractedMethod;
        }
        return null;
    }

    private PsiMethod createExtractedMethodDeclaration(PsiMethod extractedMethod) {
        PsiMethod newMethodDeclaration = (PsiMethod) extractedMethod.copy();

        newMethodDeclaration.setName(extractedMethod.getName());

        newMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        for (PsiAnnotation annotation : newMethodDeclaration.getAnnotations()) {
            if (annotation instanceof MarkerAnnotation && annotation.getQualifiedName() != null && annotation.getQualifiedName().equals("Test")) {
                annotation.delete();
            }
        }

        modifySourceMemberAccessesInTargetClass(extractedMethod, newMethodDeclaration);
        modifySourceStaticFieldInstructionsInTargetClass(extractedMethod, newMethodDeclaration);
        //check if there is a parameter type collision
        PsiParameterList originalParameters = extractedMethod.getParameterList();
        PsiParameterList newParameters = newMethodDeclaration.getParameterList();
        int i = 0;
        Set<JvmType> typeBindingsToBeRemoved = new LinkedHashSet<>();
        for (PsiParameter originalParameter : originalParameters.getParameters()) {
            PsiType originalParameterTypeBinding = originalParameter.getType();
            for (PsiClass typeBinding : requiredImportDeclarationsInExtractedClass) {
                if (!originalParameterTypeBinding.equals(typeBinding)) {
                    PsiType qualifiedType = RefactoringUtility.generateQualifiedTypeFromTypeBinding(originalParameterTypeBinding);
                    newParameters.add(factory.createParameter(originalParameter.getName(), qualifiedType));
                    typeBindingsToBeRemoved.add(originalParameterTypeBinding);
                    break;
                }
            }
            i++;
        }
        requiredImportDeclarationsInExtractedClass.removeAll(typeBindingsToBeRemoved);
        return newMethodDeclaration;
    }

    private boolean variableBindingCorrespondsToExtractedField(PsiVariable variableBinding) {
        for (PsiField extractedFieldFragment : extractedFieldFragments) {
            if (psiManager.areElementsEquivalent(extractedFieldFragment, variableBinding)) //TODO replacce all isEquavialentTo with this
                return true;
        }
        return false;
    }

    private boolean methodBindingCorrespondsToExtractedMethod(PsiMethod methodBinding) {
        for (PsiMethod extractedMethod : extractedMethods) {
            if (psiManager.areElementsEquivalent(extractedMethod, methodBinding))
                return true;
        }
        return false;
    }

    private boolean isParentAnonymousClassDeclaration(ASTNode node) {
        if (node.getTreeParent() == null) {
            return false;
        }

        if (node.getTreeParent().getPsi() instanceof PsiAnonymousClass) {
            return true;
        } else {
            return isParentAnonymousClassDeclaration(node.getTreeParent());
        }
    }

    private PsiMethod findSetterMethodInSourceClass(PsiField fieldBinding) {
        PsiClass typeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(fieldBinding, sourceTypeDeclaration);
        if (typeDeclaration != null) {
            PsiMethod[] contextMethods = typeDeclaration.getMethods();
            for (PsiMethod methodDeclaration : contextMethods) {
                PsiElement simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
                if (simpleName != null && psiManager.areElementsEquivalent(simpleName, fieldBinding)) {
                    return methodDeclaration;
                }
            }
        }
        return null;
    }

    private PsiMethod findGetterMethodInSourceClass(PsiField fieldBinding) {
        PsiClass typeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(fieldBinding, sourceTypeDeclaration);
        if (typeDeclaration != null) {
            PsiMethod[] contextMethods = typeDeclaration.getMethods();
            for (PsiMethod methodDeclaration : contextMethods) {
                PsiElement simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
                if (simpleName instanceof PsiReferenceExpression) {
                    PsiElement psiElement = ((PsiReferenceExpression) simpleName).resolve();
                    if (psiManager.areElementsEquivalent(psiElement, fieldBinding)) {
                        return methodDeclaration;
                    }
                }

            }
        }
        return null;
    }

    private void createSetterMethodInSourceClass(PsiVariable variableBinding) {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            if (psiManager.areElementsEquivalent(variableBinding, fieldDeclaration)) {
                String methodName = fieldDeclaration.getName();
                methodName = SETTER_PREFIX + methodName.substring(0, 1).toUpperCase() + methodName.substring(1, methodName.length());
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = (PsiMethod) factory.createStatementFromText("public void " + methodName + "(" + fieldDeclaration.getType().getPresentableText() + " " + fieldDeclaration.getName() + ") {\n"
                        + "this." + fieldDeclaration.getName() + " = " + fieldDeclaration.getName() + ";\n" + "}", sourceTypeDeclaration);

                //TODO добавить метод
            }
        }
    }

    private void createGetterMethodInSourceClass(PsiVariable variableBinding) {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            if (variableBinding.equals(fieldDeclaration)) { //TODO или везде equals?
                String methodName = fieldDeclaration.getName();
                methodName = GETTER_PREFIX + methodName.substring(0, 1).toUpperCase() + methodName.substring(1, methodName.length());
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = (PsiMethod) factory.createStatementFromText("public " + fieldDeclaration.getType().getPresentableText() + " " + methodName + "() {\n"
                        + "return " + fieldDeclaration.getName() + ";\n"
                        + "}", sourceTypeDeclaration);
                //TODO добавить метод
            }
        }
    }

}
