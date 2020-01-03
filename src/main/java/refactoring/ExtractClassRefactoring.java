package refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.FileContentUtil;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.util.MethodDeclarationUtility;
import core.ast.util.StatementExtractor;
import core.ast.util.TypeVisitor;
import core.ast.util.math.AdjacencyList;
import core.ast.util.math.Edge;
import core.ast.util.math.Node;
import core.ast.util.math.TarjanAlgorithm;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;

import java.util.*;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "ConstantConditions"})
public class ExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private PsiJavaFile sourceFile;
    private PsiClass sourceTypeDeclaration;
    private Set<PsiElement> javaElementsToOpenInEditor;
    private Map<PsiMethod, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
    private Map<PsiMethod, Set<PsiParameter>> additionalParametersAddedToExtractedMethods;
    private Set<PsiMethod> sourceMethodBindingsChangedWithPublicModifier;
    private Set<PsiField> sourceFieldBindingsWithCreatedSetterMethod;
    private Set<PsiField> sourceFieldBindingsWithCreatedGetterMethod;
    private Set<PsiField> fieldDeclarationsChangedWithPublicModifier; //TODO questionable field
    private Set<?> memberTypeDeclarationsChangedWithPublicModifier; //TODO questionable field
    private Map<PsiMethod, Set<PsiMethodCallExpression>> oldMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> newMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, PsiMethod> oldToNewExtractedMethodDeclarationMap;
    private Set<PsiField> extractedFieldFragments;
    private Set<PsiMethod> extractedMethods;
    private Set<PsiMethod> delegateMethods;
    private String extractedTypeName;
    //this map holds for each constructor the assignment statements that initialize final extracted fields
    private Map<PsiMethod, Map<PsiField, PsiAssignmentExpression>> constructorFinalFieldAssignmentMap;
    //this map hold the parameters that should be passed in each constructor of the extracted class
    private Map<PsiMethod, Set<PsiParameter>> extractedClassConstructorParameterMap;
    private Set<PsiField> extractedFieldsWithThisExpressionInTheirInitializer;

    // TODO comments
    private PsiElementFactory factory;
    private PsiFileFactory fileFactory;
    private Project project;
    private PsiManager psiManager;

    public ExtractClassRefactoring(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration,
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
        //this.statementRewriteMap = new LinkedHashMap<PsiStatement, ASTRewrite>();
        this.constructorFinalFieldAssignmentMap = new LinkedHashMap<>();
        this.extractedClassConstructorParameterMap = new LinkedHashMap<>();
        this.extractedFieldsWithThisExpressionInTheirInitializer = new LinkedHashSet<>();
        //this.staticallyImportedMethods = new LinkedHashSet<>();
        for (PsiMethod extractedMethod : extractedMethods) {
            additionalArgumentsAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
            additionalParametersAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
        }

        this.project = sourceFile.getProject();
        this.factory = PsiElementFactory.getInstance(project);
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
    }

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

        createExtractedClass(modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedMethodInvocationsInSourceClass();
        handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer();

        Set<PsiMethod> methodsToBeRemoved = new LinkedHashSet<>();
        for (PsiMethod method : extractedMethods) {
            if (delegateMethods.contains(method)) {
                addDelegationInExtractedMethod(method);
            } else {
                methodsToBeRemoved.add(method);
            }
        }

        if (methodsToBeRemoved.size() > 0) {
            removeSourceMethods(methodsToBeRemoved);
        }
    }

    private PsiExpression convertPsiFieldToSetterCallExpression(PsiField psiField) {
        String originalFieldName = psiField.getNameIdentifier().getText();
        String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);

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

    private PsiStatement convertPsiFieldToSetterCallStatement(PsiField psiField) {
        return factory.createStatementFromText(convertPsiFieldToSetterCallExpression(psiField).getText() + ";", sourceTypeDeclaration);
    }

    private void handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer() {
        for (PsiField psiField : extractedFieldsWithThisExpressionInTheirInitializer) {
            for (PsiMethod psiMethod : sourceTypeDeclaration.getMethods()) {
                if (psiMethod.isConstructor()) {
                    PsiStatement psiStatement = convertPsiFieldToSetterCallStatement(psiField);
                    psiMethod.getBody().addBefore(psiStatement, psiMethod.getBody().getRBrace());
                }
            }
        }
    }

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

        String delegationText = sourceMethod.getName();
        if (isStatic(sourceMethod)) {
            delegationText = extractedTypeName + "." + delegationText;
        } else {
            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
            delegationText = modifiedExtractedTypeName + "." + delegationText;
        }

        List<String> parameters = new ArrayList<>();
        for (PsiParameter parameter : sourceMethodParameters) {
            parameters.add(parameter.getName());
        }
        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
            if (isThisVariable(argument)) {
                parameters.add("this");
            } else {
                if (argument.isField()) {
                    parameters.add("this." + argument.getName());
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
                                    methodInvocation.getMethodExpression();
                                    if (methodInvocation.getMethodExpression() instanceof PsiThisExpression) {
                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(extractedMethod);
                                        StringBuilder arguments = new StringBuilder();

                                        boolean isFirst = true;
                                        for (PsiExpression argument : methodInvocation.getArgumentList().getExpressions()) {
                                            if (!isFirst) {
                                                arguments.append(", ");
                                            }

                                            arguments.append(argument.getText());
                                            isFirst = false;
                                        }

                                        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
                                            if (isThisVariable(argument)) {
                                                if (isParentAnonymousClassDeclaration(methodInvocation.getNode())) {
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
                                            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
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
            if (variableDeclaration.equals(variableBinding)) {
                return true;
            }
        }

        return false;
    }

    private void createExtractedClass(Set<PsiField> modifiedFieldsInNonExtractedMethods, Set<PsiField> accessedFieldsInNonExtractedMethods) {
        String extractedClassFileName = extractedTypeName + ".java";
        PsiJavaFile extractedClassFile = (PsiJavaFile) fileFactory.createFileFromText(JavaLanguage.INSTANCE, "");
        extractedClassFile = (PsiJavaFile) extractedClassFile.setName(extractedClassFileName);

        PsiClass extractedClass = factory.createClass(extractedTypeName);
        extractedClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        PsiPackageStatement packageStatement = sourceFile.getPackageStatement();

        if (packageStatement != null) {
            extractedClassFile.setPackageName(packageStatement.getPackageName());
        }

        PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
        if (serializableTypeBinding != null) {
            extractedClass.getImplementsList().add(serializableTypeBinding);
        }

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiField> finalFieldFragments = new LinkedHashSet<>();
        for (PsiField fieldFragment : extractedFieldFragments) {
            List<PsiExpression> initializerThisExpressions = expressionExtractor.getThisExpressions(fieldFragment.getInitializer());
            PsiField extractedFieldDeclaration;
            if (initializerThisExpressions.isEmpty()) {
                PsiField staticFieldInitializer = staticFieldInitializer(fieldFragment);
                if (staticFieldInitializer != null && !variableBindingInExtractedFields(staticFieldInitializer)) {
                    String fragmentText = fieldFragment.getName();
                    PsiClass staticFieldDeclaringClassTypeBinding = staticFieldInitializer.getContainingClass();
                    String qualifiedName = staticFieldDeclaringClassTypeBinding.getName() + "." + staticFieldInitializer.getName();

                    extractedFieldDeclaration = factory.createFieldFromText(fragmentText + "=" + qualifiedName + ";", null);
                } else {
                    extractedFieldDeclaration = (PsiField) fieldFragment.copy();
                }
            } else {
                this.extractedFieldsWithThisExpressionInTheirInitializer.add(fieldFragment);
                extractedFieldDeclaration = factory.createField(fieldFragment.getName(), fieldFragment.getType());
            }

            extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);

            if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.FINAL)) {
                extractedFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                finalFieldFragments.add(fieldFragment);
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

            extractedClass.addBefore(extractedFieldDeclaration, extractedClass.getRBrace());
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

            extractedClassConstructor = (PsiMethod) extractedClass.addBefore(extractedClassConstructor, extractedClass.getRBrace());

            for (PsiField fieldFragment : finalFieldAssignmentMap.keySet()) {
                PsiAssignmentExpression fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
                extractedClassConstructor.getBody().addBefore(factory.createStatementFromText(fieldAssignment.getText() + ";", null), extractedClassConstructor.getBody().getRBrace());
            }
        }

        for (PsiField fieldFragment : extractedFieldFragments) {
            if (accessedFieldsInNonExtractedMethods.contains(fieldFragment)) {
                PsiMethod getterMethodDeclaration = createGetterMethodDeclaration(fieldFragment);
                extractedClass.addBefore(getterMethodDeclaration, extractedClass.getRBrace());
            }
            if (modifiedFieldsInNonExtractedMethods.contains(fieldFragment) && !finalFieldFragments.contains(fieldFragment)) {
                PsiMethod setterMethodDeclaration = createSetterMethodDeclaration(fieldFragment);
                extractedClass.addBefore(setterMethodDeclaration, extractedClass.getRBrace());
            }
        }
        for (PsiMethod method : extractedMethods) {
            PsiMethod extractedMethodDeclaration = createExtractedMethodDeclaration(method);
            extractedClass.addBefore(extractedMethodDeclaration, extractedClass.getRBrace());
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
                        Node source = new Node(method);
                        Node target = new Node(methodInvocation.resolveMethod());
                        adjacencyList.addEdge(source, target, 0);
                    }
                }
            } else {
                levelMap.put(method, 0);
            }
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
                                        method, methodInvocation.resolveMethod());
                                if (belongToTheSameStronglyConnectedComponent) {
                                    double sourceAverageLevelOfTargets = getAverageLevelOfTargets(method, levelMap, adjacencyList);
                                    double targetAverageLevelOfTargets = getAverageLevelOfTargets(methodInvocation.resolveMethod(), levelMap, adjacencyList);
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
            Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<>();
            PsiParameter sourceClassParameter = null;
            boolean sourceClassParameterShouldBeFinal = false;

            Set<PsiMethodCallExpression> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
            PsiMethod newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
            List<PsiMethodCallExpression> newMethodInvocations = new ArrayList<>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
            Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
            Set<PsiParameter> additionalParametersForInvokerMethod = additionalParametersAddedToExtractedMethods.get(oldMethod);
            int i = 0;
            for (PsiMethodCallExpression oldMethodInvocation : oldMethodInvocations) {
                oldMethodInvocation.getMethodExpression();
                if (oldMethodInvocation.getMethodExpression() instanceof PsiThisExpression) {
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
                                String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1);
                                newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(modifiedSourceTypeName, null));
                            } else {
                                if (additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getName());
                                    newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(parameterName, null));
                                } else {
                                    newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(additionalArgument.getName(), null));
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
                    List<PsiParameter> additionalParametersForInvokerMethodList = new ArrayList<>(additionalParametersForInvokerMethod);
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
                        List<PsiParameter> additionalParametersForInvokerMethodList = new ArrayList<>(additionalParametersForInvokerMethod);
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
                    if (oldMethod.equals(oldMethodInvocation.resolveMethod())) {
                        //recursive invocation
                        for (PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            PsiMethodCallExpression newMethodInvocation = newMethodInvocations.get(i);
                            if (isThisVariable(additionalArgument)) {
                                String sourceTypeName = sourceTypeDeclaration.getName();
                                String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1);
                                newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(modifiedSourceTypeName, null));
                            } else {
                                if (additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getName());
                                    newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(parameterName, null));
                                } else {
                                    newMethodInvocation.getArgumentList().add(factory.createExpressionFromText(additionalArgument.getName(), null));
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
            extractedClass.addBefore(cloneMethod, extractedClass.getRBrace());
            PsiClass cloneableInterfaceTypeBinding = implementsCloneableInterface(sourceTypeDeclaration);
            if (cloneableInterfaceTypeBinding != null) {
                PsiClassType cloneableInterfaceType = RefactoringUtility.generateTypeFromTypeBinding(cloneableInterfaceTypeBinding, factory);
                extractedClass.getImplementsList().add(factory.createReferenceElementByType(cloneableInterfaceType));
            } else {
                extractedClass.getImplementsList().add(factory.createExpressionFromText("Cloneable", null));
            }
        }

        compileResults(sourceFile, extractedClassFile);
    }

    private void compileResults(final PsiJavaFile sourceFile, final PsiJavaFile extractedClassFile) {
        WriteCommandAction.writeCommandAction(project).run(() -> {
            CodeStyleManager.getInstance(project).reformat(extractedClassFile);
            sourceFile.getContainingDirectory().add(extractedClassFile);
            FileContentUtil.reparseFiles(project, Collections.singleton(sourceFile.getContainingDirectory().getVirtualFile()), false);
        });
    }

    private PsiMethod createCloneMethod() {
        // check if source class contains clone method
        PsiMethod cloneMethodBinding = null;
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
            PsiMethod cloneMethodDeclaration = factory.createMethod(cloneMethodBinding.getName(), cloneMethodBinding.getReturnType());
            cloneMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

            PsiMethodCallExpression superCloneMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText("super." + cloneMethodBinding.getName() + "()", sourceTypeDeclaration);
            PsiReturnStatement returnStatement = (PsiReturnStatement) factory.createStatementFromText("return (" + extractedTypeName + ") " + superCloneMethodInvocation.getText(), sourceTypeDeclaration);

            StringBuilder returnStatementText = new StringBuilder();

            if (cloneMethodBinding.getThrowsList().getReferenceElements().length != 0) {
                returnStatementText.append("try {\n").append(returnStatement.getText()).append("\n").append("} catch (CloneNotSupportedException e) {\n").append("throw new InternalError(\"Failed to implement Cloneable interface\");\n").append("}");
            }

            cloneMethodDeclaration.getBody().addBefore(factory.createStatementFromText(returnStatementText.toString(), null), cloneMethodDeclaration.getBody().getRBrace());
            return cloneMethodDeclaration;
        }
        return null;
    }

    private double getAverageLevelOfTargets(PsiElement methodBindingKey, Map<PsiMethod, Integer> levelMap, AdjacencyList adjacency) {
        Node n = new Node(methodBindingKey);
        LinkedHashSet<Edge> edges = adjacency.getAdjacent(n);
        int levelSum = 0;
        int targetSum = 0;
        for (Edge edge : edges) {
            Node target = edge.getTarget();
            for (PsiMethod methodDeclaration : levelMap.keySet()) {
                int level = levelMap.get(methodDeclaration);
                if (methodDeclaration.equals(target.getName())) {
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
            if (extractedMethod.equals(methodBinding))
                return extractedMethod;
        }
        return null;
    }

    private PsiMethod createExtractedMethodDeclaration(PsiMethod extractedMethod) {
        PsiMethod newMethodDeclaration = (PsiMethod) extractedMethod.copy();

        newMethodDeclaration = (PsiMethod) newMethodDeclaration.setName(extractedMethod.getName());

        newMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        for (PsiAnnotation annotation : newMethodDeclaration.getAnnotations()) {
            if (annotation instanceof MarkerAnnotation && annotation.getQualifiedName() != null && annotation.getQualifiedName().equals("Test")) {
                annotation.delete();
            }
        }

        modifySourceMemberAccessesInTargetClass(extractedMethod, newMethodDeclaration);
        modifySourceStaticFieldInstructionsInTargetClass(extractedMethod, newMethodDeclaration);
        return newMethodDeclaration;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean variableBindingCorrespondsToExtractedField(PsiVariable variableBinding) {
        for (PsiField extractedFieldFragment : extractedFieldFragments) {
            if (extractedFieldFragment.equals(variableBinding))
                return true;
        }
        return false;
    }

    private boolean methodBindingCorrespondsToExtractedMethod(PsiMethod methodBinding) {
        for (PsiMethod extractedMethod : extractedMethods) {
            if (extractedMethod.equals(methodBinding))
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
            if (variableBinding.equals(fieldDeclaration)) {
                String methodName = fieldDeclaration.getName();
                methodName = SETTER_PREFIX + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = factory.createMethodFromText("public void " + methodName + "(" + fieldDeclaration.getType().getPresentableText() + " " + fieldDeclaration.getName() + ") {\n"
                        + "this." + fieldDeclaration.getName() + " = " + fieldDeclaration.getName() + ";\n" + "}", sourceTypeDeclaration);

                sourceTypeDeclaration.addBefore(psiMethod, sourceTypeDeclaration.getRBrace());
            }
        }
    }

    private void createGetterMethodInSourceClass(PsiVariable variableBinding) {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            if (variableBinding.equals(fieldDeclaration)) {
                String methodName = fieldDeclaration.getName();
                methodName = GETTER_PREFIX + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = factory.createMethodFromText("public " + fieldDeclaration.getType().getPresentableText() + " " + methodName + "() {\n"
                        + "return " + fieldDeclaration.getName() + ";\n"
                        + "}", sourceTypeDeclaration);

                sourceTypeDeclaration.addBefore(psiMethod, sourceTypeDeclaration.getRBrace());
            }
        }
    }

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
        List<PsiReferenceExpression> newFieldInstructions = new ArrayList<>();
        List<PsiExpression> newAssignments = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> {
            newMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));

            for (PsiExpression expression : expressionExtractor.getVariableInstructions(statement)) {
                newFieldInstructions.add((PsiReferenceExpression) expression);
            }

            newAssignments.addAll(expressionExtractor.getAssignments(statement));
        });

        PsiParameter sourceClassParameter = null;
        boolean sourceClassParameterShouldBeFinal = false;
        Map<PlainVariable, PsiParameter> fieldParameterMap = new LinkedHashMap<>();
        Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<>();
        String sourceTypeName = sourceTypeDeclaration.getName();
        String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1);

        int i = 0;
        for (PsiExpression expression : sourceAssignments) {
            PsiAssignmentExpression oldAssignment = (PsiAssignmentExpression) expression;
            PsiAssignmentExpression newAssignment = (PsiAssignmentExpression) newAssignments.get(i);
            PsiExpression oldLeftHandSide = oldAssignment.getLExpression();
            PsiExpression newLeftHandSide = newAssignment.getLExpression();

            PsiVariable oldAssignedVariable = null;
            if (oldLeftHandSide instanceof PsiReferenceExpression) {
                oldAssignedVariable = (PsiVariable) ((PsiReferenceExpression) oldLeftHandSide).resolve();
            } //TODO does it works with Field access?

            PsiExpression oldRightHandSide = oldAssignment.getLExpression();
            PsiExpression newRightHandSide = newAssignment.getRExpression();
            if (oldAssignedVariable != null) {
                if (oldAssignedVariable instanceof PsiField && !oldAssignedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    PsiField oldAssignedField = (PsiField) oldAssignedVariable;
                    if (declaredInSourceTypeDeclarationOrSuperclass(oldAssignedField)) {
                        if (!variableBindingCorrespondsToExtractedField(oldAssignedField)) {
                            PsiMethod setterMethodBinding = findSetterMethodInSourceClass(oldAssignedField);
                            Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                            Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                            if (isParentAnonymousClassDeclaration(oldAssignment.getNode()))
                                sourceClassParameterShouldBeFinal = true;
                            if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                addThisVariable(additionalArgumentsAddedToMovedMethod);
                                additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                            }

                            PsiMethodCallExpression setterMethodInvocation;
                            String setterMethodInvocationName;
                            if (setterMethodBinding != null) {
                                setterMethodInvocationName = setterMethodBinding.getName();
                            } else {
                                if (!sourceFieldBindingsWithCreatedSetterMethod.contains(oldAssignedField)) {
                                    createSetterMethodInSourceClass(oldAssignedField);
                                    sourceFieldBindingsWithCreatedSetterMethod.add(oldAssignedField);
                                }
                                String originalFieldName = oldAssignedField.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                setterMethodInvocationName = setterMethodName;
                            }

                            String setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationName + "(";

                            if (!newAssignment.getOperationSign().equals(JavaTokenType.EQ)) {
                                PsiMethod getterMethodBinding = findGetterMethodInSourceClass(oldAssignedField);
                                PsiMethodCallExpression getterMethodInvocation;
                                String getterMethodName;
                                if (getterMethodBinding != null) {
                                    getterMethodName = getterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(oldAssignedField)) {
                                        createGetterMethodInSourceClass(oldAssignedField);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(oldAssignedField);
                                    }
                                    String originalFieldName = oldAssignedField.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                }

                                String getterMethodInvocationName = modifiedSourceTypeName + "." + getterMethodName;

                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationName + "()", sourceMethod);
                                PsiExpression infixExpression = factory.createExpressionFromText(getterMethodInvocation.getText() + " " + newAssignment.getOperationSign().getText() + " " + newAssignment.getRExpression().getText(), sourceMethod);

                                setterMethodInvocationText += infixExpression.getText() + ")";
                            } else {
                                setterMethodInvocationText += newAssignment.getRExpression().getText() + ")";
                            }

                            setterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(setterMethodInvocationText, sourceMethod);
                            newAssignment.replace(setterMethodInvocation);
                        }
                    }
                }
            } else {
                //if an assigned field is not found in left hand side, then replace all accessed fields in left hand side
                int j = 0;
                List<PsiExpression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldLeftHandSide);
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newLeftHandSide);
                for (PsiExpression expression2 : oldAccessedVariables) {
                    PsiReferenceExpression oldAccessedVariableReferenceExpression = (PsiReferenceExpression) expression2;
                    PsiVariable oldAccessedVariable = (PsiVariable) oldAccessedVariableReferenceExpression.resolve();

                    PsiReferenceExpression newAccessedVariableReferenceExpression = (PsiReferenceExpression) newAccessedVariables.get(j);
                    if (oldAccessedVariable != null) {
                        if (oldAccessedVariable instanceof PsiField && !oldAccessedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                            PsiField oldAccessedField = (PsiField) oldAccessedVariable;
                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) oldAccessedVariable)) {
                                if (!variableBindingCorrespondsToExtractedField(oldAccessedVariable)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(oldAccessedField)) {
                                        if (isParentAnonymousClassDeclaration(oldAssignment.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariableReferenceExpression,
                                                oldAccessedVariableReferenceExpression);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(oldAccessedVariable), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
                                                newAccessedVariableReferenceExpression,
                                                oldAccessedVariable);
                                    }
                                }
                            }
                        }
                    }
                    j++;
                }
            }
            int j = 0;
            List<PsiExpression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldRightHandSide);
            List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newRightHandSide);
            for (PsiExpression expression2 : oldAccessedVariables) {
                PsiReferenceExpression oldAccessedVariableReferenceExpression = (PsiReferenceExpression) expression2;
                PsiVariable oldAccessedVariable = (PsiVariable) oldAccessedVariableReferenceExpression.resolve();

                PsiReferenceExpression newAccessedVariableReferenceExpression = (PsiReferenceExpression) newAccessedVariables.get(j);
                if (oldAccessedVariable instanceof PsiVariable) {
                    if (oldAccessedVariable instanceof PsiField && (!oldAccessedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField oldAccessedField = (PsiField) oldAccessedVariable;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) oldAccessedVariable)) {
                            if (!variableBindingCorrespondsToExtractedField(oldAccessedVariable)) {
                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(oldAccessedField)) {
                                    if (isParentAnonymousClassDeclaration(oldAssignment.getNode()))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            newAccessedVariableReferenceExpression,
                                            oldAccessedVariableReferenceExpression);
                                } else {
                                    if (isParentAnonymousClassDeclaration(oldAccessedVariable.getNode()))
                                        fieldParameterFinalMap.put(new PlainVariable(oldAccessedVariable), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            fieldParameterMap,
                                            newAccessedVariableReferenceExpression, oldAccessedVariable);
                                }
                            }
                        }
                    }
                }
                j++;
            }
            i++;
        }

        List<PsiExpression> sourcePostfixExpressions = expressionExtractor.getPostfixExpressions(sourceMethod.getBody().getStatements());
        List<PsiExpression> newPostfixExpressions = expressionExtractor.getPostfixExpressions(newMethodDeclaration.getBody().getStatements());
        i = 0;
        for (PsiExpression expression : sourcePostfixExpressions) {
            PsiPostfixExpression oldPostfixExpression = (PsiPostfixExpression) expression;
            PsiPostfixExpression newPostfixExpression = (PsiPostfixExpression) newPostfixExpressions.get(i);
            PsiExpression oldOperand = oldPostfixExpression.getOperand();
            PsiExpression newOperand = newPostfixExpression.getOperand();
            PsiReferenceExpression oldAssignedVariable = null;
            PsiElement oldOperandResolved = ((PsiReferenceExpression) oldOperand).resolve();
            if (oldOperandResolved instanceof PsiField) {
                oldAssignedVariable = (PsiReferenceExpression) oldOperand;
            } else if (oldOperand instanceof PsiVariable) {
                oldAssignedVariable = (PsiReferenceExpression) oldOperand;
            }
            if (oldAssignedVariable != null) {
                PsiElement binding = oldAssignedVariable.resolve();
                if (binding instanceof PsiVariable) {
                    PsiVariable variableBinding = (PsiVariable) binding;
                    if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField variableBindingField = (PsiField) variableBinding;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                PsiMethod setterMethodBinding = findSetterMethodInSourceClass((PsiField) variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(oldPostfixExpression.getNode()))
                                    sourceClassParameterShouldBeFinal = true;
                                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }

                                String setterMethodInvocationText;

                                if (setterMethodBinding != null) {
                                    setterMethodInvocationText = setterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBindingField)) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    setterMethodInvocationText = setterMethodName;
                                }

                                setterMethodInvocationText += "(";
                                PsiMethod getterMethodBinding = findGetterMethodInSourceClass((PsiField) variableBinding);

                                String getterMethodInvocationText;
                                if (getterMethodBinding != null) {
                                    getterMethodInvocationText = getterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBindingField)) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    getterMethodInvocationText = getterMethodName;
                                }
                                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText + "()";

                                String infixExpressionText = getterMethodInvocationText;
                                if (newPostfixExpression.getOperationSign().getText().equals("++")) {
                                    infixExpressionText += "+";
                                } else if (newPostfixExpression.getOperationSign().getText().equals("--")) {
                                    infixExpressionText += "-";
                                }
                                infixExpressionText += "1";
                                setterMethodInvocationText += infixExpressionText + ")";
                                setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationText;
                                newPostfixExpression.replace(factory.createExpressionFromText(setterMethodInvocationText, sourceMethod));
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<PsiExpression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (PsiExpression expression2 : oldAccessedVariables) {
                    PsiReferenceExpression oldAccessedVariable = (PsiReferenceExpression) expression2;
                    PsiReferenceExpression newAccessedVariable = (PsiReferenceExpression) newAccessedVariables.get(j);
                    PsiElement rightHandBinding = oldAccessedVariable.resolve();
                    if (rightHandBinding instanceof PsiVariable) {
                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                        if (accessedVariableBinding instanceof PsiField && (!accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                            PsiField accessedVariableBindingField = (PsiField) accessedVariableBinding;

                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBindingField)) {
                                        if (isParentAnonymousClassDeclaration(oldPostfixExpression.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                oldAccessedVariable);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
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

        List<PsiExpression> sourcePrefixExpressions = expressionExtractor.getPrefixExpressions(sourceMethod.getBody().getStatements());
        List<PsiExpression> newPrefixExpressions = expressionExtractor.getPrefixExpressions(newMethodDeclaration.getBody().getStatements());
        i = 0;
        for (PsiExpression expression : sourcePrefixExpressions) {
            PsiPrefixExpression oldPrefixExpression = (PsiPrefixExpression) expression;
            PsiPrefixExpression newPrefixExpression = (PsiPrefixExpression) newPrefixExpressions.get(i);
            PsiExpression oldOperand = oldPrefixExpression.getOperand();
            PsiExpression newOperand = newPrefixExpression.getOperand();
            PsiJavaToken oldOperator = oldPrefixExpression.getOperationSign();
            PsiJavaToken newOperator = newPrefixExpression.getOperationSign();
            PsiReferenceExpression oldAssignedVariable = null;

            if (oldOperand instanceof PsiReferenceExpression) {
                if (oldOperand instanceof PsiField) {
                    oldAssignedVariable = (PsiReferenceExpression) oldOperand;
                }
            }

            if (oldAssignedVariable != null && (oldOperator.getText().equals("++")) ||
                    oldOperator.getText().equals("--")) {
                PsiElement binding = oldAssignedVariable.resolve();
                if (binding instanceof PsiVariable) {
                    PsiVariable variableBinding = (PsiVariable) binding;
                    if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField variableBindingField = (PsiField) variableBinding;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                PsiMethod setterMethodBinding = findSetterMethodInSourceClass((PsiField) variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(oldPrefixExpression.getNode()))
                                    sourceClassParameterShouldBeFinal = true;
                                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }

                                String setterMethodInvocationText;

                                if (setterMethodBinding != null) {
                                    setterMethodInvocationText = setterMethodBinding.getText();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBindingField)) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    setterMethodInvocationText = setterMethodName;
                                }
                                PsiMethod getterMethodBinding = findGetterMethodInSourceClass((PsiField) variableBinding);
                                String getterMethodInvocationText;
                                if (getterMethodBinding != null) {
                                    getterMethodInvocationText = getterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBindingField)) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    getterMethodInvocationText = getterMethodName;
                                }
                                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText + "()";
                                String infixExpressionText = getterMethodInvocationText;

                                if (newOperator.getText().equals("++")) {
                                    infixExpressionText += " + 1";
                                } else if (newOperator.getText().equals("--")) {
                                    infixExpressionText += "- 1";
                                }
                                setterMethodInvocationText += "(" + infixExpressionText + ")";
                                setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationText;
                                newPrefixExpression.replace(factory.createExpressionFromText(setterMethodInvocationText, sourceMethod));
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<PsiExpression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (PsiExpression expression2 : oldAccessedVariables) {
                    PsiReferenceExpression oldAccessedVariable = (PsiReferenceExpression) expression2;
                    PsiReferenceExpression newAccessedVariable = (PsiReferenceExpression) newAccessedVariables.get(j);
                    PsiElement rightHandBinding = oldAccessedVariable.resolve();
                    if (rightHandBinding instanceof PsiVariable) {
                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                        if (accessedVariableBinding instanceof PsiField && (!accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                            PsiField accessedVariableBindingField = (PsiField) accessedVariableBinding;
                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBindingField)) {
                                        if (isParentAnonymousClassDeclaration(oldPrefixExpression.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                oldAccessedVariable);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(oldAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
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
        for (PsiExpression expression : sourceFieldInstructions) {
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            PsiElement binding = simpleName.resolve();
            if (binding instanceof PsiVariable) {
                PsiVariable variableBinding = (PsiVariable) binding;
                if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                    PsiField variableBindingField = (PsiField) variableBinding;
                    if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                        if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                            if (!isAssignmentChild(expression)) {
                                PsiReferenceExpression expressionName = newFieldInstructions.get(i);
                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(variableBindingField)) {
                                    if (isParentAnonymousClassDeclaration(simpleName.getNode()))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod,
                                            newMethodDeclaration,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            expressionName,
                                            simpleName);
                                } else {
                                    if (isParentAnonymousClassDeclaration(simpleName.getNode()))
                                        fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            fieldParameterMap,
                                            expressionName, variableBinding);
                                }
                            }
                        }
                    } else {
                        PsiField fieldBinding = (PsiField) variableBinding;

                        PsiClass superclassTypeBinding = sourceTypeDeclaration.getSuperClass();

                        while (superclassTypeBinding != null && !superclassTypeBinding.equals(fieldBinding.getContainingClass())) {
                            superclassTypeBinding = superclassTypeBinding.getSuperClass();
                        }
                        if (superclassTypeBinding != null) {
                            PsiField[] superclassFieldBindings = superclassTypeBinding.getFields();
                            for (PsiField superclassFieldBinding : superclassFieldBindings) {
                                if (superclassFieldBinding.equals(variableBinding)) {
                                    if (!isAssignmentChild(expression)) {
                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                        Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                        if (isParentAnonymousClassDeclaration(simpleName.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                        if (!containsVariable(variableBinding, additionalArgumentsAddedToMovedMethod)) {
                                            PsiParameter fieldParameter = addParameterToMovedMethod(newMethodDeclaration, variableBinding);
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
        for (PsiExpression expression : sourceMethodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                if (methodInvocation.getMethodExpression() == null || methodInvocation.getMethodExpression() instanceof PsiThisExpression) {
                    PsiMethod methodBinding = methodInvocation.resolveMethod();
                    if (methodBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                        PsiMethod[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
                        for (PsiMethod sourceMethodDeclaration : sourceMethodDeclarations) {
                            if (sourceMethodDeclaration.equals(methodInvocation.resolveMethod())) {
                                if (!methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethod()) &&
                                        !sourceMethod.equals(methodInvocation.resolveMethod())) {
                                    PsiField fieldName = (PsiField) MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    PsiModifierList modifiers = sourceMethodDeclaration.getModifierList();
                                    PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) newMethodInvocations.get(j);
                                    if (modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                                        newMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(sourceTypeDeclaration.getName() + "." + newMethodInvocation.getText(), sourceMethod);
                                        newMethodInvocations.set(j, newMethodInvocation);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethod())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethod(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethod());
                                        }
                                    } else if (fieldName != null) {
                                        if (!variableBindingCorrespondsToExtractedField(fieldName)) {
                                            if (fieldName.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                String qualifier = sourceTypeDeclaration.getName();
                                                PsiExpression qualifiedName = factory.createExpressionFromText(qualifier + "." + fieldName.getName(), sourceMethod);
                                                newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(qualifiedName);
                                                newMethodInvocations.set(j, newMethodInvocation);
                                                setPublicModifierToSourceField(fieldName);
                                            } else {
                                                String parameterNameString = createNameForParameterizedFieldAccess(fieldName.getName());
                                                newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(factory.createExpressionFromText(parameterNameString, sourceMethod));
                                                newMethodInvocations.set(j, newMethodInvocation);

                                                if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                                    fieldParameterFinalMap.put(new PlainVariable(fieldName), true);
                                                if (!containsVariable(fieldName, additionalArgumentsAddedToMovedMethod)) {
                                                    PsiParameter fieldParameter = addParameterToMovedMethod(newMethodDeclaration, fieldName);
                                                    addVariable(fieldName, additionalArgumentsAddedToMovedMethod);
                                                    additionalParametersAddedToMovedMethod.add(fieldParameter);
                                                    fieldParameterMap.put(new PlainVariable(fieldName), fieldParameter);
                                                }
                                            }
                                        } else {
                                            newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(factory.createExpressionFromText(fieldName.getText(), sourceMethod));
                                            newMethodInvocations.set(j, newMethodInvocation);
                                        }
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }

                                        newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(factory.createExpressionFromText(modifiedSourceTypeName + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newMethodInvocation);

                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethod())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethod(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethod());
                                        }
                                    }
                                } else {
                                    if (!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                                        LinkedHashSet<PsiMethodCallExpression> methodInvocations = new LinkedHashSet<>();
                                        methodInvocations.add(methodInvocation);
                                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                                    } else {
                                        Set<PsiMethodCallExpression> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                                        methodInvocations.add(methodInvocation);
                                    }
                                    if (!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                                        LinkedHashSet<PsiMethodCallExpression> methodInvocations = new LinkedHashSet<>();
                                        methodInvocations.add((PsiMethodCallExpression) newMethodInvocations.get(j));
                                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                                    } else {
                                        Set<PsiMethodCallExpression> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                                        methodInvocations.add((PsiMethodCallExpression) newMethodInvocations.get(j));
                                    }
                                }
                            }
                        }
                    } else {
                        PsiClass superclassType = sourceTypeDeclaration.getSuperClass();
                        PsiClass superclassTypeBinding = null;
                        if (superclassType != null)
                            superclassTypeBinding = superclassType;
                        while (superclassTypeBinding != null && !methodBinding.getContainingClass().equals(superclassTypeBinding)) {
                            superclassTypeBinding = superclassTypeBinding.getSuperClass();
                        }
                        if (superclassTypeBinding != null) {
                            PsiMethod[] superclassMethodBindings = superclassTypeBinding.getMethods();
                            for (PsiMethod superclassMethodBinding : superclassMethodBindings) {
                                if (superclassMethodBinding.equals(methodBinding)) {
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) newMethodInvocations.get(j);
                                    if (superclassMethodBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                        String qualifier = sourceTypeDeclaration.getName();
                                        newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(factory.createExpressionFromText(qualifier + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newMethodInvocation);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }

                                        newMethodInvocation = (PsiMethodCallExpression) newMethodInvocation.replace(factory.createExpressionFromText(modifiedSourceTypeName + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newMethodInvocation);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodBinding)) {
                                            PsiClass superclassTypeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(superclassMethodBinding, sourceTypeDeclaration);
                                            if (superclassTypeDeclaration != null) {
                                                setPublicModifierToSourceMethod(methodInvocation.resolveMethod(), superclassTypeDeclaration);
                                            }
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodBinding);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethod())) {
                    if (!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                        LinkedHashSet<PsiMethodCallExpression> methodInvocations = new LinkedHashSet<>();
                        methodInvocations.add(methodInvocation);
                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                    } else {
                        Set<PsiMethodCallExpression> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                        methodInvocations.add(methodInvocation);
                    }
                    if (!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                        LinkedHashSet<PsiMethodCallExpression> methodInvocations = new LinkedHashSet<>();
                        methodInvocations.add((PsiMethodCallExpression) newMethodInvocations.get(j));
                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                    } else {
                        Set<PsiMethodCallExpression> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                        methodInvocations.add((PsiMethodCallExpression) newMethodInvocations.get(j));
                    }
                    if (methodInvocation.getMethodExpression() != null && ((PsiMethod) methodInvocation.getMethodExpression()).getContainingClass().equals(sourceTypeDeclaration)) {
                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethod())) {
                            setPublicModifierToSourceMethod(methodInvocation.resolveMethod(), sourceTypeDeclaration);
                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethod());
                        }
                    }
                }
            }
            j++;
        }
        //replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments
        int k = 0;
        for (PsiExpression invocation : newMethodInvocations) {
            if (invocation instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) invocation;
                PsiExpression[] arguments = methodInvocation.getArgumentList().getExpressions();
                for (PsiExpression argument : arguments) {
                    if (argument instanceof PsiThisExpression) {
                        if (isParentAnonymousClassDeclaration(sourceMethodInvocations.get(k).getNode()))
                            sourceClassParameterShouldBeFinal = true;
                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                        Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                        }
                        argument.replace(factory.createExpressionFromText(modifiedSourceTypeName, sourceMethod));
                    }
                }
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments
        List<PsiExpression> sourceClassInstanceCreations = expressionExtractor.getClassInstanceCreations(sourceMethod.getBody().getStatements());
        List<PsiExpression> newClassInstanceCreations = expressionExtractor.getClassInstanceCreations(newMethodDeclaration.getBody().getStatements());
        k = 0;
        for (PsiExpression creation : newClassInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) creation;
            PsiExpression[] arguments = classInstanceCreation.getArgumentList().getExpressions();
            for (PsiExpression argument : arguments) {
                if (argument instanceof PsiThisExpression) {
                    if (isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k).getNode()))
                        sourceClassParameterShouldBeFinal = true;
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }

                    argument.replace(factory.createExpressionFromText(modifiedSourceTypeName, sourceMethod));
                }
            }
            PsiNewExpression oldClassInstanceCreation = (PsiNewExpression) sourceClassInstanceCreations.get(k);
            PsiClass classInstanceCreationTypeBinding = (PsiClass) oldClassInstanceCreation.getClassReference().resolve();
            if (classInstanceCreationTypeBinding.getContainingClass() != null && oldClassInstanceCreation.getAnonymousClass() == null &&
                    sourceTypeDeclaration.equals(classInstanceCreationTypeBinding.getContainingClass())) {
                if (isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k).getNode()))
                    sourceClassParameterShouldBeFinal = true;
                if (classInstanceCreationTypeBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    String qualifierType = sourceTypeDeclaration.getName();
                    String qualifiedType = qualifierType + "." + classInstanceCreationTypeBinding.getName();
                    classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(qualifiedType, sourceMethod));
                    setPublicModifierToSourceMemberType(classInstanceCreationTypeBinding);
                } else {
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(modifiedSourceTypeName, sourceMethod));
                    String simpleNameType = oldClassInstanceCreation.getType().getCanonicalText();
                    if (simpleNameType != null) {
                        classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(simpleNameType, sourceMethod));
                    }
                }
            }
            k++;
        }

        //replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiVariable> sourceVariableDeclarationFragments = new ArrayList<>();
        List<PsiVariable> newVariableDeclarationFragments = new ArrayList<>();
        List<PsiStatement> sourceVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(sourceMethod.getBody().getStatements());
        for (PsiStatement statement : sourceVariableDeclarationStatements) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
            PsiVariable[] fragments = (PsiVariable[]) variableDeclarationStatement.getDeclaredElements();
            sourceVariableDeclarationFragments.addAll(Arrays.asList(fragments));
        }

        List<PsiStatement> newVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody().getStatements());
        for (PsiStatement statement : newVariableDeclarationStatements) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
            PsiVariable[] fragments = (PsiVariable[]) variableDeclarationStatement.getDeclaredElements();
            newVariableDeclarationFragments.addAll(Arrays.asList(fragments));
        }

        List<PsiStatement> sourceVariableDeclarationExpressions = statementExtractor.getVariableDeclarationStatements(sourceMethod.getBody().getStatements());
        for (PsiStatement expression : sourceVariableDeclarationExpressions) {
            PsiDeclarationStatement variableDeclarationExpression = (PsiDeclarationStatement) expression;
            PsiVariable[] fragments = (PsiVariable[]) variableDeclarationExpression.getDeclaredElements();
            sourceVariableDeclarationFragments.addAll(Arrays.asList(fragments));
        }

        List<PsiStatement> newVariableDeclarationExpressions = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody().getStatements());
        for (PsiStatement expression : newVariableDeclarationExpressions) {
            PsiDeclarationStatement variableDeclarationExpression = (PsiDeclarationStatement) expression;
            PsiVariable[] fragments = (PsiVariable[]) variableDeclarationExpression.getDeclaredElements();
            newVariableDeclarationFragments.addAll(Arrays.asList(fragments));
        }

        k = 0;
        for (PsiVariable fragment : newVariableDeclarationFragments) {
            PsiExpression initializer = fragment.getInitializer();
            if (initializer instanceof PsiThisExpression) {
                if (isParentAnonymousClassDeclaration(sourceVariableDeclarationFragments.get(k).getNode()))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                fragment.getInitializer().replace(factory.createExpressionFromText(modifiedSourceTypeName, sourceMethod));
            }
            k++;
        }

        //replaceThisExpressionWithSourceClassParameterInReturnStatementExpressions
        List<PsiStatement> sourceReturnStatements = statementExtractor.getReturnStatements(sourceMethod.getBody().getStatements());
        List<PsiStatement> newReturnStatements = statementExtractor.getReturnStatements(newMethodDeclaration.getBody().getStatements());
        k = 0;
        for (PsiStatement statement : newReturnStatements) {
            PsiReturnStatement newReturnStatement = (PsiReturnStatement) statement;
            if (newReturnStatement.getReturnValue() instanceof PsiThisExpression) {
                if (isParentAnonymousClassDeclaration(sourceReturnStatements.get(k).getNode()))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                newReturnStatement.getReturnValue().replace(factory.createExpressionFromText(modifiedSourceTypeName, sourceMethod));
            }
            k++;
        }
        if (sourceClassParameter != null && sourceClassParameterShouldBeFinal) {
            sourceClassParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
        }
        for (PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
            if (fieldParameterFinalMap.get(fieldName)) {
                PsiParameter fieldParameter = fieldParameterMap.get(fieldName);
                if (fieldParameter != null) {
                    fieldParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
                }
            }
        }
    }

    private void handleAccessedFieldNotHavingSetterMethod(PsiMethod sourceMethod,
                                                          PsiMethod newMethodDeclaration,
                                                          Map<PlainVariable, PsiParameter> fieldParameterMap, PsiReferenceExpression newAccessedVariable, PsiVariable accessedVariableBinding) {
        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
        if (!containsVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod)) {
            PsiParameter fieldParameter = addParameterToMovedMethod(newMethodDeclaration, accessedVariableBinding);
            addVariable(accessedVariableBinding, additionalArgumentsAddedToMovedMethod);
            additionalParametersAddedToMovedMethod.add(fieldParameter);
            fieldParameterMap.put(new PlainVariable(accessedVariableBinding), fieldParameter);
        }
        if (newAccessedVariable.resolve() instanceof PsiField) {
            if (newAccessedVariable.getQualifierExpression() instanceof PsiThisExpression) {
                String parameterName = createNameForParameterizedFieldAccess(((PsiField) newAccessedVariable.resolve()).getName());
                newAccessedVariable.getLastChild().replace(factory.createExpressionFromText(parameterName, sourceMethod));
            }
        } else {
            if (!(newAccessedVariable instanceof PsiQualifiedExpression)) {
                String parameterName = createNameForParameterizedFieldAccess(accessedVariableBinding.getName());
                newAccessedVariable.replace(factory.createExpressionFromText(parameterName, sourceMethod));
            }
        }
    }

    private PsiParameter handleAccessedFieldHavingSetterMethod(PsiMethod sourceMethod,
                                                               PsiMethod newMethodDeclaration, PsiParameter sourceClassParameter,
                                                               String modifiedSourceTypeName, PsiReferenceExpression newAccessedVariable, PsiReferenceExpression accessedVariableBinding) {
        PsiMethod getterMethodBinding = findGetterMethodInSourceClass((PsiField) accessedVariableBinding.resolve());
        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
            addThisVariable(additionalArgumentsAddedToMovedMethod);
            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
        }
        PsiMethodCallExpression getterMethodInvocation;
        String getterMethodInvocationText;
        if (getterMethodBinding != null) {
            getterMethodInvocationText = getterMethodBinding.getName() + "()";
        } else {
            PsiField accessedVariable = (PsiField) accessedVariableBinding.resolve();

            if (!sourceFieldBindingsWithCreatedGetterMethod.contains(accessedVariable)) {
                createGetterMethodInSourceClass(accessedVariable);
                sourceFieldBindingsWithCreatedGetterMethod.add(accessedVariable);
            }
            String originalFieldName = accessedVariable.getName();
            String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
            String getterMethodName = GETTER_PREFIX + modifiedFieldName;
            getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
            getterMethodInvocationText = getterMethodName;
        }

        if (newAccessedVariable.resolve() instanceof PsiField) {
            if (newAccessedVariable.getQualifierExpression() instanceof PsiThisExpression) {
                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText;
                newAccessedVariable.replace(factory.createExpressionFromText(getterMethodInvocationText, sourceMethod));
            }
        } else if (newAccessedVariable instanceof PsiQualifiedExpression) {
            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, sourceMethod);
            newAccessedVariable.replace(getterMethodInvocation);
        } else {
            getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText;
            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, sourceMethod);
            newAccessedVariable.replace(getterMethodInvocation);
        }
        return sourceClassParameter;
    }

    private void addVariable(PsiVariable variableBinding, Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
        PlainVariable variable = new PlainVariable(variableBinding);
        additionalArgumentsAddedToMovedMethod.add(variable);
    }

    private void addThisVariable(Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
        PlainVariable variable = new PlainVariable(sourceTypeDeclaration, "this", "this", false, false, false);
        additionalArgumentsAddedToMovedMethod.add(variable);
    }

    private boolean isThisVariable(PlainVariable argument) {
        return argument.getName().equals("this");
    }

    private boolean containsThisVariable(Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
            if (isThisVariable(argument)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsVariable(PsiVariable variableBinding, Set<PlainVariable> additionalArgumentsAddedToMovedMethod) {
        for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
            if (argument.getOrigin().equals(variableBinding)) {
                return true;
            }
        }
        return false;
    }

    private boolean declaredInSourceTypeDeclarationOrSuperclass(PsiField variableBinding) {
        return RefactoringUtility.findDeclaringTypeDeclaration(variableBinding, sourceTypeDeclaration) != null;
    }

    private PsiParameter addSourceClassParameterToMovedMethod(PsiMethod newMethodDeclaration) {
        PsiParameter parameter;
        String sourceTypeName = sourceTypeDeclaration.getName();
        parameter = factory.createParameter(sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1), factory.createType(sourceTypeDeclaration));
        parameter = (PsiParameter) newMethodDeclaration.getParameterList().add(parameter);
        return parameter;
    }

    private PsiParameter addParameterToMovedMethod(PsiMethod newMethodDeclaration, PlainVariable additionalArgument) {
        PsiParameter parameter;
        PsiField field = RefactoringUtility.findFieldDeclaration(additionalArgument, sourceTypeDeclaration);
        PsiType fieldType = field.getType();
        String parameterName;
        if (additionalArgument.isField()) {
            //adding "this" prefix to avoid collisions with other parameter names
            parameterName = createNameForParameterizedFieldAccess(field.getName());
        } else {
            parameterName = field.getName();
        }
        parameter = factory.createParameter(parameterName, fieldType);
        parameter = (PsiParameter) newMethodDeclaration.getParameterList().add(parameter);
        return parameter;
    }

    private PsiParameter addParameterToMovedMethod(PsiMethod newMethodDeclaration, PsiVariable variableBinding) {
        PsiParameter parameter;
        String parameterName;
        if (variableBinding instanceof PsiField) {
            //adding "this" prefix to avoid collisions with other parameter names
            parameterName = createNameForParameterizedFieldAccess(variableBinding.getName());
        } else {
            parameterName = variableBinding.getName();
        }
        parameter = factory.createParameter(parameterName, variableBinding.getType());
        parameter = (PsiParameter) newMethodDeclaration.getParameterList().add(parameter);
        return parameter;
    }

    private String createNameForParameterizedFieldAccess(String fieldName) {
        return "this" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }


    private void setPublicModifierToSourceMethod(PsiMethod methodBinding, PsiClass sourceTypeDeclaration) {
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        for (PsiMethod methodDeclaration : methodDeclarations) {
            if (methodDeclaration.equals(methodBinding)) {
                methodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
            }
        }
    }

    private void modifySourceStaticFieldInstructionsInTargetClass(PsiMethod sourceMethod,
                                                                  PsiMethod newMethodDeclaration) {
        ExpressionExtractor extractor = new ExpressionExtractor();
        List<PsiExpression> sourceVariableInstructions = extractor.getVariableInstructions(sourceMethod.getBody().getStatements());
        List<PsiExpression> newVariableInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody().getStatements());
        int i = 0;
        for (PsiExpression expression : sourceVariableInstructions) {
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            PsiVariable binding = (PsiVariable) simpleName.resolve();
            if (binding instanceof PsiVariable) {
                if (binding instanceof PsiField && (binding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                    if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) binding)) {
                        String qualifier = sourceTypeDeclaration.getName();
                        if (simpleName.resolve() instanceof PsiField) {
                            PsiReferenceExpression fieldAccess = (PsiReferenceExpression) newVariableInstructions.get(i);
                            fieldAccess.setQualifierExpression(fieldAccess);
                        } else if (RefactoringUtility.needsQualifier(simpleName)) {
                            PsiReferenceExpression newSimpleName = (PsiReferenceExpression) simpleName.copy();
                            PsiExpression newQualifiedName = factory.createExpressionFromText(qualifier + "." + newSimpleName.getText(), sourceMethod);
                            newVariableInstructions.get(i).replace(newQualifiedName);
                        }
                        setPublicModifierToSourceField(binding);
                    } else {
                        PsiClass qualifier;
                        if (binding.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                            qualifier = ((PsiField) binding).getContainingClass();
                        } else {
                            qualifier = sourceTypeDeclaration;
                        }
                        if (simpleName.resolve() instanceof PsiField) {
                            PsiReferenceExpression fieldAccess = (PsiReferenceExpression) newVariableInstructions.get(i);
                            fieldAccess.setQualifierExpression(factory.createReferenceExpression(qualifier));
                        } else if (RefactoringUtility.needsQualifier(simpleName)) {
                            String newSimpleName = ((PsiVariable) simpleName.resolve()).getName();
                            PsiReferenceExpression newQualifiedName = (PsiReferenceExpression) factory.createExpressionFromText(qualifier.getText() + "." + newSimpleName, sourceMethod);
                            newVariableInstructions.get(i).replace(newQualifiedName);
                        }
                        PsiClass fieldDeclaringClass = ((PsiField) binding).getContainingClass();
                        if (fieldDeclaringClass != null && fieldDeclaringClass.isEnum() && sourceTypeDeclaration.equals(fieldDeclaringClass.getContainingClass())) {
                            setPublicModifierToSourceMemberType(fieldDeclaringClass);
                        }
                    }
                }
            }
            i++;
        }
    }

    private void setPublicModifierToSourceMemberType(PsiClass typeBinding) {
        PsiClass[] bodyDeclarations = sourceTypeDeclaration.getInnerClasses();
        for (PsiClass bodyDeclaration : bodyDeclarations) {
            if (typeBinding.equals(bodyDeclaration)) {
                updateBodyDeclarationAccessModifier(bodyDeclaration);
            }
        }
    }

    private void updateBodyDeclarationAccessModifier(PsiClass memberType) {
        memberType.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
    }

    private void setPublicModifierToSourceField(PsiVariable variableBinding) {
        if (variableBinding instanceof PsiField) {
            variableBinding.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        }
    }

    private PsiMethod createSetterMethodDeclaration(PsiField fieldFragment) {
        String originalFieldName = fieldFragment.getName();
        String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
        PsiMethod setterMethodDeclaration;
        String setterMethodName = SETTER_PREFIX + modifiedFieldName;
        setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);

        setterMethodDeclaration = factory.createMethod(setterMethodName, PsiType.VOID);
        setterMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        setterMethodDeclaration.getParameterList().add(factory.createParameter(fieldFragment.getName(), fieldFragment.getType()));

        if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            setterMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
        }

        PsiAssignmentExpression assignment;
        String assigmentText;
        if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            assigmentText = extractedTypeName;
        } else {
            assigmentText = "this";
        }
        assigmentText += "." + fieldFragment.getName() + " = " + fieldFragment.getName() + ";"; //this.fieldName = fieldName (parameter)
        assignment = (PsiAssignmentExpression) factory.createStatementFromText(assigmentText, sourceFile);
        setterMethodDeclaration.getBody().addBefore(assignment, setterMethodDeclaration.getBody().getRBrace());
        return setterMethodDeclaration;
    }

    private PsiMethod createGetterMethodDeclaration(PsiField fieldFragment) {
        String originalFieldName = fieldFragment.getName();
        String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
        PsiMethod getterMethodDeclaration;
        String getterMethodName = GETTER_PREFIX + modifiedFieldName;
        getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
        getterMethodDeclaration = factory.createMethod(getterMethodName, fieldFragment.getType());
        getterMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            fieldFragment.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
        }
        PsiReturnStatement returnStatement = (PsiReturnStatement) factory.createStatementFromText("return " + fieldFragment.getName() + ";", sourceFile);
        getterMethodDeclaration.getBody().addBefore(returnStatement, getterMethodDeclaration.getBody().getRBrace());
        return getterMethodDeclaration;
    }

    private void createExtractedTypeFieldReferenceInSourceClass() {
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
        if (!constructorFinalFieldAssignmentMap.isEmpty()) {
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();

            String extractedTypeFieldAccessText = "this." + modifiedExtractedTypeName;
            String extractedTypeFieldReferenceAssignmentText = extractedTypeFieldAccessText + " = new " + extractedTypeName + "();";

            PsiStatement assigmentStatement = factory.createStatementFromText(extractedTypeFieldReferenceAssignmentText, null);

            for (PsiMethod constructor : constructorFinalFieldAssignmentMap.keySet()) {
                PsiNewExpression newExpression = (PsiNewExpression) factory.createExpressionFromText("new " + extractedTypeName + "()", sourceFile);
                Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
                Set<PsiParameter> extractedClassConstructorParameters = new LinkedHashSet<>();

                StatementExtractor statementExtractor = new StatementExtractor();
                List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(constructor.getBody().getStatements());
                List<PsiStatement> insertAfterStatements = new ArrayList<>();
                for (PsiField fieldFragment : finalFieldAssignmentMap.keySet()) {
                    PsiAssignmentExpression fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
                    List<PsiExpression> variableInstructions = expressionExtractor.getVariableInstructions(fieldAssignment.getRExpression());
                    TypeVisitor typeVisitor = new TypeVisitor();
                    fieldAssignment.getRExpression().accept(typeVisitor);
                    for (PsiExpression expression : variableInstructions) {
                        PsiReferenceExpression variable = (PsiReferenceExpression) expression;
                        boolean foundInOriginalConstructorParameters = false;
                        PsiParameter[] originalConstructorParameters = constructor.getParameterList().getParameters();
                        for (PsiParameter originalConstructorParameter : originalConstructorParameters) {
                            if (originalConstructorParameter.equals(variable.resolve())) {
                                if (!extractedClassConstructorParameters.contains(originalConstructorParameter)) {
                                    extractedClassConstructorParameters.add(originalConstructorParameter);
                                    foundInOriginalConstructorParameters = true;
                                    break;
                                }
                            }
                        }
                        if (!foundInOriginalConstructorParameters) {
                            boolean foundInVariableDeclarationStatement = false;
                            for (PsiStatement statement : variableDeclarationStatements) {
                                PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
                                PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                                for (PsiElement fragment : fragments) {
                                    if (fragment.equals(variable.resolve())) {
                                        if (extractedClassConstructorParameters.stream().noneMatch(param -> param.getType().equals(((PsiField) fragment).getType()))) {
                                            newExpression.getArgumentList().add(variable.copy());
                                            extractedClassConstructorParameters.add((PsiParameter) fragment);
                                            if (!insertAfterStatements.contains(variableDeclarationStatement)) {
                                                insertAfterStatements.add(variableDeclarationStatement);
                                            }
                                            foundInVariableDeclarationStatement = true;
                                            break;
                                        }
                                    }
                                }
                                if (foundInVariableDeclarationStatement) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!insertAfterStatements.isEmpty()) {
                    PsiStatement lastStatement = insertAfterStatements.get(0);
                    int maxStartPosition = lastStatement.getStartOffsetInParent();
                    for (int i = 1; i < insertAfterStatements.size(); i++) {
                        PsiStatement currentStatement = insertAfterStatements.get(i);
                        if (currentStatement.getStartOffsetInParent() > maxStartPosition) {
                            maxStartPosition = currentStatement.getStartOffsetInParent();
                            lastStatement = currentStatement;
                        }
                    }
                    constructor.getBody().addAfter(assigmentStatement, lastStatement);
                } else {
                    constructor.getBody().addAfter(assigmentStatement, constructor.getBody().getLBrace());
                }
                extractedClassConstructorParameterMap.put(constructor, extractedClassConstructorParameters);
            }
        }
        PsiField extractedReferenceFieldDeclaration = factory.createField(modifiedExtractedTypeName, null);
        extractedReferenceFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
        PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
        if (serializableTypeBinding != null && !existsNonTransientExtractedFieldFragment()) {
            extractedReferenceFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.TRANSIENT, true);
            updateReadObjectInSourceClass(modifiedExtractedTypeName);
            updateWriteObjectInSourceClass(modifiedExtractedTypeName);
        }
        updateCloneInSourceClass(modifiedExtractedTypeName);
        sourceTypeDeclaration.addBefore(extractedReferenceFieldDeclaration, sourceTypeDeclaration.getRBrace());
    }

    private void updateWriteObjectInSourceClass(String modifiedExtractedTypeName) {
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        boolean methodFound = false;
        for (PsiMethod method : methodDeclarations) {
            if (isWriteObject(method)) {
                methodFound = true;
                if (!extractedMethods.contains(method)) {
                    PsiCodeBlock methodBody = method.getBody();
                    if (methodBody != null) {
                        PsiParameterList parameters = method.getParameterList();
                        PsiParameter parameterSimpleName = parameters.getParameters()[0];
                        PsiStatement methodInvocationStatement = createMethodInvocationStatementForWriteObject(modifiedExtractedTypeName, parameterSimpleName.getText());
                        PsiStatement firstStatement = isFirstStatementMethodInvocationExpressionStatementWithName(method, "defaultWriteObject");
                        if (firstStatement != null) {
                            methodBody.addAfter(methodInvocationStatement, firstStatement);
                        } else {
                            methodBody.addAfter(methodInvocationStatement, methodBody.getLBrace());
                        }
                    }
                }
            }
        }
        if (!methodFound) {
            PsiMethod writeObjectMethod = factory.createMethod("writeObject", PsiType.VOID);
            writeObjectMethod.getParameterList().add(factory.createParameter("stream", factory.createTypeFromText("java.io.ObjectOutputStream", sourceFile)));
            writeObjectMethod.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
            writeObjectMethod.getThrowsList().add(factory.createReferenceElementByType(factory.createTypeByFQClassName("java.io.IOException")));

            String methodInvocationText = "defaultWriteObject";
            String parameterSimpleName = "stream";
            methodInvocationText = parameterSimpleName + "." + methodInvocationText + "();";

            writeObjectMethod.getBody().addBefore(factory.createStatementFromText(methodInvocationText, null), writeObjectMethod.getBody().getRBrace());
            PsiStatement methodInvocationStatement2 = createMethodInvocationStatementForWriteObject(modifiedExtractedTypeName, parameterSimpleName);

            writeObjectMethod.getBody().addBefore(methodInvocationStatement2, writeObjectMethod.getBody().getRBrace());

            sourceTypeDeclaration.addBefore(writeObjectMethod, sourceTypeDeclaration.getRBrace());
        }
    }

    private PsiStatement createMethodInvocationStatementForWriteObject(String modifiedExtractedTypeName, String parameterSimpleName) {
        PsiReferenceExpression fieldAccess = (PsiReferenceExpression) factory.createExpressionFromText("this." + modifiedExtractedTypeName, sourceFile);

        PsiMethodCallExpression writeObjectMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(parameterSimpleName + "." + "writeObject(" + fieldAccess.getText() + ")", sourceFile);

        return factory.createStatementFromText(writeObjectMethodInvocation + ";", sourceFile);
    }

    private void updateReadObjectInSourceClass(String modifiedExtractedTypeName) {
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        boolean methodFound = false;
        for (PsiMethod method : methodDeclarations) {
            if (isReadObject(method)) {
                methodFound = true;
                if (!extractedMethods.contains(method)) {
                    PsiCodeBlock methodBody = method.getBody();
                    if (methodBody != null) {
                        PsiParameterList parameters = method.getParameterList();
                        PsiParameter parameterSimpleName = parameters.getParameters()[0];
                        PsiStatement assignmentStatement = createAssignmentStatementForReadObject(modifiedExtractedTypeName, parameterSimpleName.getText());
                        PsiStatement firstStatement = isFirstStatementMethodInvocationExpressionStatementWithName(method, "defaultReadObject");
                        if (firstStatement != null) {
                            methodBody.addAfter(assignmentStatement, firstStatement);
                        } else {
                            methodBody.addAfter(assignmentStatement, methodBody.getLBrace());
                        }
                    }
                }
            }
        }
        if (!methodFound) {
            PsiMethod readObjectMethod = factory.createMethod("readObject", PsiType.VOID);
            readObjectMethod.getParameterList().add(factory.createParameter("stream", factory.createTypeByFQClassName("java.io.ObjectInputStream")));
            readObjectMethod.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);

            String parameterSimpleName = "stream";

            readObjectMethod.getThrowsList().add(factory.createReferenceElementByType(factory.createTypeByFQClassName("java.io.IOException")));
            readObjectMethod.getThrowsList().add(factory.createReferenceElementByType(factory.createTypeByFQClassName("java.lang.ClassNotFoundException")));

            PsiCodeBlock methodBody = factory.createCodeBlockFromText("{return " + parameterSimpleName + "." + "defaultReadObject();}", sourceFile);

            String methodInvocationText = parameterSimpleName + "." + "defaultReadObject" + "();";

            methodBody.addBefore(factory.createStatementFromText(methodInvocationText, null), methodBody.getRBrace());
            PsiStatement assignmentStatement = createAssignmentStatementForReadObject(modifiedExtractedTypeName, parameterSimpleName);

            methodBody.addBefore(assignmentStatement, methodBody.getRBrace());

            readObjectMethod.getBody().replace(methodBody);
            sourceTypeDeclaration.addBefore(readObjectMethod, sourceTypeDeclaration.getLBrace());
        }
    }

    private PsiStatement createAssignmentStatementForReadObject(String modifiedExtractedTypeName, String parameterSimpleName) {
        return factory.createStatementFromText("this." + modifiedExtractedTypeName + " = " + "(" + extractedTypeName + ")" + parameterSimpleName + "." + "readObject();", sourceFile);
    }

    private void updateCloneInSourceClass(String modifiedExtractedTypeName) {
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        boolean methodFound = false;
        for (PsiMethod method : methodDeclarations) {
            if (isClone(method)) {
                methodFound = true;
                if (!extractedMethods.contains(method)) {
                    PsiCodeBlock methodBody = method.getBody();
                    if (methodBody != null) {
                        PsiDeclarationStatement firstStatement = isFirstStatementVariableDeclarationStatementWithSuperCloneInitializer(method);
                        if (firstStatement != null) {
                            PsiVariable fragment = (PsiVariable) firstStatement.getDeclaredElements()[0];

                            PsiStatement assignmentStatement = createAssignmentStatementForClone(fragment.getName(), modifiedExtractedTypeName);
                            methodBody.addAfter(assignmentStatement, firstStatement);
                        }
                    }
                }
            }
        }
        if (!methodFound) {
            PsiMethod cloneMethodBinding = findCloneMethod(sourceTypeDeclaration.getSuperClass());
            if (cloneMethodBinding != null) {
                PsiMethod cloneMethodDeclaration = factory.createMethod(cloneMethodBinding.getName(), cloneMethodBinding.getReturnType());

                cloneMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                PsiClassType[] thrownExceptionTypeBindings = cloneMethodBinding.getThrowsList().getReferencedTypes();
                for (PsiClassType typeBinding : thrownExceptionTypeBindings) {
                    cloneMethodDeclaration.getThrowsList().add(factory.createReferenceElementByType(typeBinding));
                }

                PsiCodeBlock body = cloneMethodDeclaration.getBody();

                PsiClassType sourceClassType = RefactoringUtility.generateTypeFromTypeBinding(sourceTypeDeclaration, factory);
                PsiExpression superCloneInvocation = (PsiExpression) factory.createStatementFromText("(" + sourceClassType + ")" + " super.clone();", sourceFile);
                PsiStatement fragment = factory.createVariableDeclarationStatement("clone", sourceClassType, superCloneInvocation);
                body.addBefore(fragment, body.getRBrace());

                PsiStatement assignmentStatement = createAssignmentStatementForClone("clone", modifiedExtractedTypeName);
                body.addBefore(assignmentStatement, body.getRBrace());

                PsiReturnStatement returnStatement = (PsiReturnStatement) factory.createStatementFromText("return clone;", sourceFile);
                body.addBefore(returnStatement, body.getRBrace());

                cloneMethodDeclaration.getBody().replace(body);
                sourceTypeDeclaration.addBefore(cloneMethodDeclaration, sourceTypeDeclaration.getRBrace());
            }
        }
    }

    private PsiStatement createAssignmentStatementForClone(String cloneVariableName, String modifiedExtractedTypeName) {
        return factory.createStatementFromText(cloneVariableName + "." + modifiedExtractedTypeName + " = " + "(" + extractedTypeName + ") this." + modifiedExtractedTypeName + ".clone();", null);
    }

    private boolean isWriteObject(PsiMethod method) {
        return method.getName().equals("writeObject") && method.getParameterList().getParameters().length == 1 &&
                method.getParameterList().getParameters()[0].getType().getCanonicalText().equals("java.io.ObjectOutputStream");
    }

    private boolean isReadObject(PsiMethod method) {
        return method.getName().equals("readObject") && method.getParameterList().getParameters().length == 1 &&
                (method.getParameterList().getParameters()[0]).getType().getCanonicalText().equals("java.io.ObjectInputStream");
    }

    private boolean isClone(PsiMethod method) {
        return method.getName().equals("clone") && method.getParameterList().getParameters().length == 0 &&
                method.getReturnType().getCanonicalText().equals("java.lang.Object");
    }

    private PsiMethod findCloneMethod(PsiClass typeBinding) {
        if (typeBinding != null && !typeBinding.getQualifiedName().equals("java.lang.Object")) {
            for (PsiMethod methodBinding : typeBinding.getMethods()) {
                if (isClone(methodBinding)) {
                    return methodBinding;
                }
            }
            return findCloneMethod(typeBinding.getSuperClass());
        }
        return null;
    }

    private PsiStatement isFirstStatementMethodInvocationExpressionStatementWithName(PsiMethod method, String methodName) {
        PsiCodeBlock methodBody = method.getBody();
        if (methodBody != null) {
            PsiStatement[] statements = methodBody.getStatements();
            if (statements.length != 0) {
                PsiStatement firstStatement = statements[0];
                if (firstStatement instanceof PsiExpressionStatement) {
                    PsiExpressionStatement expressionStatement = (PsiExpressionStatement) firstStatement;
                    PsiExpression expression = expressionStatement.getExpression();
                    if (expression instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                        if (methodInvocation.resolveMethod().getName().equals(methodName)) {
                            return firstStatement;
                        }
                    }
                }
            }
        }
        return null;
    }

    private PsiDeclarationStatement isFirstStatementVariableDeclarationStatementWithSuperCloneInitializer(PsiMethod method) {
        PsiCodeBlock methodBody = method.getBody();
        if (methodBody != null) {
            PsiStatement[] statements = methodBody.getStatements();
            if (statements.length != 0) {
                PsiStatement firstStatement = statements[0];
                if (firstStatement instanceof PsiDeclarationStatement) {
                    PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) firstStatement;
                    PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
                    if (fragments.length == 1) {
                        for (PsiElement fragment : fragments) {
                            if (fragment instanceof PsiVariable) {
                                if (((PsiVariable) fragment).getInitializer() != null) {
                                    PsiExpression expression = ((PsiVariable) fragment).getInitializer();
                                    if (expression instanceof PsiTypeCastExpression) {
                                        PsiTypeCastExpression castExpression = (PsiTypeCastExpression) expression;
                                        if (castExpression.getOperand() instanceof PsiMethodCallExpression) {
                                            PsiMethodCallExpression superMethodInvocation = (PsiMethodCallExpression) castExpression.getOperand();
                                            if (superMethodInvocation.getFirstChild() instanceof PsiSuperExpression && superMethodInvocation.resolveMethod().getName().equals("clone") &&
                                                    superMethodInvocation.getArgumentList().getExpressionCount() == 0) {
                                                return variableDeclarationStatement;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void removeFieldFragmentsInSourceClass(Set<PsiField> fieldFragments) {
        for (PsiField field : fieldFragments) {
            field.delete();
        }
    }

    private void modifyExtractedFieldAssignmentsInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> modifiedFields, Set<PsiField> accessedFields) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
        boolean rewriteAST; //TODO use
        for (PsiMethod methodDeclaration : contextMethods) {
            if (!extractedMethods.contains(methodDeclaration)) {
                PsiCodeBlock methodBody = methodDeclaration.getBody();
                if (methodBody != null) {
                    PsiStatement[] statements = methodBody.getStatements();
                    for (PsiStatement statement : statements) {
                        List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
                        for (PsiExpression expression : assignments) {
                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                            PsiExpression leftHandSide = assignment.getLExpression();
                            PsiReferenceExpression assignedVariable = null;
                            if (leftHandSide instanceof PsiReferenceExpression) {
                                assignedVariable = (PsiReferenceExpression) leftHandSide;
                            }
                            PsiExpression rightHandSide = assignment.getRExpression();
                            List<PsiExpression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
                            List<PsiExpression> arrayAccesses = expressionExtractor.getArrayAccesses(leftHandSide);
                            for (PsiField fieldFragment : fieldFragments) {
                                String originalFieldName = fieldFragment.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                                if (assignedVariable != null) {
                                    PsiElement leftHandBinding = assignedVariable.resolve();
                                    if (leftHandBinding instanceof PsiVariable) {
                                        PsiVariable assignedVariableBinding = (PsiVariable) leftHandBinding;
                                        if (assignedVariableBinding instanceof PsiField && fieldFragment.equals(assignedVariableBinding)) {
                                            if (methodDeclaration.isConstructor() && (assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.FINAL))) {
                                                if (assignment.getRExpression() instanceof PsiExpressionStatement) {
                                                    PsiExpressionStatement assignmentStatement = (PsiExpressionStatement) assignment.getRExpression();
                                                    assignmentStatement.delete();
                                                    if (constructorFinalFieldAssignmentMap.containsKey(methodDeclaration)) {
                                                        Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(methodDeclaration);
                                                        finalFieldAssignmentMap.put(fieldFragment, assignment);
                                                    } else {
                                                        Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = new LinkedHashMap<>();
                                                        finalFieldAssignmentMap.put(fieldFragment, assignment);
                                                        constructorFinalFieldAssignmentMap.put(methodDeclaration, finalFieldAssignmentMap);
                                                    }
                                                }
                                            } else {
                                                String setterMethodInvocationText;

                                                String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                                setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
                                                setterMethodInvocationText = setterMethodName;
                                                if (!assignment.getOperationSign().getTokenType().equals(JavaTokenType.EQ)) {
                                                    accessedFields.add(fieldFragment);
                                                    String infixExpressionText;

                                                    String getterMethodInvocationText = getterMethodName;

                                                    if (assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                        getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                    } else {
                                                        getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                    }

                                                    getterMethodInvocationText += "()";

                                                    infixExpressionText = getterMethodInvocationText + assignment.getOperationSign().getText() + assignment.getRExpression();
                                                    setterMethodInvocationText += "(" + infixExpressionText + ")";
                                                } else {
                                                    setterMethodInvocationText += "(" + assignment.getRExpression() + ")";
                                                }

                                                if (leftHandSide instanceof PsiQualifiedReference) {
                                                    String qualifier = ((PsiQualifiedReference) leftHandSide).getQualifier().getText();
                                                    setterMethodInvocationText = modifiedExtractedTypeName + "." + qualifier + "." + setterMethodInvocationText;
                                                } else if ((assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                                                    setterMethodInvocationText = extractedTypeName + "." + setterMethodInvocationText;
                                                } else {
                                                    setterMethodInvocationText = modifiedExtractedTypeName + "." + setterMethodInvocationText;
                                                }
                                                assignment.replace(factory.createExpressionFromText(setterMethodInvocationText, null));
                                            }
                                            rewriteAST = true;
                                            modifiedFields.add(fieldFragment);
                                        }
                                    }
                                }
                                for (PsiExpression expression2 : arrayAccesses) {
                                    PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression2;
                                    PsiExpression arrayExpression = arrayAccess.getArrayExpression();
                                    PsiReferenceExpression arrayVariable = (PsiReferenceExpression) arrayExpression;
                                    if (arrayVariable != null) {
                                        PsiElement arrayBinding = arrayVariable.resolve();
                                        if (arrayBinding instanceof PsiVariable) {
                                            PsiVariable arrayVariableBinding = (PsiVariable) arrayBinding;
                                            if (arrayVariableBinding instanceof PsiField && fieldFragment.equals(arrayVariableBinding)) {
                                                PsiMethodCallExpression getterMethodInvocation;
                                                String getterMethodInvocationText = getterMethodName;
                                                if (arrayVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                    getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                } else {
                                                    getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                }
                                                getterMethodInvocationText += "()";
                                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);
                                                arrayExpression.replace(getterMethodInvocation);

                                                accessedFields.add(fieldFragment);
                                            }
                                        }
                                    }
                                }
                                for (PsiExpression expression2 : accessedVariables) {
                                    PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression2;
                                    PsiElement rightHandBinding = accessedVariable.resolve();
                                    if (rightHandBinding instanceof PsiVariable) {
                                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                                        if (accessedVariableBinding instanceof PsiField && fieldFragment.equals(accessedVariableBinding)) {
                                            PsiMethodCallExpression getterMethodInvocation;
                                            String getterMethodInvocationText = getterMethodName;
                                            if (accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }
                                            getterMethodInvocationText += "()";
                                            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);
                                            accessedVariable.replace(getterMethodInvocation);
                                            accessedFields.add(fieldFragment);
                                        }
                                    }
                                }
                            }
                        }
                        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
                        for (PsiExpression expression : postfixExpressions) {
                            PsiPostfixExpression postfix = (PsiPostfixExpression) expression;
                            PsiExpression operand = postfix.getOperand();
                            PsiElement assignedVariable = ((PsiReferenceExpression) operand).resolve(); //TODO maybe incorrect
                            List<PsiExpression> arrayAccesses = expressionExtractor.getArrayAccesses(operand);
                            for (PsiField fieldFragment : fieldFragments) {
                                String originalFieldName = fieldFragment.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                                if (assignedVariable != null) {
                                    PsiVariable operandBinding = (PsiVariable) assignedVariable;
                                    if (operandBinding instanceof PsiVariable) {
                                        if (operandBinding instanceof PsiField && fieldFragment.equals(operandBinding)) {
                                            PsiMethodCallExpression setterMethodInvocation;
                                            String setterMethodInvocationText;
                                            String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                            setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
                                            setterMethodInvocationText = setterMethodName;

                                            accessedFields.add(fieldFragment);
                                            String infixExpressionText = "";

                                            String getterMethodInvocationText = getterMethodName;

                                            if (operandBinding.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }
                                            getterMethodInvocationText += "()";

                                            infixExpressionText += getterMethodInvocationText;
                                            if (postfix.getOperationSign().getText().equals("++")) {
                                                infixExpressionText += " + ";
                                            } else if (postfix.getOperationSign().getText().equals("--")) {
                                                infixExpressionText += " - ";
                                            }
                                            infixExpressionText += "1";

                                            setterMethodInvocationText += '(';
                                            setterMethodInvocationText += infixExpressionText;
                                            if (operand instanceof PsiQualifiedExpression) {
                                                String qualifier = ((PsiQualifiedExpression) operand).getQualifier().getQualifiedName();
                                                setterMethodInvocationText = modifiedExtractedTypeName + "." + qualifier + "." + setterMethodInvocationText;
                                            } else if (operandBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                setterMethodInvocationText = extractedTypeName + "." + setterMethodInvocationText;
                                            } else {
                                                setterMethodInvocationText = modifiedExtractedTypeName + "." + setterMethodInvocationText;
                                            }
                                            setterMethodInvocationText += ")";

                                            setterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(setterMethodInvocationText, null);
                                            postfix.replace(setterMethodInvocation);
                                            rewriteAST = true;
                                            modifiedFields.add(fieldFragment);
                                        }
                                    }
                                }
                                for (PsiExpression expression2 : arrayAccesses) {
                                    PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression2;
                                    PsiExpression arrayExpression = arrayAccess.getArrayExpression();
                                    PsiVariable arrayVariable = (PsiVariable) ((PsiReferenceExpression) arrayAccess).resolve(); //TODO maybe wrong
                                    if (arrayVariable != null) {
                                        if (arrayVariable instanceof PsiVariable) {
                                            if (arrayVariable instanceof PsiField && fieldFragment.equals(arrayVariable)) {
                                                PsiMethodCallExpression getterMethodInvocation;
                                                String getterMethodInvocationText;
                                                getterMethodInvocationText = getterMethodName;
                                                if (arrayVariable.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                                                    getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                } else {
                                                    getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                }
                                                getterMethodInvocationText += "()";
                                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);
                                                arrayExpression.replace(getterMethodInvocation);
                                                rewriteAST = true;
                                                accessedFields.add(fieldFragment);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        //TODO I DO actually need rewrite ast thing FOR preview
                    }
                }
            }
        }
    }

    private void modifyExtractedFieldAccessesInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> accessedFields) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
        for (PsiMethod methodDeclaration : contextMethods) {
            if (!extractedMethods.contains(methodDeclaration)) {
                PsiCodeBlock methodBody = methodDeclaration.getBody();
                if (methodBody != null) {
                    PsiStatement[] statements = methodBody.getStatements();
                    for (PsiStatement statement : statements) {
                        boolean rewriteAST = false;
                        List<PsiExpression> accessedVariables = expressionExtractor.getVariableInstructions(statement);
                        List<PsiExpression> arrayAccesses = expressionExtractor.getArrayAccesses(statement);
                        for (PsiField fieldFragment : fieldFragments) {
                            String originalFieldName = fieldFragment.getName();
                            String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                            String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                            getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                            for (PsiExpression expression : accessedVariables) {
                                PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression;
                                PsiElement binding = accessedVariable.resolve();
                                if (binding instanceof PsiVariable) {
                                    PsiVariable accessedVariableBinding = (PsiVariable) binding;
                                    if (accessedVariableBinding instanceof PsiField && fieldFragment.equals(accessedVariableBinding)) {
                                        if (!isAssignmentChild(expression)) {
                                            PsiMethodCallExpression getterMethodInvocation;
                                            String getterMethodInvocationText = getterMethodName;
                                            if (accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText += modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }
                                            getterMethodInvocationText += "()";
                                            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);
                                            accessedVariable.replace(getterMethodInvocation);
                                            rewriteAST = true;
                                            accessedFields.add(fieldFragment);
                                        }
                                    }
                                }
                            }
                            for (PsiExpression expression : arrayAccesses) {
                                PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression;
                                PsiExpression arrayExpression = arrayAccess.getArrayExpression();
                                PsiElement arrayVariable = ((PsiReferenceExpression) arrayExpression).resolve();
                                if (arrayVariable != null) {
                                    if (arrayVariable instanceof PsiVariable) {
                                        PsiVariable arrayVariableBinding = (PsiVariable) arrayVariable;
                                        if (arrayVariableBinding instanceof PsiField && fieldFragment.equals(arrayVariableBinding)) {
                                            if (!isAssignmentChild(expression)) {
                                                PsiMethodCallExpression getterMethodInvocation;
                                                String getterMethodInvocationText = getterMethodName;
                                                if (arrayVariableBinding.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                                                    getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                } else {
                                                    getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                }
                                                getterMethodInvocationText += "()";
                                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);
                                                arrayVariable.replace(getterMethodInvocation);
                                                rewriteAST = true;
                                                accessedFields.add(fieldFragment);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        /*
                        TODO

                        if (rewriteAST) {
                            if (!statementRewriteMap.containsKey(statement))
                                statementRewriteMap.put(statement, sourceRewriter);
							/*try {
								TextEdit sourceEdit = sourceRewriter.rewriteAST();
								ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
								CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
								change.getEdit().addChild(sourceEdit);
								change.addTextEditGroup(new TextEditGroup("Replace field access with invocation of getter method", new TextEdit[] {sourceEdit}));
							} catch (JavaModelException e) {
								e.printStackTrace();
							}
						}
                         */
                    }
                }
            }
        }
    }

    private Set<PsiMethod> getAllMethodDeclarationsInSourceClass() {
        Set<PsiMethod> contextMethods = new LinkedHashSet<>();
        for (PsiField fieldDeclaration : sourceTypeDeclaration.getFields()) {
            contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
        }
        List<PsiMethod> methodDeclarationList = Arrays.asList(sourceTypeDeclaration.getMethods());
        contextMethods.addAll(methodDeclarationList);
		/*for(MethodDeclaration methodDeclaration : methodDeclarationList) {
			contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(methodDeclaration));
		}*/
        //get methods of inner classes
        PsiClass[] types = sourceTypeDeclaration.getInnerClasses();
        for (PsiClass type : types) {
            for (PsiField fieldDeclaration : type.getFields()) {
                contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
            }
            List<PsiMethod> innerMethodDeclarationList = Arrays.asList(type.getMethods());
            contextMethods.addAll(innerMethodDeclarationList);
			/*for(MethodDeclaration methodDeclaration : innerMethodDeclarationList) {
				contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(methodDeclaration));
			}*/
        }
        return contextMethods;
    }

    private Set<PsiMethod> getMethodDeclarationsWithinAnonymousClassDeclarations(PsiMethod methodDeclaration) {
        Set<PsiMethod> methods = new LinkedHashSet<>();
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(methodDeclaration.getBody().getStatements());
        for (PsiExpression expression : classInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
            PsiClass anonymousClassDeclaration = classInstanceCreation.getAnonymousClass();
            if (anonymousClassDeclaration != null) {
                methods.addAll(Arrays.asList(anonymousClassDeclaration.getMethods()));
            }
        }

        return methods;
    }

    private Set<PsiMethod> getMethodDeclarationsWithinAnonymousClassDeclarations(PsiField fieldDeclaration) {
        Set<PsiMethod> methods = new LinkedHashSet<>();
        PsiExpression expression = fieldDeclaration.getInitializer();
        if (expression instanceof PsiNewExpression) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
            PsiClass anonymousClassDeclaration = classInstanceCreation.getAnonymousClass();
            if (anonymousClassDeclaration != null) {
                methods.addAll(Arrays.asList(anonymousClassDeclaration.getMethods()));
            }
        }

        return methods;
    }

    private boolean isAssignmentChild(PsiElement node) {
        if (node == null) {
            return false;
        }

        if (node instanceof PsiAssignmentExpression)
            return true;
        else if (node instanceof PsiPrefixExpression) {
            PsiPrefixExpression prefixExpression = (PsiPrefixExpression) node;
            if (prefixExpression.getOperationSign().getText().equals("++") ||
                    prefixExpression.getOperationSign().getText().equals("--"))
                return true;
            else
                return isAssignmentChild(node.getParent());
        } else if (node instanceof PsiPostfixExpression) {
            PsiPostfixExpression postfixExpression = (PsiPostfixExpression) node;
            if (postfixExpression.getOperationSign().getText().equals("++") ||
                    postfixExpression.getOperationSign().getText().equals("--"))
                return true;
            else
                return isAssignmentChild(node.getParent());
        } else if (node instanceof PsiStatement)
            return false;
        else
            return isAssignmentChild(node.getParent());
    }

    public String getName() {
        return "Extract Class";
    }

    /*
    TODO

    public RefactoringStatus checkInitialConditions(ProgressMonitor pm) {
        RefactoringStatus status = new RefactoringStatus();
        try {
            pm.beginTask("Checking preconditions...", 1);
        } finally {
            pm.done();
        }
        return status;
    }

     */

    /*
    TODO
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        final RefactoringStatus status = new RefactoringStatus();
        try {
            pm.beginTask("Checking preconditions...", 2);
            apply();
        } finally {
            pm.done();
        }
        return status;
    }

     */

    /*
    TODO
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        try {
            pm.beginTask("Creating change...", 1);
            final Collection<Change> changes = new ArrayList<Change>();
            changes.addAll(compilationUnitChanges.values());
            changes.addAll(createCompilationUnitChanges.values());
            CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
                @Override
                public ChangeDescriptor getDescriptor() {
                    ICompilationUnit sourceICompilationUnit = (ICompilationUnit) sourceCompilationUnit.getJavaElement();
                    String project = sourceICompilationUnit.getJavaProject().getElementName();
                    String description = MessageFormat.format("Extracting class from ''{0}''", new Object[]{sourceTypeDeclaration.getName().getIdentifier()});
                    String comment = null;
                    return new RefactoringChangeDescriptor(new ExtractClassRefactoringDescriptor(project, description, comment,
                            sourceCompilationUnit, sourceTypeDeclaration, sourceFile, extractedFieldFragments, extractedMethods, delegateMethods, extractedTypeName));
                }
            };
            return change;
        } finally {
            pm.done();
        }
    }

     */

    public PsiFile getSourceFile() {
        return sourceFile;
    }

    public Project getProject() {
        return project;
    }
}