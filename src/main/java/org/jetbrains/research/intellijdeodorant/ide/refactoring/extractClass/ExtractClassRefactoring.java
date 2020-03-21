package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass;

import com.intellij.lang.ASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FileContentUtil;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;
import org.jetbrains.research.intellijdeodorant.core.ast.util.StatementExtractor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringUtility;
import org.jetbrains.research.intellijdeodorant.utils.math.AdjacencyList;
import org.jetbrains.research.intellijdeodorant.utils.math.Edge;
import org.jetbrains.research.intellijdeodorant.utils.math.Node;
import org.jetbrains.research.intellijdeodorant.utils.math.TarjanAlgorithm;

import java.util.*;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private final PsiJavaFile sourceFile;
    private final PsiClass sourceTypeDeclaration;
    private final Map<PsiMethod, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
    private final Map<PsiMethod, Set<PsiParameter>> additionalParametersAddedToExtractedMethods;
    private final Set<PsiMethod> sourceMethodBindingsChangedWithPublicModifier;
    private final Set<PsiField> sourceFieldBindingsWithCreatedSetterMethod;
    private final Set<PsiField> sourceFieldBindingsWithCreatedGetterMethod;
    private final Map<PsiMethod, Set<PsiMethodCallExpression>> oldMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> newMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, PsiMethod> oldToNewExtractedMethodDeclarationMap;
    private final Set<PsiField> extractedFieldFragments;
    private final Set<PsiMethod> extractedMethods;
    private final Set<PsiMethod> delegateMethods;
    private final String defaultExtractedTypeName;
    private String extractedTypeName;
    //this map holds for each constructor the assignment statements that initialize final extracted fields
    private final Map<PsiMethod, Map<PsiField, PsiAssignmentExpression>> constructorFinalFieldAssignmentMap;
    //this map holds the parameters that should be passed in each constructor of the extracted class
    private final Map<PsiMethod, Set<PsiParameter>> extractedClassConstructorParameterMap;
    private final Set<PsiField> extractedFieldsWithThisExpressionInTheirInitializer;

    private final PsiElementFactory factory;
    private final PsiFileFactory fileFactory;
    private final Project project;
    private final PsiManager psiManager;
    private final JavaCodeStyleManager javaCodeStyleManager;
    private final CodeStyleManager codeStyleManager;

    /*
    We create each of the new methods inside 'sandbox' file - a full copy of the source file, in order to correctly
    resolve all references, which is vital for IDEA API.

    Then we add copies of the sandboxed methods to the extracted class.
     */
    private final Map<PsiMethod, PsiMethod> sourceToSandboxMethodMap;
    private final Map<PsiField, PsiField> sandboxToSourceFieldMap;
    private final Map<PsiMethod, PsiMethod> sandboxToExtractedMethodMap;

    public ExtractClassRefactoring(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration,
                                   Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods, Set<PsiMethod> delegateMethods, String defaultExtractedTypeName) {
        this.sourceFile = sourceFile;
        this.sourceTypeDeclaration = sourceTypeDeclaration;
        this.additionalArgumentsAddedToExtractedMethods = new LinkedHashMap<>();
        this.additionalParametersAddedToExtractedMethods = new LinkedHashMap<>();
        this.sourceMethodBindingsChangedWithPublicModifier = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedSetterMethod = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedGetterMethod = new LinkedHashSet<>();
        this.oldMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.newMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.oldToNewExtractedMethodDeclarationMap = new LinkedHashMap<>();
        this.extractedFieldFragments = extractedFieldFragments;
        this.extractedMethods = extractedMethods;
        this.delegateMethods = delegateMethods;
        this.defaultExtractedTypeName = defaultExtractedTypeName;
        this.extractedTypeName = defaultExtractedTypeName;
        this.constructorFinalFieldAssignmentMap = new LinkedHashMap<>();
        this.extractedClassConstructorParameterMap = new LinkedHashMap<>();
        this.extractedFieldsWithThisExpressionInTheirInitializer = new LinkedHashSet<>();
        for (PsiMethod extractedMethod : extractedMethods) {
            additionalArgumentsAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
            additionalParametersAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
        }

        this.project = sourceFile.getProject();
        this.factory = PsiElementFactory.getInstance(project);
        this.fileFactory = PsiFileFactory.getInstance(project);
        this.psiManager = PsiManager.getInstance(project);
        this.codeStyleManager = CodeStyleManager.getInstance(project);
        this.javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        this.sourceToSandboxMethodMap = new LinkedHashMap<>();
        this.sandboxToSourceFieldMap = new LinkedHashMap<>();
        this.sandboxToExtractedMethodMap = new LinkedHashMap<>();
    }

    public String getExtractedTypeName() {
        return extractedTypeName;
    }

    public String getDefaultExtractedTypeName() {
        return defaultExtractedTypeName;
    }

    public void setExtractedTypeName(String targetTypeName) {
        this.extractedTypeName = targetTypeName;
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

        Set<PsiField> modifiedFieldsInNonExtractedMethods = new LinkedHashSet<>();
        Set<PsiField> accessedFieldsInNonExtractedMethods = new LinkedHashSet<>();

        /*
        We reorganized the order of refactoring's changes compared to the original plugin so that
        the transitional code during the refactoring will always be compilable (which is vital for IDEA api)

        As a part of it, we have to create extracted class before any changes to the original class, so at first, we should
        collect modified and accessed fields in source class, and only then change them in
        `modifyExtractedFieldAssignmentsInSourceClass()` and `modifyExtractedFieldAccessesInSourceClass()`

        Same for `createExtractedTypeFieldReferenceInSourceClass()`.
        It both collects required parameters for the extracted class constructor and creates a corresponding field in the source class.
        We have to collect required parameters to create extracted class, but we cannot create the field in the source class before creating the extracted class.
         */
        collectExtractedFieldAssignmentsInSourceClass(extractedFieldFragments, modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        collectExtractedFieldAccessesInSourceClass(extractedFieldFragments, accessedFieldsInNonExtractedMethods);
        createExtractedTypeFieldReferenceInSourceClass(ModifyType.EXTRACTED);

        PsiJavaFile extractedClassFile = createExtractedClass(modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);

        createExtractedTypeFieldReferenceInSourceClass(ModifyType.SOURCE);

        modifyExtractedFieldAssignmentsInSourceClass(extractedFieldFragments, modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedFieldAccessesInSourceClass(extractedFieldFragments, accessedFieldsInNonExtractedMethods);
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

        removeFieldFragmentsInSourceClass(extractedFieldFragments);

        commitResults(extractedClassFile);
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

        sourceMethod.getBody().replace(factory.createCodeBlock());

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
        /*
        The original plugin makes here a workaround the bug in Eclipse. IDEA does not have the same bug.
         */
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
                                PsiMethod method = methodInvocation.resolveMethod();
                                if (extractedMethods.contains(method)) {
                                    PsiExpression qualifier = methodInvocation.getMethodExpression().getQualifierExpression();
                                    if (qualifier == null || qualifier instanceof PsiThisExpression) {
                                        if (qualifier != null) {
                                            qualifier.delete();
                                        }

                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(method);
                                        StringBuilder arguments = new StringBuilder();

                                        boolean isFirst = true;
                                        for (PsiExpression argument : methodInvocation.getArgumentList().getExpressions()) {
                                            if (!isFirst) {
                                                arguments.append(", ");
                                            }

                                            arguments.append(argument.getText());
                                            isFirst = false;
                                        }

                                        if (additionalArgumentsAddedToMovedMethod != null) {
                                            for (PlainVariable argument : additionalArgumentsAddedToMovedMethod) {
                                                if (!isFirst) {
                                                    arguments.append(',');
                                                }

                                                if (isThisVariable(argument)) {
                                                    if (isParentAnonymousClassDeclaration(methodInvocation.getNode())) {
                                                        arguments.append(sourceTypeDeclaration.getName()).append(".this");
                                                    } else {
                                                        arguments.append("this");
                                                    }
                                                } else {
                                                    if (argument.isField()) {
                                                        arguments.append("this.").append(argument.getName());
                                                    } else {
                                                        arguments.append(argument.getName());
                                                    }
                                                }
                                            }
                                        }

                                        String modifiedMethodCallExpressionText = method.getName() + "(" + arguments.toString() + ")";
                                        if (isStatic(method)) {
                                            modifiedMethodCallExpressionText = extractedTypeName + "." + modifiedMethodCallExpressionText;
                                        } else {
                                            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
                                            modifiedMethodCallExpressionText = modifiedExtractedTypeName + "." + modifiedMethodCallExpressionText;
                                        }

                                        expression.replace(factory.createExpressionFromText(modifiedMethodCallExpressionText, expression));
                                    } else {
                                        delegateMethods.add(method);
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
            if (!(fieldFragment.getModifierList() != null && fieldFragment.getModifierList().hasExplicitModifier(PsiModifier.TRANSIENT))) {
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

    private void mapSourceMembersAndSandboxCopies(PsiElement firstElement, PsiElement secondElement) {
        if (firstElement instanceof PsiMethod && secondElement instanceof PsiMethod) {
            sourceToSandboxMethodMap.put((PsiMethod) firstElement, (PsiMethod) secondElement);
        }

        if (firstElement instanceof PsiField && secondElement instanceof PsiField) {
            sandboxToSourceFieldMap.put((PsiField) secondElement, (PsiField) firstElement);
        }

        for (int i = 0; i < firstElement.getChildren().length; i++) {
            mapSourceMembersAndSandboxCopies(firstElement.getChildren()[i], secondElement.getChildren()[i]);
        }
    }

    private PsiJavaFile createExtractedClass(Set<PsiField> modifiedFieldsInNonExtractedMethods, Set<PsiField> accessedFieldsInNonExtractedMethods) {
        PsiJavaFile sandboxFile = (PsiJavaFile) sourceFile.copy();
        mapSourceMembersAndSandboxCopies(sourceFile, sandboxFile);

        String extractedClassFileName = extractedTypeName + ".java";
        PsiJavaFile extractedClassFile = (PsiJavaFile) fileFactory.createFileFromText(JavaLanguage.INSTANCE, "");
        extractedClassFile = (PsiJavaFile) extractedClassFile.setName(extractedClassFileName);

        PsiClass extractedClass = factory.createClass(extractedTypeName);
        extractedClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        //Just all possible imports and hope that IDEA will optimize it correctly.
        extractedClassFile.setPackageName(sourceFile.getPackageName());
        extractedClassFile.getImportList().add(factory.createImportStatementOnDemand(sourceFile.getPackageName() + "." + PsiTreeUtil.getChildOfType(sourceFile, PsiClass.class).getName()));

        for (PsiImportStatement importStatement : sourceFile.getImportList().getImportStatements()) {
            extractedClassFile.getImportList().add(importStatement);
        }

        for (PsiImportStaticStatement importStaticStatement : sourceFile.getImportList().getImportStaticStatements()) {
            extractedClassFile.getImportList().add(importStaticStatement);
        }

        PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
        if (serializableTypeBinding != null) {
            extractedClass.getImplementsList().add(factory.createClassReferenceElement(serializableTypeBinding));
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
                    extractedFieldDeclaration = factory.createFieldFromText(fieldFragment.getText(), null);
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

            if (extractedClassConstructorParameters != null) {
                for (PsiParameter variableDeclaration : extractedClassConstructorParameters) {
                    PsiType parameterType = variableDeclaration.getType();
                    extractedClassConstructor.getParameterList().add(factory.createParameter(variableDeclaration.getName(), parameterType));
                }
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
            createExtractedMethodDeclaration(method, extractedClass);
        }

        remapNewMethodInvocationsFromSandboxMethodToExtracted();

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
                PsiExpression qualifier = oldMethodInvocation.getMethodExpression().getQualifierExpression();
                if (qualifier == null || qualifier instanceof PsiThisExpression) {
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
                PsiExpression qualifier = oldMethodInvocation.getMethodExpression().getQualifierExpression();
                if (qualifier == null || qualifier instanceof PsiThisExpression) {
                    //invocation without expression
                    if (oldMethod.equals(oldMethodInvocation.resolveMethod())) {
                        //recursive invocation
                        for (PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            PsiMethodCallExpression newMethodInvocation = newMethodInvocations.get(i);

                            removeRedundantThisQualifierFromExpression(newMethodInvocation);

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
                extractedClass.getImplementsList().add(factory.createReferenceFromText("java.lang.Cloneable", null));
            }
        }

        extractedClassFile.add(extractedClass);

        extractedClassFile = (PsiJavaFile) sourceFile.getContainingDirectory().add(extractedClassFile);
        FileContentUtil.reparseFiles(extractedClassFile.getContainingDirectory().getVirtualFile());

        return extractedClassFile;
    }

    private void remapNewMethodInvocationsFromSandboxMethodToExtracted() {
        Map<PsiMethod, Set<PsiMethodCallExpression>> newMap = new LinkedHashMap<>();

        for (PsiMethod sandboxMethod : newMethodInvocationsWithinExtractedMethods.keySet()) {
            PsiMethod extractedMethod = sandboxToExtractedMethodMap.get(sandboxMethod);

            Set<PsiMethodCallExpression> extractedInvocations = new LinkedHashSet<>();
            mapMethodInvocations(extractedInvocations, sandboxMethod, extractedMethod);
            newMap.put(extractedMethod, extractedInvocations);
        }

        newMethodInvocationsWithinExtractedMethods = newMap;

        Map<PsiMethod, PsiMethod> oldMethodToNew = new LinkedHashMap<>();
        for (PsiMethod source : oldToNewExtractedMethodDeclarationMap.keySet()) {
            PsiMethod sandbox = oldToNewExtractedMethodDeclarationMap.get(source);
            PsiMethod extracted = sandboxToExtractedMethodMap.get(sandbox);

            oldMethodToNew.put(source, extracted);
        }

        oldToNewExtractedMethodDeclarationMap = oldMethodToNew;
    }

    private void mapMethodInvocations(Set<PsiMethodCallExpression> set, PsiElement firstElement, PsiElement secondElement) {
        if (firstElement instanceof PsiMethodCallExpression && secondElement instanceof PsiMethodCallExpression) {
            set.add((PsiMethodCallExpression) secondElement);
        }

        for (int i = 0; i < firstElement.getChildren().length && i < secondElement.getChildren().length; i++) {
            mapMethodInvocations(set, firstElement.getChildren()[i], secondElement.getChildren()[i]);
        }
    }

    private void commitResults(PsiJavaFile extractedClassFile) {
        commit(extractedClassFile);
        commit(sourceFile);
    }

    private void commit(PsiJavaFile file) {
        file = (PsiJavaFile) codeStyleManager.reformat(file);

        javaCodeStyleManager.optimizeImports(file);
        javaCodeStyleManager.shortenClassReferences(file);

        Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
        PsiDocumentManager.getInstance(project).commitDocument(document);
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
            PsiElement returnStatement = factory.createStatementFromText("return (" + extractedTypeName + ") " + superCloneMethodInvocation.getText(), sourceTypeDeclaration);

            StringBuilder returnStatementText = new StringBuilder();

            if (cloneMethodBinding.getThrowsList().getReferenceElements().length != 0) {
                returnStatementText.append("try {\n").append(returnStatement.getText()).append(";\n").append("} catch (CloneNotSupportedException e) {\n").append("throw new InternalError(\"Failed to implement Cloneable interface\");\n").append("}");
                returnStatement = factory.createStatementFromText(returnStatementText.toString(), null);
            }

            cloneMethodDeclaration.getBody().addBefore(returnStatement, cloneMethodDeclaration.getBody().getRBrace());
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

    private void createExtractedMethodDeclaration(PsiMethod extractedMethod, PsiClass extractedClass) {
        /*
        All new methods firstly updates in the 'sandbox' - full copy of the source file, in order to correctly resolve
        all references (they cannot change in the source file directly because this file may hold a corresponding delegate methods)
         */

        PsiMethod newMethodDeclaration = sourceToSandboxMethodMap.get(extractedMethod);

        newMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        for (PsiAnnotation annotation : extractedMethod.getAnnotations()) {
            if (annotation instanceof MarkerAnnotation && annotation.getQualifiedName() != null && annotation.getQualifiedName().equals("Test")) {
                annotation.delete();
            }
        }

        modifySourceMemberAccessesInTargetClass(extractedMethod, newMethodDeclaration);
        modifySourceStaticFieldInstructionsInTargetClass(newMethodDeclaration);

        PsiMethod realExtractedMethod = (PsiMethod) extractedClass.addBefore(newMethodDeclaration, extractedClass.getRBrace());
        sandboxToExtractedMethodMap.put(newMethodDeclaration, realExtractedMethod);
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

    private void createSetterMethodInSourceClass(PsiField variableBinding) {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            if (fieldDeclaration.equals(variableBinding) || fieldDeclaration.equals(sandboxToSourceFieldMap.get(variableBinding))) {
                String methodName = fieldDeclaration.getName();
                methodName = SETTER_PREFIX + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = factory.createMethodFromText("public void " + methodName + "(" + fieldDeclaration.getType().getPresentableText() + " " + fieldDeclaration.getName() + ") {\n"
                        + "this." + fieldDeclaration.getName() + " = " + fieldDeclaration.getName() + ";\n" + "}", sourceTypeDeclaration);

                sourceTypeDeclaration.addBefore(psiMethod, sourceTypeDeclaration.getRBrace());
            }
        }
    }

    private void createGetterMethodInSourceClass(PsiField variableBinding) {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            if (fieldDeclaration.equals(variableBinding) || fieldDeclaration.equals(sandboxToSourceFieldMap.get(variableBinding))) {
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
        /*
        The original plugin finds expressions in the `sourceMethod`, calls it `old`, and for each creates corresponding `new` expression
         and puts it to the `newMethodDeclaration`.

         This is Eclipse-specific. In IDEA, we can directly work with the extracted method, except for information that does concern updating the source class.
         */

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        oldToNewExtractedMethodDeclarationMap.put(sourceMethod, newMethodDeclaration);

        List<PsiExpression> newAssignments = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> newAssignments.addAll(expressionExtractor.getAssignments(statement)));

        PsiParameter sourceClassParameter = null;
        boolean sourceClassParameterShouldBeFinal = false;
        Map<PlainVariable, PsiParameter> fieldParameterMap = new LinkedHashMap<>();
        Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<>();
        String sourceTypeName = sourceTypeDeclaration.getName();
        String modifiedSourceTypeName = sourceTypeName.substring(0, 1).toLowerCase() + sourceTypeName.substring(1);

        for (PsiExpression expression : newAssignments) {
            PsiAssignmentExpression newAssignment = (PsiAssignmentExpression) expression;
            PsiExpression newLeftHandSide = newAssignment.getLExpression();

            PsiVariable newAssignedVariable = null;

            PsiExpression qualifier = null;
            if (newLeftHandSide instanceof PsiReferenceExpression) {
                newAssignedVariable = (PsiVariable) ((PsiReferenceExpression) newLeftHandSide).resolve();
                qualifier = ((PsiReferenceExpression) newLeftHandSide).getQualifierExpression();
            }

            PsiElement newRightHandSide = newAssignment.getRExpression();
            if (newAssignedVariable != null) {
                if (newAssignedVariable instanceof PsiField && !newAssignedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    PsiField newAssignedField = (PsiField) newAssignedVariable;
                    if (declaredInSourceTypeDeclarationOrSuperclass(newAssignedField)) {
                        if (!variableBindingCorrespondsToExtractedField(newAssignedField)) {
                            PsiMethod setterMethodBinding = findSetterMethodInSourceClass(newAssignedField);
                            Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                            Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                            if (isParentAnonymousClassDeclaration(newAssignment.getNode()))
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
                                if (!sourceFieldBindingsWithCreatedSetterMethod.contains(newAssignedField)) {
                                    createSetterMethodInSourceClass(newAssignedField);
                                    sourceFieldBindingsWithCreatedSetterMethod.add(newAssignedField);
                                }
                                String originalFieldName = newAssignedField.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                setterMethodInvocationName = setterMethodName;
                            }

                            String setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationName + "(";

                            if (qualifier != null) {
                                setterMethodInvocationText = qualifier.getText() + setterMethodInvocationText;
                            }

                            if (!newAssignment.getOperationSign().getText().equals("=")) {
                                PsiMethod getterMethodBinding = findGetterMethodInSourceClass(newAssignedField);
                                PsiMethodCallExpression getterMethodInvocation;
                                String getterMethodName;
                                if (getterMethodBinding != null) {
                                    getterMethodName = getterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(newAssignedField)) {
                                        createGetterMethodInSourceClass(newAssignedField);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(newAssignedField);
                                    }
                                    String originalFieldName = newAssignedField.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                }

                                String getterMethodInvocationName = modifiedSourceTypeName + "." + getterMethodName;

                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationName + "()", null);

                                String operationSign = removeEQFromOperationSign(newAssignment.getOperationSign());

                                PsiExpression infixExpression = factory.createExpressionFromText(getterMethodInvocation.getText() + " " + operationSign + " " + newAssignment.getRExpression().getText(), sourceMethod);

                                setterMethodInvocationText += infixExpression.getText() + ")";
                            } else {
                                setterMethodInvocationText += newAssignment.getRExpression().getText() + ")";
                            }

                            setterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(setterMethodInvocationText, null);
                            setterMethodInvocation = (PsiMethodCallExpression) newAssignment.replace(setterMethodInvocation);
                            newRightHandSide = setterMethodInvocation.getArgumentList();
                        }
                    }
                }
            } else {
                //if an assigned field is not found in left hand side, then replace all accessed fields in left hand side
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newLeftHandSide);
                for (PsiExpression expression2 : newAccessedVariables) {
                    PsiReferenceExpression newAccessedVariableReferenceExpression = (PsiReferenceExpression) expression2;
                    PsiVariable newAccessedVariable = (PsiVariable) newAccessedVariableReferenceExpression.resolve();

                    if (newAccessedVariable != null) {
                        if (newAccessedVariable instanceof PsiField && !newAccessedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                            PsiField oldAccessedField = (PsiField) newAccessedVariable;
                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) newAccessedVariable)) {
                                if (!variableBindingCorrespondsToExtractedField(newAccessedVariable)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(oldAccessedField)) {
                                        if (isParentAnonymousClassDeclaration(newAssignment.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariableReferenceExpression);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(newAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(newAccessedVariable), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
                                                newAccessedVariableReferenceExpression);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            List<PsiExpression> newAccessedVariables;

            if (newRightHandSide instanceof PsiExpressionList) {
                newAccessedVariables = expressionExtractor.getVariableInstructions(((PsiExpressionList) newRightHandSide).getExpressions());
            } else {
                newAccessedVariables = expressionExtractor.getVariableInstructions((PsiExpression) newRightHandSide);
            }

            for (PsiExpression expression2 : newAccessedVariables) {
                PsiReferenceExpression newAccessedVariableReferenceExpression = (PsiReferenceExpression) expression2;

                removeRedundantThisQualifierFromExpression(newAccessedVariableReferenceExpression);

                PsiVariable newAccessedVariable = (PsiVariable) newAccessedVariableReferenceExpression.resolve();
                if (newAccessedVariable instanceof PsiVariable) {
                    if (newAccessedVariable instanceof PsiField && (!newAccessedVariable.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField newAccessedField = (PsiField) newAccessedVariable;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) newAccessedVariable)) {
                            if (!variableBindingCorrespondsToExtractedField(newAccessedVariable)) {
                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(newAccessedField)) {
                                    if (isParentAnonymousClassDeclaration(newAssignment.getNode()))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            newAccessedVariableReferenceExpression);
                                } else {
                                    if (isParentAnonymousClassDeclaration(newAccessedVariable.getNode()))
                                        fieldParameterFinalMap.put(new PlainVariable(newAccessedVariable), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            fieldParameterMap,
                                            newAccessedVariableReferenceExpression);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<PsiExpression> newPostfixExpressions = expressionExtractor.getPostfixExpressions(newMethodDeclaration.getBody().getStatements());
        for (PsiExpression expression : newPostfixExpressions) {
            PsiPostfixExpression newPostfixExpression = (PsiPostfixExpression) expression;
            PsiExpression newOperand = newPostfixExpression.getOperand();

            PsiReferenceExpression newAssignedVariable = null;
            PsiExpression setterQualifier = null;
            if (newOperand instanceof PsiReferenceExpression) {
                PsiReferenceExpression newOperandReference = (PsiReferenceExpression) newOperand;
                PsiElement newOperandResolved = newOperandReference.resolve();
                if (newOperandResolved instanceof PsiField || newOperand instanceof PsiVariable) {
                    newAssignedVariable = newOperandReference;
                }

                setterQualifier = newOperandReference.getQualifierExpression();
            } else if (newOperand instanceof PsiArrayAccessExpression) {
                PsiArrayAccessExpression arrayAccessExpression = (PsiArrayAccessExpression) newOperand;
                newAssignedVariable = (PsiReferenceExpression) arrayAccessExpression.getArrayExpression();
                setterQualifier = newAssignedVariable.getQualifierExpression();
            }

            removeRedundantThisQualifierFromExpression(setterQualifier);

            if (newAssignedVariable != null) {
                PsiElement binding = newAssignedVariable.resolve();
                if (binding instanceof PsiVariable) {
                    PsiVariable variableBinding = (PsiVariable) binding;
                    if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField variableBindingField = (PsiField) variableBinding;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                PsiMethod setterMethodBinding = findSetterMethodInSourceClass((PsiField) variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(newPostfixExpression.getNode()))
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
                                        createSetterMethodInSourceClass(variableBindingField);
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
                                        createGetterMethodInSourceClass(variableBindingField);
                                        sourceFieldBindingsWithCreatedGetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    getterMethodInvocationText = getterMethodName;
                                }
                                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText + "()";

                                if (setterQualifier != null) {
                                    //field to get and to set are the same
                                    getterMethodInvocationText = setterQualifier.getText() + "." + getterMethodInvocationText;
                                }

                                String infixExpressionText = getterMethodInvocationText;
                                if (newPostfixExpression.getOperationSign().getText().equals("++")) {
                                    infixExpressionText += "+";
                                } else if (newPostfixExpression.getOperationSign().getText().equals("--")) {
                                    infixExpressionText += "-";
                                }
                                infixExpressionText += "1";
                                setterMethodInvocationText += infixExpressionText + ")";
                                setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationText;

                                if (setterQualifier != null) {
                                    setterMethodInvocationText = setterQualifier.getText() + setterMethodInvocationText;
                                }

                                newPostfixExpression.replace(factory.createExpressionFromText(setterMethodInvocationText, null));
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (PsiExpression expression2 : newAccessedVariables) {
                    PsiReferenceExpression newAccessedVariable = (PsiReferenceExpression) expression2;
                    PsiElement rightHandBinding = newAccessedVariable.resolve();
                    if (rightHandBinding instanceof PsiVariable) {
                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                        if (accessedVariableBinding instanceof PsiField && (!accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                            PsiField accessedVariableBindingField = (PsiField) accessedVariableBinding;

                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBindingField)) {
                                        if (isParentAnonymousClassDeclaration(newPostfixExpression.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(newAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
                                                newAccessedVariable);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<PsiExpression> newPrefixExpressions = expressionExtractor.getPrefixExpressions(newMethodDeclaration.getBody().getStatements());
        for (PsiExpression expression : newPrefixExpressions) {
            PsiPrefixExpression newPrefixExpression = (PsiPrefixExpression) expression;
            PsiExpression newOperand = newPrefixExpression.getOperand();
            PsiJavaToken newOperator = newPrefixExpression.getOperationSign();

            PsiReferenceExpression newAssignedVariable = null;
            PsiExpression setterQualifier = null;
            if (newOperand instanceof PsiReferenceExpression) {
                if (newOperand instanceof PsiField) {
                    newAssignedVariable = (PsiReferenceExpression) newOperand;
                }

                setterQualifier = ((PsiReferenceExpression) newOperand).getQualifierExpression();
            }

            removeRedundantThisQualifierFromExpression(setterQualifier);

            if (newAssignedVariable != null && (newOperator.getText().equals("++")) ||
                    newOperator.getText().equals("--")) {
                PsiElement binding = newAssignedVariable.resolve();
                if (binding instanceof PsiVariable) {
                    PsiVariable variableBinding = (PsiVariable) binding;
                    if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                        PsiField variableBindingField = (PsiField) variableBinding;

                        if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                            if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                PsiMethod setterMethodBinding = findSetterMethodInSourceClass((PsiField) variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if (isParentAnonymousClassDeclaration(newPrefixExpression.getNode()))
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
                                        createSetterMethodInSourceClass(variableBindingField);
                                        sourceFieldBindingsWithCreatedSetterMethod.add((PsiField) variableBinding);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    setterMethodInvocationText = setterMethodName;
                                }
                                PsiMethod getterMethodBinding = findGetterMethodInSourceClass(variableBindingField);
                                String getterMethodInvocationText;
                                if (getterMethodBinding != null) {
                                    getterMethodInvocationText = getterMethodBinding.getName();
                                } else {
                                    if (!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBindingField)) {
                                        createGetterMethodInSourceClass(variableBindingField);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(variableBindingField);
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    getterMethodInvocationText = getterMethodName;
                                }
                                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText + "()";

                                if (setterQualifier != null) {
                                    getterMethodInvocationText = setterQualifier.getText() + "." + getterMethodInvocationText;
                                }

                                String infixExpressionText = getterMethodInvocationText;

                                if (newOperator.getText().equals("++")) {
                                    infixExpressionText += " + 1";
                                } else if (newOperator.getText().equals("--")) {
                                    infixExpressionText += "- 1";
                                }
                                setterMethodInvocationText += "(" + infixExpressionText + ")";
                                setterMethodInvocationText = modifiedSourceTypeName + "." + setterMethodInvocationText;

                                if (setterQualifier != null) {
                                    setterMethodInvocationText = setterQualifier.getText() + setterMethodInvocationText;
                                }

                                newPrefixExpression.replace(factory.createExpressionFromText(setterMethodInvocationText, sourceMethod));
                            }
                        }
                    }
                }
            } else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                List<PsiExpression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for (PsiExpression expression2 : newAccessedVariables) {
                    PsiReferenceExpression newAccessedVariable = (PsiReferenceExpression) expression2;
                    PsiElement rightHandBinding = newAccessedVariable.resolve();
                    if (rightHandBinding instanceof PsiVariable) {
                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                        if (accessedVariableBinding instanceof PsiField && (!accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                            PsiField accessedVariableBindingField = (PsiField) accessedVariableBinding;
                            if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) accessedVariableBinding)) {
                                if (!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if (sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBindingField)) {
                                        if (isParentAnonymousClassDeclaration(newPrefixExpression.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(newAccessedVariable.getNode()))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                fieldParameterMap,
                                                newAccessedVariable);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<PsiReferenceExpression> newFieldInstructions = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> {
            for (PsiExpression expression : expressionExtractor.getVariableInstructions(statement)) {
                newFieldInstructions.add((PsiReferenceExpression) expression);
            }
        });

        for (PsiReferenceExpression expression : newFieldInstructions) {
            removeRedundantThisQualifierFromExpression(expression);
        }

        for (PsiExpression expression : newFieldInstructions) {
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            PsiElement binding = simpleName.resolve();
            if (binding instanceof PsiVariable) {
                PsiVariable variableBinding = (PsiVariable) binding;
                if (variableBinding instanceof PsiField && (!variableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                    PsiField variableBindingField = (PsiField) variableBinding;
                    if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) variableBinding)) {
                        if (!variableBindingCorrespondsToExtractedField(variableBinding)) {
                            if (!isAssignmentChild(expression)) {

                                if (sourceFieldBindingsWithCreatedSetterMethod.contains(variableBindingField)) {
                                    if (isParentAnonymousClassDeclaration(simpleName.getNode()))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod,
                                            newMethodDeclaration,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            (PsiReferenceExpression) expression);
                                } else {
                                    if (isParentAnonymousClassDeclaration(simpleName.getNode()))
                                        fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            fieldParameterMap,
                                            (PsiReferenceExpression) expression);
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
        }

        List<PsiExpression> sourceMethodInvocations = new ArrayList<>();
        Arrays.stream(sourceMethod.getBody().getStatements()).forEach(statement -> sourceMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement)));

        List<PsiExpression> newMethodInvocations = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> newMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement)));

        int j = 0;
        for (PsiExpression expression : sourceMethodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                PsiReferenceExpression methodExpression = methodInvocation.getMethodExpression();

                PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
                if (qualifierExpression == null || qualifierExpression instanceof PsiThisExpression) {
                    PsiMethod methodBinding = methodInvocation.resolveMethod();

                    if (methodBinding != null && methodBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                        PsiMethod[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
                        for (PsiMethod sourceMethodDeclaration : sourceMethodDeclarations) {
                            if (sourceMethodDeclaration.equals(methodInvocation.resolveMethod())) {
                                if (!methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethod()) &&
                                        !newMethodDeclaration.equals(methodInvocation.resolveMethod())) {
                                    PsiReferenceExpression fieldNameReference = (PsiReferenceExpression) MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
                                    PsiField fieldName = null;
                                    if (fieldNameReference != null) {
                                        fieldName = (PsiField) fieldNameReference.resolve();
                                    }
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    PsiModifierList modifiers = sourceMethodDeclaration.getModifierList();
                                    PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) newMethodInvocations.get(j);

                                    removeRedundantThisQualifierFromExpression(newMethodInvocation);

                                    if (modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                                        PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(sourceTypeDeclaration.getName() + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newExpression);
                                        if (!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethod())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethod(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethod());
                                        }
                                    } else if (fieldName != null) {
                                        if (!variableBindingCorrespondsToExtractedField(fieldName)) {
                                            if (fieldName.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                String qualifier = sourceTypeDeclaration.getName();
                                                PsiExpression qualifiedName = factory.createExpressionFromText(qualifier + "." + fieldName.getName(), null);
                                                PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(qualifiedName);
                                                newMethodInvocations.set(j, newExpression);
                                                setPublicModifierToSourceField(fieldName);
                                            } else {
                                                if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                                    fieldParameterFinalMap.put(new PlainVariable(fieldName), true);
                                                if (!containsVariable(fieldName, additionalArgumentsAddedToMovedMethod)) {
                                                    PsiParameter fieldParameter = addParameterToMovedMethod(newMethodDeclaration, fieldName);
                                                    addVariable(fieldName, additionalArgumentsAddedToMovedMethod);
                                                    additionalParametersAddedToMovedMethod.add(fieldParameter);
                                                    fieldParameterMap.put(new PlainVariable(fieldName), fieldParameter);
                                                }

                                                String parameterNameString = createNameForParameterizedFieldAccess(fieldName.getName());

                                                PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(parameterNameString, null));
                                                newMethodInvocations.set(j, newExpression);
                                            }
                                        } else {
                                            PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(fieldName.getName(), null));
                                            newMethodInvocations.set(j, newExpression);
                                        }
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }

                                        PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(modifiedSourceTypeName + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newExpression);
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
                        while (superclassTypeBinding != null && methodBinding != null && !superclassTypeBinding.equals(methodBinding.getContainingClass())) {
                            superclassTypeBinding = superclassTypeBinding.getSuperClass();
                        }
                        if (superclassTypeBinding != null) {
                            PsiMethod[] superclassMethodBindings = superclassTypeBinding.getMethods();
                            for (PsiMethod superclassMethodBinding : superclassMethodBindings) {
                                if (superclassMethodBinding.equals(methodBinding)) {
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) newMethodInvocations.get(j);

                                    removeRedundantThisQualifierFromExpression(newMethodInvocation);

                                    if (superclassMethodBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                        String qualifier = sourceTypeDeclaration.getName();
                                        PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(qualifier + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newExpression);
                                    } else {
                                        if (isParentAnonymousClassDeclaration(methodInvocation.getNode()))
                                            sourceClassParameterShouldBeFinal = true;
                                        if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }

                                        PsiExpression newExpression = (PsiExpression) newMethodInvocation.replace(factory.createExpressionFromText(modifiedSourceTypeName + "." + newMethodInvocation.getText(), sourceMethod));
                                        newMethodInvocations.set(j, newExpression);
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
                    if (methodInvocation.getMethodExpression() != null && ((PsiMethod) methodInvocation.getMethodExpression().resolve()).getContainingClass().equals(sourceTypeDeclaration)) {
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
        for (PsiExpression invocation : newMethodInvocations) {
            if (invocation instanceof PsiMethodCallExpression) {
                removeRedundantThisQualifierFromExpression(invocation);
                PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) invocation;
                PsiExpression[] arguments = methodInvocation.getArgumentList().getExpressions();
                for (PsiExpression argument : arguments) {
                    if (argument instanceof PsiThisExpression) {
                        if (isParentAnonymousClassDeclaration(invocation.getNode()))
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
        }
        //replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments
        List<PsiExpression> newClassInstanceCreations = expressionExtractor.getClassInstanceCreations(newMethodDeclaration.getBody().getStatements());
        for (PsiExpression creation : newClassInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) creation;
            PsiExpression[] arguments = new PsiExpression[0];

            PsiExpressionList argumentList = classInstanceCreation.getArgumentList();
            if (argumentList != null) {
                arguments = argumentList.getExpressions();
            }

            for (PsiExpression argument : arguments) {
                if (argument instanceof PsiThisExpression) {
                    if (isParentAnonymousClassDeclaration(creation.getNode()))
                        sourceClassParameterShouldBeFinal = true;
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }

                    argument.replace(factory.createExpressionFromText(modifiedSourceTypeName, null));
                }
            }
            PsiNewExpression newClassInstanceCreation = (PsiNewExpression) creation;

            PsiClass classInstanceCreationTypeBinding = null;
            if (newClassInstanceCreation.getClassReference() != null) {
                classInstanceCreationTypeBinding = (PsiClass) newClassInstanceCreation.getClassReference().resolve();
            }

            if (classInstanceCreationTypeBinding != null && classInstanceCreationTypeBinding.getContainingClass() != null && newClassInstanceCreation.getAnonymousClass() == null &&
                    sourceTypeDeclaration.equals(classInstanceCreationTypeBinding.getContainingClass())) {
                if (isParentAnonymousClassDeclaration(creation.getNode()))
                    sourceClassParameterShouldBeFinal = true;
                if (classInstanceCreationTypeBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    String qualifierType = sourceTypeDeclaration.getName();
                    String qualifiedType = qualifierType + "." + classInstanceCreationTypeBinding.getName();
                    classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(qualifiedType, null));
                    setPublicModifierToSourceMemberType(classInstanceCreationTypeBinding);
                } else {
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(modifiedSourceTypeName, null));
                    String simpleNameType = newClassInstanceCreation.getType().getCanonicalText();
                    if (simpleNameType != null) {
                        classInstanceCreation.getQualifier().replace(factory.createExpressionFromText(simpleNameType, null));
                    }
                }
            }
        }

        //replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers
        StatementExtractor statementExtractor = new StatementExtractor();
        List<PsiVariable> newVariableDeclarationFragments = new ArrayList<>();

        List<PsiStatement> newVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody().getStatements());
        for (PsiStatement statement : newVariableDeclarationStatements) {
            PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;

            for (PsiElement element : variableDeclarationStatement.getDeclaredElements()) {
                PsiVariable fragment = (PsiVariable) element;
                newVariableDeclarationFragments.add(fragment);
            }
        }

        List<PsiStatement> newVariableDeclarationExpressions = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody().getStatements());
        for (PsiStatement expression : newVariableDeclarationExpressions) {
            PsiDeclarationStatement variableDeclarationExpression = (PsiDeclarationStatement) expression;

            for (PsiElement element : variableDeclarationExpression.getDeclaredElements()) {
                PsiVariable fragment = (PsiVariable) element;
                newVariableDeclarationFragments.add(fragment);
            }
        }

        for (PsiVariable fragment : newVariableDeclarationFragments) {
            PsiExpression initializer = fragment.getInitializer();
            if (initializer instanceof PsiThisExpression) {
                if (isParentAnonymousClassDeclaration(fragment.getNode()))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                fragment.getInitializer().replace(factory.createExpressionFromText(modifiedSourceTypeName, null));
            }
        }

        //replaceThisExpressionWithSourceClassParameterInReturnStatementExpressions
        List<PsiStatement> newReturnStatements = statementExtractor.getReturnStatements(newMethodDeclaration.getBody().getStatements());
        for (PsiStatement statement : newReturnStatements) {
            PsiReturnStatement newReturnStatement = (PsiReturnStatement) statement;
            if (newReturnStatement.getReturnValue() instanceof PsiThisExpression) {
                if (isParentAnonymousClassDeclaration(statement.getNode()))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if (!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                newReturnStatement.getReturnValue().replace(factory.createExpressionFromText(modifiedSourceTypeName, null));
            }
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

    /**
     * Transform signs such as '+=' to '+'
     */
    private String removeEQFromOperationSign(PsiJavaToken operationSign) {
        String operationSignText = operationSign.getText();
        StringBuilder resultOperationSign = new StringBuilder();
        for (int i = 0; i < operationSignText.length(); i++) {
            char c = operationSignText.charAt(i);
            if (c != '=') {
                resultOperationSign.append(c);
            }
        }

        return resultOperationSign.toString();
    }

    /**
     * Removes `this` qualifier from the extracted elements. Note that `OuterClass.this` and `super` are forbidden for the
     * extracted members, so no need to remove them.
     */
    private void removeRedundantThisQualifierFromExpression(PsiElement element) {
        if (element instanceof PsiReferenceExpression) {
            PsiReferenceExpression expression = (PsiReferenceExpression) element;
            PsiExpression qualifier = expression.getQualifierExpression();

            if (qualifier == null) {
                return;
            }

            PsiElement resolvedElement = ((PsiReferenceExpression) element).resolve();

            if (resolvedElement instanceof PsiField) {
                PsiField sourceField = sandboxToSourceFieldMap.get(resolvedElement);
                if (extractedFieldFragments.contains(sourceField)) {
                    return;
                }
            } else if (resolvedElement instanceof PsiMethod) {
                for (PsiMethod sourceMethod : sourceToSandboxMethodMap.keySet()) {
                    if (sourceToSandboxMethodMap.get(sourceMethod).equals(resolvedElement)) {
                        if (extractedMethods.contains(sourceMethod)) {
                            return;
                        } else {
                            break;
                        }
                    }
                }
            }

            if (qualifier instanceof PsiThisExpression) {
                qualifier.delete();
            } else if (qualifier.getFirstChild() instanceof PsiThisExpression) {
                qualifier.getFirstChild().delete();
            }
        }
    }

    private void handleAccessedFieldNotHavingSetterMethod(PsiMethod sourceMethod,
                                                          PsiMethod newMethodDeclaration,
                                                          Map<PlainVariable, PsiParameter> fieldParameterMap, PsiReferenceExpression newAccessedVariable) {
        PsiField sandboxField = (PsiField) newAccessedVariable.resolve();
        PsiField sourceField = sandboxToSourceFieldMap.get(sandboxField);

        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
        Set<PsiParameter> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
        if (!containsVariable(sourceField, additionalArgumentsAddedToMovedMethod)) {
            PsiParameter fieldParameter = addParameterToMovedMethod(newMethodDeclaration, sourceField);
            addVariable(sourceField, additionalArgumentsAddedToMovedMethod);
            additionalParametersAddedToMovedMethod.add(fieldParameter);
            fieldParameterMap.put(new PlainVariable(sourceField), fieldParameter);
        }

        if (newAccessedVariable.resolve() instanceof PsiField) {
            PsiExpression qualifier = newAccessedVariable.getQualifierExpression();
            if (qualifier == null || qualifier instanceof PsiThisExpression) {
                String parameterName = createNameForParameterizedFieldAccess(((PsiField) newAccessedVariable.resolve()).getName());
                newAccessedVariable.getLastChild().replace(factory.createExpressionFromText(parameterName, null));
            }
        } else {
            if (!(newAccessedVariable instanceof PsiQualifiedExpression)) {
                String parameterName = createNameForParameterizedFieldAccess(sourceField.getName());
                newAccessedVariable.replace(factory.createExpressionFromText(parameterName, null));
            }
        }
    }

    private PsiParameter handleAccessedFieldHavingSetterMethod(PsiMethod sourceMethod, PsiMethod newMethodDeclaration, PsiParameter sourceClassParameter,
                                                               String modifiedSourceTypeName, PsiReferenceExpression newAccessedVariable) {
        PsiField sandboxField = (PsiField) newAccessedVariable.resolve();
        PsiField sourceField = sandboxToSourceFieldMap.get(sandboxField);

        PsiMethod getterMethodBinding = findGetterMethodInSourceClass(sourceField);
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
            if (!sourceFieldBindingsWithCreatedGetterMethod.contains(sourceField)) {
                createGetterMethodInSourceClass(sourceField);
                sourceFieldBindingsWithCreatedGetterMethod.add(sourceField);
            }
            String originalFieldName = sourceField.getName();
            String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
            String getterMethodName = GETTER_PREFIX + modifiedFieldName;
            getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
            getterMethodInvocationText = getterMethodName + "()";
        }

        if (newAccessedVariable.resolve() instanceof PsiField) {
            PsiExpression qualifier = newAccessedVariable.getQualifierExpression();
            if (qualifier == null || qualifier instanceof PsiThisExpression) {
                getterMethodInvocationText = modifiedSourceTypeName + "." + getterMethodInvocationText;
                newAccessedVariable.replace(factory.createExpressionFromText(getterMethodInvocationText, sourceMethod));
            }
        } else if (newAccessedVariable instanceof PsiQualifiedExpression) {
            PsiExpression qualifier = newAccessedVariable.getQualifierExpression();
            if (qualifier != null) {
                getterMethodInvocationText = qualifier.getText() + "." + getterMethodInvocationText;
            }

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
        PsiField sourceField = sandboxToSourceFieldMap.get(variableBinding);

        return RefactoringUtility.findDeclaringTypeDeclaration(sourceField, sourceTypeDeclaration) != null
                && !extractedFieldFragments.contains(sourceField);
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

    private void modifySourceStaticFieldInstructionsInTargetClass(PsiMethod newMethodDeclaration) {
        ExpressionExtractor extractor = new ExpressionExtractor();
        List<PsiExpression> newVariableInstructions = extractor.getVariableInstructions(newMethodDeclaration.getBody().getStatements());

        for (PsiExpression expression : newVariableInstructions) {
            removeRedundantThisQualifierFromExpression(expression);
        }

        for (PsiExpression expression : newVariableInstructions) {
            PsiReferenceExpression simpleName = (PsiReferenceExpression) expression;
            PsiVariable binding = (PsiVariable) simpleName.resolve();
            if (binding != null) {
                if (binding instanceof PsiField && (binding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                    if (declaredInSourceTypeDeclarationOrSuperclass((PsiField) binding)) {
                        String qualifier = sourceTypeDeclaration.getName();
                        if (simpleName.resolve() instanceof PsiField) {
                            PsiReferenceExpression fieldAccess = (PsiReferenceExpression) expression;
                            fieldAccess.setQualifierExpression(factory.createExpressionFromText(sourceTypeDeclaration.getName(), null));
                        } else if (RefactoringUtility.needsQualifier(simpleName)) {
                            PsiReferenceExpression newSimpleName = (PsiReferenceExpression) simpleName.copy();
                            PsiExpression newQualifiedName = factory.createExpressionFromText(qualifier + "." + newSimpleName.getText(), null);
                            simpleName.replace(newQualifiedName);
                        }
                        setPublicModifierToSourceField((PsiField) binding);
                    } else {
                        PsiClass qualifier;
                        if (binding.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                            qualifier = ((PsiField) binding).getContainingClass();
                        } else {
                            qualifier = sourceTypeDeclaration;
                            //If source class is inner class, this field is from outer class and is not public, here will a wrong result both in this and an original plugin.
                        }
                        if (simpleName.resolve() instanceof PsiField) {
                            PsiReferenceExpression fieldAccess = (PsiReferenceExpression) expression;

                            fieldAccess.setQualifierExpression(factory.createReferenceExpression(qualifier));
                        } else if (RefactoringUtility.needsQualifier(simpleName)) {
                            String newSimpleName = ((PsiVariable) simpleName.resolve()).getName();
                            PsiReferenceExpression newQualifiedName = (PsiReferenceExpression) factory.createExpressionFromText(qualifier.getText() + "." + newSimpleName, null);
                            simpleName.replace(newQualifiedName);
                        }
                        PsiClass fieldDeclaringClass = ((PsiField) binding).getContainingClass();
                        if (fieldDeclaringClass != null && fieldDeclaringClass.isEnum() && sourceTypeDeclaration.equals(fieldDeclaringClass.getContainingClass())) {
                            setPublicModifierToSourceMemberType(fieldDeclaringClass);
                        }
                    }
                }
            }
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

    private void setPublicModifierToSourceField(PsiField variableBinding) {
        if (sandboxToSourceFieldMap.containsKey(variableBinding)) {
            variableBinding = sandboxToSourceFieldMap.get(variableBinding);
        }

        variableBinding.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
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

        String assigmentText;
        if (fieldFragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            assigmentText = extractedTypeName;
        } else {
            assigmentText = "this";
        }
        assigmentText += "." + fieldFragment.getName() + " = " + fieldFragment.getName() + ";"; //this.fieldName = fieldName (parameter)
        PsiStatement assignment = factory.createStatementFromText(assigmentText, sourceFile);
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
        PsiReturnStatement returnStatement = (PsiReturnStatement) factory.createStatementFromText("return " + fieldFragment.getName() + ";", null);
        getterMethodDeclaration.getBody().addBefore(returnStatement, getterMethodDeclaration.getBody().getRBrace());

        return getterMethodDeclaration;
    }

    private enum ModifyType {
        SOURCE, EXTRACTED
    }

    /**
     * This method both modifies a source class and collect info for the extracted one. However, we cannot change source class
     * before creating a new one, so here we make corresponding actions to the source class only if `modifyType` == SOURCE.
     */
    private void createExtractedTypeFieldReferenceInSourceClass(ModifyType modifyType) {
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);

        PsiExpression initializer = null;

        if (constructorFinalFieldAssignmentMap.isEmpty()) {
            initializer = factory.createExpressionFromText("new " + extractedTypeName + "()", null);
        } else {
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();

            String extractedTypeFieldAccessText = "this." + modifiedExtractedTypeName;

            PsiMethodCallExpression constructorCall = (PsiMethodCallExpression) factory.createExpressionFromText(extractedTypeName + "()", null);

            for (PsiMethod constructor : constructorFinalFieldAssignmentMap.keySet()) {
                Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
                Set<PsiParameter> extractedClassConstructorParameters = new LinkedHashSet<>();

                StatementExtractor statementExtractor = new StatementExtractor();
                List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(constructor.getBody().getStatements());
                List<PsiStatement> insertAfterStatements = new ArrayList<>();
                for (PsiField fieldFragment : finalFieldAssignmentMap.keySet()) {
                    PsiAssignmentExpression fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
                    List<PsiExpression> variableInstructions = expressionExtractor.getVariableInstructions(fieldAssignment.getRExpression());
                    for (PsiExpression expression : variableInstructions) {
                        PsiReferenceExpression variable = (PsiReferenceExpression) expression;
                        boolean foundInOriginalConstructorParameters = false;
                        PsiParameter[] originalConstructorParameters = constructor.getParameterList().getParameters();
                        for (PsiParameter originalConstructorParameter : originalConstructorParameters) {
                            if (originalConstructorParameter.equals(variable.resolve())) {
                                if (!extractedClassConstructorParameters.contains(originalConstructorParameter)) {
                                    constructorCall.getArgumentList().add(variable);
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
                                    if (fragment instanceof PsiVariable && fragment.equals(variable.resolve())) {
                                        PsiVariable fragmentVariable = (PsiVariable) fragment;
                                        if (extractedClassConstructorParameters.stream().noneMatch(param -> param.getName().equals(fragmentVariable.getName()))) {
                                            constructorCall.getArgumentList().add(variable);
                                            PsiParameter fragmentParameter = factory.createParameter(fragmentVariable.getName(), fragmentVariable.getType());
                                            extractedClassConstructorParameters.add(fragmentParameter);
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

                if (modifyType == ModifyType.SOURCE) {
                    PsiStatement assignmentStatement = factory.createStatementFromText(extractedTypeFieldAccessText + " = new " + constructorCall.getText() + ";", null);

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

                        constructor.getBody().addAfter(assignmentStatement, lastStatement);
                    } else {
                        constructor.getBody().addAfter(assignmentStatement, constructor.getBody().getLBrace());
                    }
                }

                extractedClassConstructorParameterMap.put(constructor, extractedClassConstructorParameters);
            }
        }

        if (modifyType == ModifyType.SOURCE) {
            PsiField extractedReferenceFieldDeclaration = factory.createField(modifiedExtractedTypeName, factory.createTypeFromText(extractedTypeName, null));
            extractedReferenceFieldDeclaration.setInitializer(initializer);
            extractedReferenceFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
            PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
            if (serializableTypeBinding != null && !existsNonTransientExtractedFieldFragment()) {
                extractedReferenceFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.TRANSIENT, true);
                updateReadObjectInSourceClass(modifiedExtractedTypeName);
                updateWriteObjectInSourceClass(modifiedExtractedTypeName);
            }
            updateCloneInSourceClass(modifiedExtractedTypeName);
            sourceTypeDeclaration.addAfter(extractedReferenceFieldDeclaration, sourceTypeDeclaration.getLBrace());
        }
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
                        PsiStatement methodInvocationStatement = createMethodInvocationStatementForWriteObject(modifiedExtractedTypeName, parameterSimpleName.getName());
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
                        PsiStatement assignmentStatement = createAssignmentStatementForReadObject(modifiedExtractedTypeName, parameterSimpleName.getName());
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

            PsiCodeBlock methodBody = factory.createCodeBlock();

            String methodInvocationText = parameterSimpleName + "." + "defaultReadObject" + "();";

            methodBody.addAfter(factory.createStatementFromText(methodInvocationText, null), methodBody.getLBrace());
            PsiStatement assignmentStatement = createAssignmentStatementForReadObject(modifiedExtractedTypeName, parameterSimpleName);

            methodBody.addAfter(assignmentStatement, methodBody.getLBrace());

            readObjectMethod.getBody().replace(methodBody);
            sourceTypeDeclaration.addAfter(readObjectMethod, sourceTypeDeclaration.getLBrace());
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
                PsiExpression superCloneInvocation = factory.createExpressionFromText("(" + sourceClassType.getClassName() + ")" + " super.clone()", null);
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


    private enum HandleType {
        COLLECT, MODIFY
    }

    /**
     * This map collects required info for the extracted class and modifies the source class. We cannot do the second before
     * collecting thee first info to create an extracted class.
     */
    private void handleExtractedFieldAssignmentsInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> modifiedFields, Set<PsiField> accessedFields, HandleType handleType) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1);
        for (PsiMethod methodDeclaration : contextMethods) {
            if (!extractedMethods.contains(methodDeclaration)) {
                PsiCodeBlock methodBody = methodDeclaration.getBody();
                if (methodBody != null) {
                    PsiStatement[] statements = methodBody.getStatements();
                    for (PsiStatement statement : statements) {
                        List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
                        for (PsiExpression expression : assignments) {
                            if (!expression.isValid()) {
                                continue;
                            }

                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                            PsiExpression leftHandSide = assignment.getLExpression();
                            PsiElement assignedVariable = null;
                            PsiExpression setterQualifier = null;
                            if (leftHandSide instanceof PsiReferenceExpression) {
                                assignedVariable = ((PsiReferenceExpression) leftHandSide).resolve();
                                setterQualifier = ((PsiReferenceExpression) leftHandSide).getQualifierExpression();
                            }

                            PsiElement rightHandSide = assignment.getRExpression();

                            boolean deleteAssignmentStatement = false;

                            List<PsiExpression> arrayAccesses = expressionExtractor.getArrayAccesses(leftHandSide);
                            for (PsiField fieldFragment : fieldFragments) {
                                String originalFieldName = fieldFragment.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                                if (assignedVariable != null) {
                                    if (assignedVariable instanceof PsiVariable) {
                                        PsiVariable assignedVariableBinding = (PsiVariable) assignedVariable;
                                        if (assignedVariableBinding instanceof PsiField && fieldFragment.equals(assignedVariableBinding)) {
                                            if (handleType == HandleType.COLLECT) {
                                                modifiedFields.add(fieldFragment);
                                            }

                                            if (methodDeclaration.isConstructor() && (assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.FINAL))) {
                                                if (assignment.getParent() instanceof PsiExpressionStatement) {
                                                    if (constructorFinalFieldAssignmentMap.containsKey(methodDeclaration)) {
                                                        Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(methodDeclaration);
                                                        finalFieldAssignmentMap.put(fieldFragment, (PsiAssignmentExpression) assignment.copy());
                                                    } else {
                                                        Map<PsiField, PsiAssignmentExpression> finalFieldAssignmentMap = new LinkedHashMap<>();
                                                        finalFieldAssignmentMap.put(fieldFragment, (PsiAssignmentExpression) assignment.copy());
                                                        constructorFinalFieldAssignmentMap.put(methodDeclaration, finalFieldAssignmentMap);
                                                    }

                                                    if (handleType == HandleType.MODIFY) {
                                                        deleteAssignmentStatement = true;
                                                    }
                                                }
                                            } else {
                                                String setterMethodInvocationText;

                                                String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                                setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
                                                setterMethodInvocationText = setterMethodName;
                                                if (!assignment.getOperationSign().getTokenType().equals(JavaTokenType.EQ)) {
                                                    if (handleType == HandleType.COLLECT) {
                                                        accessedFields.add(fieldFragment);
                                                    }

                                                    String infixExpressionText;

                                                    String getterMethodInvocationText = getterMethodName;

                                                    if (assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                        getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                    } else {
                                                        getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                    }

                                                    if (setterQualifier != null) {
                                                        getterMethodInvocationText = setterQualifier.getText() + "." + getterMethodInvocationText;
                                                    }


                                                    getterMethodInvocationText += "()";

                                                    String resultOperationSign = removeEQFromOperationSign(assignment.getOperationSign());

                                                    infixExpressionText = getterMethodInvocationText + resultOperationSign + assignment.getRExpression().getText();
                                                    setterMethodInvocationText += "(" + infixExpressionText + ")";
                                                } else {
                                                    setterMethodInvocationText += "(" + assignment.getRExpression().getText() + ")";
                                                }

                                                if (leftHandSide instanceof PsiQualifiedReference) {
                                                    String prefix = modifiedExtractedTypeName;

                                                    if (setterQualifier != null && !(setterQualifier instanceof PsiThisExpression)) {
                                                        prefix = setterQualifier.getText() + "." + prefix;
                                                    }

                                                    setterMethodInvocationText = prefix + "." + setterMethodInvocationText;
                                                } else if ((assignedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC))) {
                                                    setterMethodInvocationText = extractedTypeName + "." + setterMethodInvocationText;
                                                } else {
                                                    setterMethodInvocationText = modifiedExtractedTypeName + "." + setterMethodInvocationText;
                                                }

                                                if (handleType == HandleType.MODIFY) {
                                                    rightHandSide = assignment.replace(factory.createExpressionFromText(setterMethodInvocationText, null));
                                                    rightHandSide = ((PsiMethodCallExpression) rightHandSide).getArgumentList();
                                                }
                                            }
                                        }
                                    }
                                }

                                for (PsiExpression expression2 : arrayAccesses) {
                                    PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression2;
                                    PsiExpression arrayExpression = arrayAccess.getArrayExpression();

                                    if (arrayExpression instanceof PsiMethodCallExpression) {
                                        continue;
                                    }

                                    PsiReferenceExpression arrayVariable = (PsiReferenceExpression) arrayExpression;
                                    if (arrayVariable != null) {
                                        PsiElement arrayBinding = arrayVariable.resolve();
                                        if (arrayBinding instanceof PsiVariable) {
                                            PsiVariable arrayVariableBinding = (PsiVariable) arrayBinding;
                                            if (arrayVariableBinding instanceof PsiField && fieldFragment.equals(arrayVariableBinding)) {
                                                if (handleType == HandleType.COLLECT) {
                                                    accessedFields.add(fieldFragment);
                                                }

                                                PsiMethodCallExpression getterMethodInvocation;
                                                String getterMethodInvocationText = getterMethodName;
                                                if (arrayVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                    getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                                } else {
                                                    getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                                }

                                                PsiExpression getterQualifier = arrayVariable.getQualifierExpression();
                                                if (getterQualifier != null) {
                                                    getterMethodInvocationText = getterQualifier.getText() + "." + getterMethodInvocationText;
                                                }

                                                getterMethodInvocationText += "()";
                                                getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);

                                                if (handleType == HandleType.MODIFY) {
                                                    arrayExpression.replace(getterMethodInvocation);
                                                }
                                            }
                                        }
                                    }
                                }

                                //Moved down here from the original place because rightHandSide could invalidate when replacing assignment with setter.
                                List<PsiExpression> accessedVariables;

                                if (!rightHandSide.isValid()) {
                                    continue;
                                }

                                if (rightHandSide instanceof PsiExpressionList) {
                                    PsiExpressionList rightHandSideExpressionList = (PsiExpressionList) rightHandSide;
                                    accessedVariables = expressionExtractor.getVariableInstructions(rightHandSideExpressionList.getExpressions());
                                } else {
                                    accessedVariables = expressionExtractor.getVariableInstructions((PsiExpression) rightHandSide);
                                }

                                for (PsiExpression expression2 : accessedVariables) {
                                    PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression2;
                                    PsiElement rightHandBinding = accessedVariable.resolve();
                                    if (rightHandBinding instanceof PsiVariable) {
                                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                                        if (accessedVariableBinding instanceof PsiField && fieldFragment.equals(accessedVariableBinding)) {
                                            if (handleType == HandleType.COLLECT) {
                                                accessedFields.add(fieldFragment);
                                            }

                                            PsiMethodCallExpression getterMethodInvocation;
                                            String getterMethodInvocationText = getterMethodName;
                                            if (accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }

                                            PsiExpression getterQualifier = accessedVariable.getQualifierExpression();
                                            if (getterQualifier != null) {
                                                getterMethodInvocationText = getterQualifier.getText() + "." + getterMethodInvocationText;
                                            }

                                            getterMethodInvocationText += "()";
                                            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);

                                            if (handleType == HandleType.MODIFY) {
                                                accessedVariable.replace(getterMethodInvocation);
                                            }
                                        }
                                    }
                                }

                                if (deleteAssignmentStatement) {
                                    assignment.getParent().delete();
                                }
                            }
                        }
                        List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
                        for (PsiExpression expression : postfixExpressions) {
                            PsiPostfixExpression postfix = (PsiPostfixExpression) expression;
                            PsiExpression operand = postfix.getOperand();

                            PsiElement assignedVariable = null;
                            PsiExpression setterQualifier = null;
                            if (operand instanceof PsiReferenceExpression) {
                                assignedVariable = ((PsiReferenceExpression) operand).resolve();
                                setterQualifier = ((PsiReferenceExpression) operand).getQualifierExpression();
                            }

                            List<PsiExpression> arrayAccesses = expressionExtractor.getArrayAccesses(operand);
                            for (PsiField fieldFragment : fieldFragments) {
                                String originalFieldName = fieldFragment.getName();
                                String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                                String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                                if (assignedVariable != null) {
                                    PsiVariable operandBinding = (PsiVariable) assignedVariable;
                                    if (operandBinding instanceof PsiField && fieldFragment.equals(operandBinding)) {
                                        if (handleType == HandleType.COLLECT) {
                                            accessedFields.add(fieldFragment);
                                            modifiedFields.add(fieldFragment);
                                        }

                                        PsiMethodCallExpression setterMethodInvocation;
                                        String setterMethodInvocationText;
                                        String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                        setterMethodName = appendAccessorMethodSuffix(setterMethodName, this.extractedMethods);
                                        setterMethodInvocationText = setterMethodName;


                                        String infixExpressionText = "";

                                        String getterMethodInvocationText = getterMethodName;

                                        if (operandBinding.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                                            getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                        } else {
                                            getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                        }

                                        if (setterQualifier != null) {
                                            getterMethodInvocationText = setterQualifier.getText() + "." + getterMethodInvocationText;
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
                                            setterMethodInvocationText = modifiedExtractedTypeName + "." + setterQualifier.getText() + "." + setterMethodInvocationText;
                                        } else if (operandBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                            setterMethodInvocationText = extractedTypeName + "." + setterMethodInvocationText;
                                        } else {
                                            setterMethodInvocationText = modifiedExtractedTypeName + "." + setterMethodInvocationText;
                                        }
                                        setterMethodInvocationText += ")";

                                        setterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(setterMethodInvocationText, null);

                                        if (handleType == HandleType.MODIFY) {
                                            postfix.replace(setterMethodInvocation);
                                        }

                                    }
                                }
                                for (PsiExpression expression2 : arrayAccesses) {
                                    PsiArrayAccessExpression arrayAccess = (PsiArrayAccessExpression) expression2;
                                    PsiExpression arrayExpression = arrayAccess.getArrayExpression();

                                    if (!(arrayAccess instanceof PsiReferenceExpression)) {
                                        continue;
                                    }

                                    PsiReferenceExpression arrayReference = (PsiReferenceExpression) arrayAccess;
                                    PsiVariable arrayVariable = (PsiVariable) ((PsiReferenceExpression) arrayAccess).resolve();
                                    if (arrayVariable != null) {
                                        if (arrayVariable instanceof PsiField && fieldFragment.equals(arrayVariable)) {
                                            if (handleType == HandleType.COLLECT) {
                                                accessedFields.add(fieldFragment);
                                            }

                                            PsiMethodCallExpression getterMethodInvocation;
                                            String getterMethodInvocationText;
                                            getterMethodInvocationText = getterMethodName;
                                            if (arrayVariable.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }

                                            PsiExpression getterQualifier = arrayReference.getQualifierExpression();
                                            if (getterQualifier != null) {
                                                getterMethodInvocationText = getterQualifier.getText() + "." + getterMethodInvocationText;
                                            }

                                            getterMethodInvocationText += "()";
                                            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);

                                            if (handleType == HandleType.MODIFY) {
                                                arrayExpression.replace(getterMethodInvocation);
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
    }

    private void modifyExtractedFieldAssignmentsInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> modifiedFields, Set<PsiField> accessedFields) {
        //`modifiedField` and `accessedFields` Sets stayed in place in order to save signature of the original plugin methods, but they are redundant because
        //they are used only to collect modified and accessed fields which we already collected in another method.
        handleExtractedFieldAssignmentsInSourceClass(fieldFragments, null, null, HandleType.MODIFY);
    }

    private void collectExtractedFieldAssignmentsInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> modifiedFields, Set<PsiField> accessedFields) {
        handleExtractedFieldAssignmentsInSourceClass(fieldFragments, modifiedFields, accessedFields, HandleType.COLLECT);
    }

    private void handleExtractedFieldAccessesInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> accessedFields, HandleType handleType) {
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
                        for (PsiField fieldFragment : fieldFragments) {
                            String originalFieldName = fieldFragment.getName();
                            String modifiedFieldName = originalFieldName.substring(0, 1).toUpperCase() + originalFieldName.substring(1);
                            String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                            getterMethodName = appendAccessorMethodSuffix(getterMethodName, this.extractedMethods);
                            for (PsiExpression expression : accessedVariables) {
                                if (!expression.isValid()) {
                                    continue;
                                }

                                PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression;
                                PsiElement binding = accessedVariable.resolve();
                                if (binding instanceof PsiVariable) {
                                    PsiVariable accessedVariableBinding = (PsiVariable) binding;
                                    if (accessedVariableBinding instanceof PsiField && fieldFragment.equals(accessedVariableBinding)) {
                                        if (!isAssignmentChild(expression)) {
                                            if (handleType == HandleType.COLLECT) {
                                                accessedFields.add(fieldFragment);
                                            }

                                            PsiMethodCallExpression getterMethodInvocation;
                                            String getterMethodInvocationText = getterMethodName;

                                            if (accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                                getterMethodInvocationText = extractedTypeName + "." + getterMethodInvocationText;
                                            } else {
                                                getterMethodInvocationText = modifiedExtractedTypeName + "." + getterMethodInvocationText;
                                            }

                                            PsiExpression getterQualifier = accessedVariable.getQualifierExpression();
                                            if (getterQualifier != null) {
                                                getterMethodInvocationText = getterQualifier.getText() + "." + getterMethodInvocationText;
                                            }

                                            getterMethodInvocationText += "()";
                                            getterMethodInvocation = (PsiMethodCallExpression) factory.createExpressionFromText(getterMethodInvocationText, null);

                                            if (handleType == HandleType.MODIFY) {
                                                accessedVariable.replace(getterMethodInvocation);
                                            }

                                            rewriteAST = true;
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

    private void modifyExtractedFieldAccessesInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> accessedFields) {
        //`accessedFields` Sets stayed in place in order to save signature of the original plugin methods, but they are redundant because
        //they are used only to collect accessed fields which we already collected in another method.
        handleExtractedFieldAccessesInSourceClass(fieldFragments, null, HandleType.MODIFY);
    }

    private void collectExtractedFieldAccessesInSourceClass(Set<PsiField> fieldFragments, Set<PsiField> accessedFields) {
        handleExtractedFieldAccessesInSourceClass(fieldFragments, accessedFields, HandleType.COLLECT);
    }

    private Set<PsiMethod> getAllMethodDeclarationsInSourceClass() {
        Set<PsiMethod> contextMethods = new LinkedHashSet<>();
        for (PsiField fieldDeclaration : sourceTypeDeclaration.getFields()) {
            contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
        }
        List<PsiMethod> methodDeclarationList = Arrays.asList(sourceTypeDeclaration.getMethods());
        contextMethods.addAll(methodDeclarationList);
        PsiClass[] types = sourceTypeDeclaration.getInnerClasses();
        for (PsiClass type : types) {
            for (PsiField fieldDeclaration : type.getFields()) {
                contextMethods.addAll(getMethodDeclarationsWithinAnonymousClassDeclarations(fieldDeclaration));
            }
            List<PsiMethod> innerMethodDeclarationList = Arrays.asList(type.getMethods());
            contextMethods.addAll(innerMethodDeclarationList);
        }
        return contextMethods;
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

    public void checkInitialConditions(ProgressIndicator indicator) {
        indicator.setText(IntelliJDeodorantBundle.message("god.class.identification.indicator.preconditions"));
    }

    public PsiClass getSourceClass() {
        return sourceTypeDeclaration;
    }

    public PsiFile getSourceFile() {
        return sourceFile;
    }

    public Project getProject() {
        return project;
    }

    public int getExtractedFieldFragmentsCount() {
        return extractedFieldFragments.size();
    }

    public int getExtractedMethodsCount() {
        return extractedMethods.size();
    }
}