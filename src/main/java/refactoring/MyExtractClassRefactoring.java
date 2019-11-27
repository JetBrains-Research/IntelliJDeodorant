package refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.sun.deploy.util.JVMParameters;
import core.ast.ASTReader;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.util.MethodDeclarationUtility;
import core.ast.util.TypeVisitor;
import core.ast.util.math.AdjacencyList;
import core.ast.util.math.Edge;
import core.ast.util.math.Node;
import core.ast.util.math.TarjanAlgorithm;
import org.codehaus.groovy.ast.expr.MethodCallExpression;

import java.util.*;
import java.util.stream.Collectors;

import com.intellij.refactoring.extractclass.*;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;

@SuppressWarnings("FieldCanBeLocal")
public class MyExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private PsiFile sourceFile;
    private PsiClass sourceTypeDeclaration;
    private Set<PsiElement> javaElementsToOpenInEditor;
    private Set<PsiClass> requiredImportDeclarationsInExtractedClass;
    private Map<PsiMethod, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
    private Map<PsiMethod, Set<PsiVariable>> additionalParametersAddedToExtractedMethods;
    private Set<String> sourceMethodBindingsChangedWithPublicModifier;
    private Set<String> sourceFieldBindingsWithCreatedSetterMethod;
    private Set<String> sourceFieldBindingsWithCreatedGetterMethod;
    private Set<PsiField> fieldDeclarationsChangedWithPublicModifier;
    private Set<?> memberTypeDeclarationsChangedWithPublicModifier;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> oldMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, Set<MethodCallExpression>> newMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, PsiMethod> oldToNewExtractedMethodDeclarationMap;
    private Set<PsiField> extractedFieldFragments;
    private Set<PsiMethod> extractedMethods;
    private Set<PsiMethod> delegateMethods;
    private String extractedTypeName;
    private boolean leaveDelegateForPublicMethods;
    //this map holds for each constructor the assignment statements that initialize final extracted fields
    private Map<PsiMethod, Map<PsiVariable, PsiAssignmentExpression>> constructorFinalFieldAssignmentMap;
    //this map hold the parameters that should be passed in each constructor of the extracted class
    private Map<PsiMethod, Set<PsiVariable>> extractedClassConstructorParameterMap;
    private Set<PsiField> extractedFieldsWithThisExpressionInTheirInitializer;
    private Set<PsiMethod> staticallyImportedMethods;

    // TODO comments
    private PsiElementFactory factory;
    private PsiFileFactory fileFactory;
    private Project project;
    private PsiManager psiManager;

    public MyExtractClassRefactoring(PsiFile sourceFile, PsiClass sourceTypeDeclaration,
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
        Set<PsiVariable> modifiedFieldsInNonExtractedMethods = new LinkedHashSet<PsiVariable>();
        Set<PsiVariable> accessedFieldsInNonExtractedMethods = new LinkedHashSet<PsiVariable>();
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

    //TODO comment
    private static PsiMethodCallExpression convertPsiMethodToMethodInvocationExpression(String extractedTypeName, PsiMethod psiMethod) {
        String methodInvocationText = psiMethod.getName();

        if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            sourceRewriter.set(delegation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(), null);
        } else {
            String modifiedExtractedTypeName = extractedTypeName.substring(0, 1).toLowerCase() + extractedTypeName.substring(1, extractedTypeName.length());
            SimpleName expressionName = ast.newSimpleName(modifiedExtractedTypeName);
            sourceRewriter.set(delegation, MethodInvocation.EXPRESSION_PROPERTY, expressionName, null);
        }
        return (PsiMethodCallExpression) factory.createExpressionFromText(, psiMethod);
    }

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

    //TODO ...
    private void createExtractedClass(Set<PsiField> modifiedFieldsInNonExtractedMethods, Set<PsiField> accessedFieldsInNonExtractedMethods) {
        PsiDirectory contextContainer = sourceFile.getContainingDirectory();
        PsiFile extractedClassFile = contextContainer.createFile(extractedTypeName + ".java");

        //javaElementsToOpenInEditor.add(extractedClassICompilationUnit);
        PsiPackage sourcePackage = PsiTreeUtil.findChildrenOfType(sourceFile, PsiPackage.class).iterator().next();
        if(sourcePackage != null) {
            //TODO package package;
        }

        //TODO string ExtractedTypeName = extractedTypeName
        //TODO get у класса того и ему делать set
        //TODO поставили public

        //TODO superы
        PsiClass serializableTypeBinding = implementsSerializableInterface(sourceTypeDeclaration);
        if(serializableTypeBinding != null) {
            //implements SERIALIZABLE, extractedClassImplementedInterfacesRewrite.insertLast(serializableType, null);
            requiredImportDeclarationsInExtractedClass.add(serializableTypeBinding);
        }

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiField> finalFieldFragments = new LinkedHashSet<>();
        Set<PsiField> finalFieldFragmentsWithoutInitializer = new LinkedHashSet<>();
        for(PsiField fieldFragment : extractedFieldFragments) {
            List<PsiExpression> initializerThisExpressions = expressionExtractor.getThisExpressions(fieldFragment.getInitializer());
            PsiField extractedFieldDeclaration = null;
            if(initializerThisExpressions.isEmpty()) {
                PsiField staticFieldInitializer = staticFieldInitializer(fieldFragment);
                if(staticFieldInitializer != null && !variableBindingInExtractedFields(staticFieldInitializer)) {
                    PsiField fragment = factory.createField(fieldFragment.getName(), );

                    PsiClass staticFieldDeclaringClassTypeBinding = staticFieldInitializer.getContainingClass();
                     qualifiedName = extractedClassAST.newQualifiedName(extractedClassAST.newSimpleName(staticFieldDeclaringClassTypeBinding.getName()), extractedClassAST.newSimpleName(staticFieldInitializer.getName()));
                    extractedClassRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, qualifiedName, null);
                    extractedFieldDeclaration = extractedClassAST.newFieldDeclaration(fragment);
                }
                else {
                    extractedFieldDeclaration = extractedClassAST.newFieldDeclaration((VariableDeclarationFragment)ASTNode.copySubtree(extractedClassAST, fieldFragment));
                }
            }
            else {
                this.extractedFieldsWithThisExpressionInTheirInitializer.add(fieldFragment);
                VariableDeclarationFragment fragment = extractedClassAST.newVariableDeclarationFragment();
                extractedClassRewriter.set(fragment, VariableDeclarationFragment.NAME_PROPERTY, extractedClassAST.newSimpleName(fieldFragment.getName().getIdentifier()), null);
                extractedFieldDeclaration = extractedClassAST.newFieldDeclaration(fragment);
            }
            FieldDeclaration originalFieldDeclaration = (FieldDeclaration)fieldFragment.getParent();
            extractedClassRewriter.set(extractedFieldDeclaration, FieldDeclaration.TYPE_PROPERTY, originalFieldDeclaration.getType(), null);
            ListRewrite extractedFieldDeclarationModifiersRewrite = extractedClassRewriter.getListRewrite(extractedFieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
            extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD), null);
            List<IExtendedModifier> originalModifiers = originalFieldDeclaration.modifiers();
            for(IExtendedModifier extendedModifier : originalModifiers) {
                if(extendedModifier.isModifier()) {
                    Modifier modifier = (Modifier)extendedModifier;
                    if(modifier.isFinal()) {
                        extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                        finalFieldFragments.add(fieldFragment);
                        if(fieldFragment.getInitializer() == null)
                            finalFieldFragmentsWithoutInitializer.add(fieldFragment);
                    }
                    else if(modifier.isStatic()) {
                        extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD), null);
                    }
                    else if(modifier.isTransient()) {
                        extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD), null);
                    }
                    else if(modifier.isVolatile()) {
                        extractedFieldDeclarationModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD), null);
                    }
                }
            }
            extractedClassBodyRewrite.insertLast(extractedFieldDeclaration, null);
        }
        for(MethodDeclaration constructor : constructorFinalFieldAssignmentMap.keySet()) {
            Map<VariableDeclaration, Assignment> finalFieldAssignmentMap = constructorFinalFieldAssignmentMap.get(constructor);
            MethodDeclaration extractedClassConstructor = extractedClassAST.newMethodDeclaration();
            extractedClassConstructor.setConstructor(true);
            extractedClassRewriter.set(extractedClassConstructor, MethodDeclaration.NAME_PROPERTY, extractedClassName, null);
            ListRewrite extractedClassConstructorModifiersRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.MODIFIERS2_PROPERTY);
            extractedClassConstructorModifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);

            Block extractedClassConstructorBody = extractedClassAST.newBlock();
            extractedClassRewriter.set(extractedClassConstructor, MethodDeclaration.BODY_PROPERTY, extractedClassConstructorBody, null);
            ListRewrite extractedClassConstructorBodyStatementsRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructorBody, Block.STATEMENTS_PROPERTY);
            ListRewrite extractedClassConstructorParametersRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.PARAMETERS_PROPERTY);
            Set<VariableDeclaration> extractedClassConstructorParameters = extractedClassConstructorParameterMap.get(constructor);
            for(VariableDeclaration variableDeclaration : extractedClassConstructorParameters) {
                Type parameterType = null;
                if(variableDeclaration instanceof SingleVariableDeclaration) {
                    parameterType = ((SingleVariableDeclaration)variableDeclaration).getType();
                }
                else if(variableDeclaration instanceof VariableDeclarationFragment) {
                    VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)variableDeclaration.getParent();
                    parameterType = variableDeclarationStatement.getType();
                }
                SingleVariableDeclaration constructorParameter = extractedClassAST.newSingleVariableDeclaration();
                extractedClassRewriter.set(constructorParameter, SingleVariableDeclaration.TYPE_PROPERTY, parameterType, null);
                extractedClassRewriter.set(constructorParameter, SingleVariableDeclaration.NAME_PROPERTY, variableDeclaration.getName(), null);
                extractedClassConstructorParametersRewrite.insertLast(constructorParameter, null);
            }
            Set<ITypeBinding> thrownExceptionTypeBindings = new LinkedHashSet<ITypeBinding>();
            for(VariableDeclaration fieldFragment : finalFieldAssignmentMap.keySet()) {
                Assignment fieldAssignment = finalFieldAssignmentMap.get(fieldFragment);
                Assignment newFieldAssignment = extractedClassAST.newAssignment();
                extractedClassRewriter.set(newFieldAssignment, Assignment.LEFT_HAND_SIDE_PROPERTY, fieldAssignment.getLeftHandSide(), null);
                extractedClassRewriter.set(newFieldAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, fieldAssignment.getRightHandSide(), null);
                extractedClassRewriter.set(newFieldAssignment, Assignment.OPERATOR_PROPERTY, fieldAssignment.getOperator(), null);
                ExpressionStatement assignmentStatement = extractedClassAST.newExpressionStatement(newFieldAssignment);
                extractedClassConstructorBodyStatementsRewrite.insertLast(assignmentStatement, null);
                ThrownExceptionVisitor thrownExceptionVisitor = new ThrownExceptionVisitor();
                fieldAssignment.getRightHandSide().accept(thrownExceptionVisitor);
                thrownExceptionTypeBindings.addAll(thrownExceptionVisitor.getTypeBindings());
            }
            RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
            ListRewrite constructorThrownExceptionsRewrite = extractedClassRewriter.getListRewrite(extractedClassConstructor, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
            for(ITypeBinding thrownExceptionTypeBinding : thrownExceptionTypeBindings) {
                constructorThrownExceptionsRewrite.insertLast(RefactoringUtility.generateTypeFromTypeBinding(thrownExceptionTypeBinding, extractedClassAST, extractedClassRewriter), null);
            }
            extractedClassBodyRewrite.insertLast(extractedClassConstructor, null);
        }
        for(VariableDeclaration fieldFragment : extractedFieldFragments) {
            if(accessedFieldsInNonExtractedMethods.contains(fieldFragment)) {
                MethodDeclaration getterMethodDeclaration = createGetterMethodDeclaration(fieldFragment, extractedClassAST, extractedClassRewriter);
                extractedClassBodyRewrite.insertLast(getterMethodDeclaration, null);
            }
            if(modifiedFieldsInNonExtractedMethods.contains(fieldFragment) && !finalFieldFragments.contains(fieldFragment)) {
                MethodDeclaration setterMethodDeclaration = createSetterMethodDeclaration(fieldFragment, extractedClassAST, extractedClassRewriter);
                extractedClassBodyRewrite.insertLast(setterMethodDeclaration, null);
            }
        }
        for(MethodDeclaration method : extractedMethods) {
            MethodDeclaration extractedMethodDeclaration = createExtractedMethodDeclaration(method, extractedClassAST, extractedClassRewriter);
            extractedClassBodyRewrite.insertLast(extractedMethodDeclaration, null);
        }
        Map<MethodDeclaration, Integer> levelMap = new LinkedHashMap<MethodDeclaration, Integer>();
        //create adjacency list
        AdjacencyList adjacencyList = new AdjacencyList();
        for(MethodDeclaration method : extractedMethods) {
            if(oldMethodInvocationsWithinExtractedMethods.containsKey(method)) {
                levelMap.put(method, -1);
                for(MethodInvocation methodInvocation : oldMethodInvocationsWithinExtractedMethods.get(method)) {
                    //exclude recursive invocations
                    if(!method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                        Node source = new Node(method.resolveBinding().getKey());
                        Node target = new Node(methodInvocation.resolveMethodBinding().getKey());
                        adjacencyList.addEdge(source, target, 0);
                    }
                }
            }
            else
                levelMap.put(method, 0);
        }
        TarjanAlgorithm tarjan = new TarjanAlgorithm(adjacencyList);
        while(!allExtractedMethodsObtainedLevel(levelMap)) {
            for(MethodDeclaration method : extractedMethods) {
                if(levelMap.get(method) == -1) {
                    Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(method);
                    int maxLevel = 0;
                    boolean dependsOnMethodWithoutLevel = false;
                    for(MethodInvocation methodInvocation : methodInvocations) {
                        //exclude recursive invocations
                        if(!method.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                            MethodDeclaration invokedMethod = getExtractedMethod(methodInvocation.resolveMethodBinding());
                            int invokedMethodLevel = levelMap.get(invokedMethod);
                            if(invokedMethodLevel == -1) {
                                boolean belongToTheSameStronglyConnectedComponent = tarjan.belongToTheSameStronglyConnectedComponent(
                                        method.resolveBinding().getKey(), methodInvocation.resolveMethodBinding().getKey());
                                if(belongToTheSameStronglyConnectedComponent) {
                                    double sourceAverageLevelOfTargets = getAverageLevelOfTargets(method.resolveBinding().getKey(), levelMap, adjacencyList);
                                    double targetAverageLevelOfTargets = getAverageLevelOfTargets(methodInvocation.resolveMethodBinding().getKey(), levelMap, adjacencyList);
                                    if(sourceAverageLevelOfTargets > targetAverageLevelOfTargets) {
                                        dependsOnMethodWithoutLevel = true;
                                        break;
                                    }
                                }
                                else {
                                    dependsOnMethodWithoutLevel = true;
                                    break;
                                }
                            }
                            else {
                                if(invokedMethodLevel > maxLevel)
                                    maxLevel = invokedMethodLevel;
                            }
                        }
                    }
                    if(!dependsOnMethodWithoutLevel) {
                        levelMap.put(method, maxLevel + 1);
                    }
                }
            }
        }
        Set<MethodDeclaration> sortedMethods = new LinkedHashSet<MethodDeclaration>();
        int min = 0;
        while(!levelMap.isEmpty()) {
            for(MethodDeclaration method : extractedMethods) {
                if(levelMap.containsKey(method)) {
                    int level = levelMap.get(method);
                    if(level == min) {
                        levelMap.remove(method);
                        if(level > 0)
                            sortedMethods.add(method);
                    }
                }
            }
            min++;
        }
        for(MethodDeclaration oldMethod : sortedMethods) {
            Map<PlainVariable, SingleVariableDeclaration> fieldParameterMap = new LinkedHashMap<PlainVariable, SingleVariableDeclaration>();
            Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
            SingleVariableDeclaration sourceClassParameter = null;
            boolean sourceClassParameterShouldBeFinal = false;

            Set<MethodInvocation> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
            MethodDeclaration newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
            List<MethodInvocation> newMethodInvocations = new ArrayList<MethodInvocation>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
            Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
            Set<SingleVariableDeclaration> additionalParametersForInvokerMethod = additionalParametersAddedToExtractedMethods.get(oldMethod);
            int i = 0;
            for(MethodInvocation oldMethodInvocation : oldMethodInvocations) {
                if(oldMethodInvocation.getExpression() == null || oldMethodInvocation.getExpression() instanceof ThisExpression) {
                    //invocation without expression
                    if(!oldMethod.resolveBinding().isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
                        //non-recursive
                        MethodDeclaration oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethodBinding());
                        Set<PlainVariable> additionalArgumentsForExtractedInvokedMethod = additionalArgumentsAddedToExtractedMethods.get(oldExtractedInvokedMethod);
                        for(PlainVariable additionalArgument : additionalArgumentsForExtractedInvokedMethod) {
                            if(isParentAnonymousClassDeclaration(oldMethodInvocation)) {
                                if(isThisVariable(additionalArgument))
                                    sourceClassParameterShouldBeFinal = true;
                                else
                                    fieldParameterFinalMap.put(additionalArgument, true);
                            }
                            if(!additionalArgumentsForInvokerMethod.contains(additionalArgument)) {
                                if(isThisVariable(additionalArgument)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethod, extractedClassRewriter);
                                    addThisVariable(additionalArgumentsForInvokerMethod);
                                    additionalParametersForInvokerMethod.add(sourceClassParameter);
                                }
                                else {
                                    SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethod, additionalArgument, extractedClassRewriter);
                                    additionalArgumentsForInvokerMethod.add(additionalArgument);
                                    additionalParametersForInvokerMethod.add(fieldParameter);
                                    fieldParameterMap.put(additionalArgument, fieldParameter);
                                }
                            }
                            MethodInvocation newMethodInvocation = newMethodInvocations.get(i);
                            ListRewrite argumentsRewrite = extractedClassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                            if(isThisVariable(additionalArgument)) {
                                String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
                                String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
                                SimpleName parameterName = extractedClassAST.newSimpleName(modifiedSourceTypeName);
                                argumentsRewrite.insertLast(parameterName, null);
                            }
                            else {
                                if(additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getVariableName());
                                    argumentsRewrite.insertLast(extractedClassAST.newSimpleName(parameterName), null);
                                }
                                else {
                                    argumentsRewrite.insertLast(extractedClassAST.newSimpleName(additionalArgument.getVariableName()), null);
                                }
                            }
                        }
                    }
                }
                else {
                    //invocation with expression
                    MethodDeclaration oldExtractedInvokedMethod = getExtractedMethod(oldMethodInvocation.resolveMethodBinding());
                    delegateMethods.add(oldExtractedInvokedMethod);
                }
                i++;
            }
            if(sourceClassParameterShouldBeFinal) {
                if(sourceClassParameter != null) {
                    ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(sourceClassParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                    modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                }
                else {
                    int j = 0;
                    List<SingleVariableDeclaration> additionalParametersForInvokerMethodList = new ArrayList<SingleVariableDeclaration>(additionalParametersForInvokerMethod);
                    SingleVariableDeclaration additionalParameter = null;
                    for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                        if(isThisVariable(additionalArgument)) {
                            additionalParameter = additionalParametersForInvokerMethodList.get(j);
                            break;
                        }
                        j++;
                    }
                    ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(additionalParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                    modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                }
            }
            for(PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
                if(fieldParameterFinalMap.get(fieldName) == true) {
                    SingleVariableDeclaration fieldParameter = fieldParameterMap.get(fieldName);
                    if(fieldParameter != null) {
                        ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(fieldParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                        modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                    }
                    else {
                        int j = 0;
                        List<SingleVariableDeclaration> additionalParametersForInvokerMethodList = new ArrayList<SingleVariableDeclaration>(additionalParametersForInvokerMethod);
                        SingleVariableDeclaration additionalParameter = null;
                        for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            if(additionalArgument.equals(fieldName)) {
                                additionalParameter = additionalParametersForInvokerMethodList.get(j);
                                break;
                            }
                            j++;
                        }
                        ListRewrite modifiersRewrite = extractedClassRewriter.getListRewrite(additionalParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                        modifiersRewrite.insertLast(extractedClassAST.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                    }
                }
            }
        }
        //handle recursive extracted method invocations
        for(MethodDeclaration oldMethod : oldMethodInvocationsWithinExtractedMethods.keySet()) {
            Set<MethodInvocation> oldMethodInvocations = oldMethodInvocationsWithinExtractedMethods.get(oldMethod);
            MethodDeclaration newMethod = oldToNewExtractedMethodDeclarationMap.get(oldMethod);
            List<MethodInvocation> newMethodInvocations = new ArrayList<MethodInvocation>(newMethodInvocationsWithinExtractedMethods.get(newMethod));
            Set<PlainVariable> additionalArgumentsForInvokerMethod = additionalArgumentsAddedToExtractedMethods.get(oldMethod);
            int i = 0;
            for(MethodInvocation oldMethodInvocation : oldMethodInvocations) {
                if(oldMethodInvocation.getExpression() == null || oldMethodInvocation.getExpression() instanceof ThisExpression) {
                    //invocation without expression
                    if(oldMethod.resolveBinding().isEqualTo(oldMethodInvocation.resolveMethodBinding())) {
                        //recursive invocation
                        for(PlainVariable additionalArgument : additionalArgumentsForInvokerMethod) {
                            MethodInvocation newMethodInvocation = newMethodInvocations.get(i);
                            ListRewrite argumentsRewrite = extractedClassRewriter.getListRewrite(newMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                            if(isThisVariable(additionalArgument)) {
                                String sourceTypeName = sourceTypeDeclaration.getName().getIdentifier();
                                String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());
                                SimpleName parameterName = extractedClassAST.newSimpleName(modifiedSourceTypeName);
                                argumentsRewrite.insertLast(parameterName, null);
                            }
                            else {
                                if(additionalArgument.isField()) {
                                    //adding "this" prefix to avoid collisions with other parameter names
                                    String parameterName = createNameForParameterizedFieldAccess(additionalArgument.getVariableName());
                                    argumentsRewrite.insertLast(extractedClassAST.newSimpleName(parameterName), null);
                                }
                                else {
                                    argumentsRewrite.insertLast(extractedClassAST.newSimpleName(additionalArgument.getVariableName()), null);
                                }
                            }
                        }
                    }
                }
                i++;
            }
        }
        MethodDeclaration cloneMethod = createCloneMethod(extractedClassRewriter, extractedClassAST);
        if(cloneMethod != null) {
            extractedClassBodyRewrite.insertLast(cloneMethod, null);
            ITypeBinding cloneableInterfaceTypeBinding = implementsCloneableInterface(sourceTypeDeclaration.resolveBinding());
            if(cloneableInterfaceTypeBinding != null) {
                Type cloneableInterfaceType = RefactoringUtility.generateTypeFromTypeBinding(cloneableInterfaceTypeBinding, extractedClassAST, extractedClassRewriter);
                extractedClassImplementedInterfacesRewrite.insertLast(cloneableInterfaceType, null);
                Set<ITypeBinding> typeBindings = new LinkedHashSet<ITypeBinding>();
                typeBindings.add(cloneableInterfaceTypeBinding);
                RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass);
            }
            else {
                SimpleType cloneableType = extractedClassAST.newSimpleType(extractedClassAST.newSimpleName("Cloneable"));
                extractedClassImplementedInterfacesRewrite.insertLast(cloneableType, null);
            }
        }
        extractedClassTypesRewrite.insertLast(extractedClassTypeDeclaration, null);

        try {
            for(ITypeBinding typeBinding : requiredImportDeclarationsInExtractedClass) {
                addImportDeclaration(typeBinding, extractedClassCompilationUnit, extractedClassRewriter);
            }
            for(IMethodBinding methodBinding : staticallyImportedMethods) {
                addStaticImportDeclaration(methodBinding, extractedClassCompilationUnit, extractedClassRewriter);
            }
            TextEdit extractedClassEdit = extractedClassRewriter.rewriteAST(extractedClassDocument, null);
            extractedClassEdit.apply(extractedClassDocument);
            CreateCompilationUnitChange createCompilationUnitChange =
                    new CreateCompilationUnitChange(extractedClassICompilationUnit, extractedClassDocument.get(), extractedClassFile.getCharset());
            createCompilationUnitChanges.put(extractedClassICompilationUnit, createCompilationUnitChange);
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (MalformedTreeException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private PsiMethod createCloneMethod() {
        // check if source class contains clone method
        PsiMethod cloneMethodBinding = null; //TODO
        PsiMethod[] methodDeclarations = sourceTypeDeclaration.getMethods();
        for(PsiMethod method : methodDeclarations) {
            if(isClone(method)) {
                cloneMethodBinding = method;
                break;
            }
        }
        if(cloneMethodBinding == null) {
            cloneMethodBinding = findCloneMethod(sourceTypeDeclaration.getSuperClass());
        }
        if(cloneMethodBinding != null) {
            PsiMethod cloneMethodDeclaration = factory.createMethod(cloneMethodBinding.getName(), RefactoringUtility.generateTypeFromTypeBinding(cloneMethodBinding.getReturnType());//TODO
            cloneMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

            PsiReferenceList thrownExceptionTypes = cloneMethodBinding.getThrowsList();
            Set<PsiReference> thrownExceptionTypeBindings = new LinkedHashSet<>();
            for(PsiReference typeBinding : thrownExceptionTypes.getReferenceElements()) {
                PsiReference type = RefactoringUtility.generateTypeFromTypeBinding(typeBinding.resolve());
                cloneMethodBinding.getThrowsList().add(type.resolve()); //TODO
                thrownExceptionTypeBindings.add(typeBinding);
            }
            RefactoringUtility.getSimpleTypeBindings(thrownExceptionTypeBindings, requiredImportDeclarationsInExtractedClass);
            PsiCodeBlock body = cloneMethodBinding.getBody();

            PsiReturnStatement returnStatement; //TODO
            PsiMethodCallExpression superCloneMethodInvocation; //TODO
            cloneMethodBinding.getName(); //TODO это имя у superCloneMethodInvocation

            CastExpression castExpression = extractedClassAST.newCastExpression();
            extractedClassRewriter.set(castExpression, CastExpression.EXPRESSION_PROPERTY, superCloneMethodInvocation, null);
            extractedClassRewriter.set(castExpression, CastExpression.TYPE_PROPERTY, extractedClassAST.newSimpleName(extractedTypeName), null);
            extractedClassRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, castExpression, null);

            StringBuilder returnStatementText = new StringBuilder();

            if(thrownExceptionTypes.getReferenceElements().length == 0) {
                PsiTryStatement tryStatement; //TODO new try statement
                PsiCodeBlock tryBody; //TODO new codeblock
                returnStatementText.append(
                        "try {\n"
                        + returnStatement.getText() + "\n" + //TODO тут надо понять что писать
                        + "} catch (CloneNotSupportedException e) {\n"
                        + "throw new InternalError();\n"
                        + "}");
            }
            else {
                returnStatementText.append() //TODO
            }
            return cloneMethodDeclaration;
        }
        return null;
    }

    private double getAverageLevelOfTargets(String methodBindingKey, Map<PsiMethod, Integer> levelMap, AdjacencyList adjacency) {
        Node n = new Node(methodBindingKey);
        LinkedHashSet<Edge> edges = adjacency.getAdjacent(n);
        int levelSum = 0;
        int targetSum = 0;
        for(Edge edge : edges) {
            Node target = edge.getTarget();
            for(PsiMethod methodDeclaration : levelMap.keySet()) {
                int level = levelMap.get(methodDeclaration);
                if(methodDeclaration.getName().equals(target.getName())) { //TODO is just getName ok?
                    if(level != -1) {
                        levelSum += level;
                        targetSum++;
                    }
                    break;
                }
            }
        }
        if(targetSum == 0)
            return Double.MAX_VALUE;
        else
            return (double)levelSum/(double)targetSum;
    }

    private boolean allExtractedMethodsObtainedLevel(Map<PsiMethod, Integer> levelMap) {
        for(PsiMethod method : levelMap.keySet()) {
            if(levelMap.get(method) == -1)
                return false;
        }
        return true;
    }

    private PsiMethod getExtractedMethod(PsiMethod methodBinding) {
        for(PsiMethod extractedMethod : extractedMethods) {
            if(extractedMethod.isEquivalentTo(methodBinding)) //equivalent or equal?
                return extractedMethod;
        }
        return null;
    }

    private PsiMethod createExtractedMethodDeclaration(PsiMethod extractedMethod) {
        PsiMethod newMethodDeclaration = (PsiMethod) extractedMethod.copy();

        newMethodDeclaration.setName(extractedMethod.getName());

        newMethodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        for(PsiAnnotation annotation : newMethodDeclaration.getAnnotations()) {
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
        for(PsiParameter originalParameter : originalParameters.getParameters()) {
            PsiType originalParameterTypeBinding = originalParameter.getType();
            for(PsiClass typeBinding : requiredImportDeclarationsInExtractedClass) {
                if(!originalParameterTypeBinding.equals(typeBinding)) {
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
        for(PsiField extractedFieldFragment : extractedFieldFragments) {
            if(psiManager.areElementsEquivalent(extractedFieldFragment, variableBinding)) //TODO replacce all isEquavialentTo with this
                return true;
        }
        return false;
    }

    private boolean methodBindingCorrespondsToExtractedMethod(PsiMethod methodBinding) {
        for(PsiMethod extractedMethod : extractedMethods) {
            if(psiManager.areElementsEquivalent(extractedMethod, methodBinding))
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
        if(typeDeclaration != null) {
            PsiMethod[] contextMethods = typeDeclaration.getMethods();
            for (PsiMethod methodDeclaration : contextMethods) {
                PsiElement simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
                if(simpleName != null && psiManager.areElementsEquivalent(simpleName, fieldBinding)) {
                    return methodDeclaration;
                }
            }
        }
        return null;
    }

    private PsiMethod findGetterMethodInSourceClass(PsiField fieldBinding) {
        PsiClass typeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(fieldBinding, sourceTypeDeclaration);
        if(typeDeclaration != null) {
            PsiMethod[] contextMethods = typeDeclaration.getMethods();
            for(PsiMethod methodDeclaration : contextMethods) {
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
        for(PsiField fieldDeclaration : fieldDeclarations) {
            if(psiManager.areElementsEquivalent(variableBinding, fieldDeclaration)) {
                String methodName = fieldDeclaration.getName();
                methodName = SETTER_PREFIX + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
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
                methodName = GETTER_PREFIX + methodName.substring(0,1).toUpperCase() + methodName.substring(1,methodName.length());
                methodName = appendAccessorMethodSuffix(methodName, sourceTypeDeclaration.getMethods());

                PsiMethod psiMethod = (PsiMethod) factory.createStatementFromText("public " + fieldDeclaration.getType().getPresentableText() + " " + methodName + "() {\n"
                + "return " + fieldDeclaration.getName() + ";\n"
                + "}", sourceTypeDeclaration);
                //TODO добавить метод
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
        List<PsiExpression> newFieldInstructions = new ArrayList<>();
        List<PsiExpression> newAssignments = new ArrayList<>();
        Arrays.stream(newMethodDeclaration.getBody().getStatements()).forEach(statement -> {
            newMethodInvocations.addAll(expressionExtractor.getMethodInvocations(statement));
            newFieldInstructions.addAll(expressionExtractor.getVariableInstructions(statement));
            newAssignments.addAll(expressionExtractor.getAssignments(statement));
        });

        PsiVariable sourceClassParameter = null;
        boolean sourceClassParameterShouldBeFinal = false;
        Map<PlainVariable, PsiField> fieldParameterMap = new LinkedHashMap<>();
        Map<PlainVariable, Boolean> fieldParameterFinalMap = new LinkedHashMap<PlainVariable, Boolean>();
        String sourceTypeName = sourceTypeDeclaration.getName();
        String modifiedSourceTypeName = sourceTypeName.substring(0,1).toLowerCase() + sourceTypeName.substring(1,sourceTypeName.length());

        int i = 0;
        for(PsiExpression expression : sourceAssignments) {
            PsiAssignmentExpression oldAssignment = (PsiAssignmentExpression) expression;
            PsiAssignmentExpression newAssignment = (PsiAssignmentExpression) newAssignments.get(i);
            PsiExpression oldLeftHandSide = oldAssignment.getLExpression();
            PsiExpression newLeftHandSide = newAssignment.getLExpression();

            PsiField oldAssignedVariable = null;
            PsiField newAssignedVariable = null; //TODO field?
            if(oldLeftHandSide instanceof PsiReferenceExpression) {
                oldAssignedVariable = (PsiField) ((PsiReferenceExpression) oldLeftHandSide).resolve();
                newAssignedVariable = (SimpleName)newLeftHandSide;
            }
            else if(oldLeftHandSide instanceof FieldAccess) {
                FieldAccess oldFieldAccess = (FieldAccess)oldLeftHandSide;
                oldAssignedVariable = oldFieldAccess.getName();
                FieldAccess newFieldAccess = (FieldAccess)newLeftHandSide;
                newAssignedVariable = newFieldAccess.getName();
            }
            Expression oldRightHandSide = oldAssignment.getRightHandSide();
            Expression newRightHandSide = newAssignment.getRightHandSide();
            if(oldAssignedVariable != null) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if(binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding)binding;
                    if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if(isParentAnonymousClassDeclaration(oldAssignment))
                                    sourceClassParameterShouldBeFinal = true;
                                if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if(setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                }
                                else {
                                    if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                if(!newAssignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
                                    IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                    MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                    if(getterMethodBinding != null) {
                                        targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                    }
                                    else {
                                        if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                            createGetterMethodInSourceClass(variableBinding);
                                            sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                        }
                                        String originalFieldName = variableBinding.getName();
                                        String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                        String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                        getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                        targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                    }
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                    InfixExpression infixExpression = ast.newInfixExpression();
                                    targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                    targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, newAssignment.getRightHandSide(), null);
                                    if(newAssignment.getOperator().equals(Assignment.Operator.PLUS_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.MINUS_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.TIMES_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.DIVIDE_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.DIVIDE, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.REMAINDER_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.REMAINDER, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_AND_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.AND, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_OR_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.OR, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.XOR, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.LEFT_SHIFT, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_SIGNED, null);
                                    }
                                    else if(newAssignment.getOperator().equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN)) {
                                        targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, null);
                                    }
                                    setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                }
                                else {
                                    setterMethodInvocationArgumentsRewrite.insertLast(newAssignment.getRightHandSide(), null);
                                }
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newAssignment, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            }
            else {
                //if an assigned field is not found in left hand side, then replace all accessed fields in left hand side
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldLeftHandSide);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newLeftHandSide);
                for(Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName)expression2;
                    SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
                        if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if(isParentAnonymousClassDeclaration(oldAssignment))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                    else {
                                        if(isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
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
            int j = 0;
            List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldRightHandSide);
            List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newRightHandSide);
            for(Expression expression2 : oldAccessedVariables) {
                SimpleName oldAccessedVariable = (SimpleName)expression2;
                SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
                IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
                    if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                            if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                    if(isParentAnonymousClassDeclaration(oldAssignment))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            newAccessedVariable,
                                            accessedVariableBinding);
                                }
                                else {
                                    if(isParentAnonymousClassDeclaration(oldAccessedVariable))
                                        fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast, fieldParameterMap,
                                            newAccessedVariable, accessedVariableBinding);
                                }
                            }
                        }
                    }
                }
                j++;
            }
            i++;
        }

        List<Expression> sourcePostfixExpressions = expressionExtractor.getPostfixExpressions(sourceMethod.getBody());
        List<Expression> newPostfixExpressions = expressionExtractor.getPostfixExpressions(newMethodDeclaration.getBody());
        i = 0;
        for(Expression expression : sourcePostfixExpressions) {
            PostfixExpression oldPostfixExpression = (PostfixExpression)expression;
            PostfixExpression newPostfixExpression = (PostfixExpression)newPostfixExpressions.get(i);
            Expression oldOperand = oldPostfixExpression.getOperand();
            Expression newOperand = newPostfixExpression.getOperand();
            SimpleName oldAssignedVariable = null;
            SimpleName newAssignedVariable = null;
            if(oldOperand instanceof SimpleName) {
                oldAssignedVariable = (SimpleName)oldOperand;
                newAssignedVariable = (SimpleName)newOperand;
            }
            else if(oldOperand instanceof FieldAccess) {
                FieldAccess oldFieldAccess = (FieldAccess)oldOperand;
                oldAssignedVariable = oldFieldAccess.getName();
                FieldAccess newFieldAccess = (FieldAccess)newOperand;
                newAssignedVariable = newFieldAccess.getName();
            }
            if(oldAssignedVariable != null) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if(binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding)binding;
                    if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if(isParentAnonymousClassDeclaration(oldPostfixExpression))
                                    sourceClassParameterShouldBeFinal = true;
                                if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if(setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                }
                                else {
                                    if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                if(getterMethodBinding != null) {
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                }
                                else {
                                    if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                }
                                targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                InfixExpression infixExpression = ast.newInfixExpression();
                                targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
                                if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.INCREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                }
                                else if(newPostfixExpression.getOperator().equals(PostfixExpression.Operator.DECREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                }
                                setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newPostfixExpression, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            }
            else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for(Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName)expression2;
                    SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
                        if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if(isParentAnonymousClassDeclaration(oldPostfixExpression))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                    else {
                                        if(isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
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

        List<Expression> sourcePrefixExpressions = expressionExtractor.getPrefixExpressions(sourceMethod.getBody());
        List<Expression> newPrefixExpressions = expressionExtractor.getPrefixExpressions(newMethodDeclaration.getBody());
        i = 0;
        for(Expression expression : sourcePrefixExpressions) {
            PrefixExpression oldPrefixExpression = (PrefixExpression)expression;
            PrefixExpression newPrefixExpression = (PrefixExpression)newPrefixExpressions.get(i);
            Expression oldOperand = oldPrefixExpression.getOperand();
            Expression newOperand = newPrefixExpression.getOperand();
            Operator oldOperator = oldPrefixExpression.getOperator();
            Operator newOperator = newPrefixExpression.getOperator();
            SimpleName oldAssignedVariable = null;
            SimpleName newAssignedVariable = null;
            if(oldOperand instanceof SimpleName) {
                oldAssignedVariable = (SimpleName)oldOperand;
                newAssignedVariable = (SimpleName)newOperand;
            }
            else if(oldOperand instanceof FieldAccess) {
                FieldAccess oldFieldAccess = (FieldAccess)oldOperand;
                oldAssignedVariable = oldFieldAccess.getName();
                FieldAccess newFieldAccess = (FieldAccess)newOperand;
                newAssignedVariable = newFieldAccess.getName();
            }
            if(oldAssignedVariable != null && (oldOperator.equals(PrefixExpression.Operator.INCREMENT) ||
                    oldOperator.equals(PrefixExpression.Operator.DECREMENT))) {
                IBinding binding = oldAssignedVariable.resolveBinding();
                if(binding != null && binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding variableBinding = (IVariableBinding)binding;
                    if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                        if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                            if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
                                IMethodBinding setterMethodBinding = findSetterMethodInSourceClass(variableBinding);
                                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                if(isParentAnonymousClassDeclaration(oldPrefixExpression))
                                    sourceClassParameterShouldBeFinal = true;
                                if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                }
                                MethodInvocation setterMethodInvocation = ast.newMethodInvocation();
                                if(setterMethodBinding != null) {
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodBinding.getName()), null);
                                }
                                else {
                                    if(!sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                        createSetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedSetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                    String setterMethodName = SETTER_PREFIX + modifiedFieldName;
                                    setterMethodName = appendAccessorMethodSuffix(setterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(setterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(setterMethodName), null);
                                }
                                ListRewrite setterMethodInvocationArgumentsRewrite = targetRewriter.getListRewrite(setterMethodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                                IMethodBinding getterMethodBinding = findGetterMethodInSourceClass(variableBinding);
                                MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
                                if(getterMethodBinding != null) {
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodBinding.getName()), null);
                                }
                                else {
                                    if(!sourceFieldBindingsWithCreatedGetterMethod.contains(variableBinding.getKey())) {
                                        createGetterMethodInSourceClass(variableBinding);
                                        sourceFieldBindingsWithCreatedGetterMethod.add(variableBinding.getKey());
                                    }
                                    String originalFieldName = variableBinding.getName();
                                    String modifiedFieldName = originalFieldName.substring(0,1).toUpperCase() + originalFieldName.substring(1,originalFieldName.length());
                                    String getterMethodName = GETTER_PREFIX + modifiedFieldName;
                                    getterMethodName = appendAccessorMethodSuffix(getterMethodName, sourceTypeDeclaration.getMethods());
                                    targetRewriter.set(getterMethodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(getterMethodName), null);
                                }
                                targetRewriter.set(getterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                InfixExpression infixExpression = ast.newInfixExpression();
                                targetRewriter.set(infixExpression, InfixExpression.LEFT_OPERAND_PROPERTY, getterMethodInvocation, null);
                                targetRewriter.set(infixExpression, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNumberLiteral("1"), null);
                                if(newOperator.equals(PrefixExpression.Operator.INCREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.PLUS, null);
                                }
                                else if(newOperator.equals(PrefixExpression.Operator.DECREMENT)) {
                                    targetRewriter.set(infixExpression, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
                                }
                                setterMethodInvocationArgumentsRewrite.insertLast(infixExpression, null);
                                targetRewriter.set(setterMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                                targetRewriter.replace(newPrefixExpression, setterMethodInvocation, null);
                            }
                        }
                    }
                }
            }
            else {
                //if an assigned field is not found in operand, then replace all accessed fields in operand
                int j = 0;
                List<Expression> oldAccessedVariables = expressionExtractor.getVariableInstructions(oldOperand);
                List<Expression> newAccessedVariables = expressionExtractor.getVariableInstructions(newOperand);
                for(Expression expression2 : oldAccessedVariables) {
                    SimpleName oldAccessedVariable = (SimpleName)expression2;
                    SimpleName newAccessedVariable = (SimpleName)newAccessedVariables.get(j);
                    IBinding rightHandBinding = oldAccessedVariable.resolveBinding();
                    if(rightHandBinding != null && rightHandBinding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding accessedVariableBinding = (IVariableBinding)rightHandBinding;
                        if(accessedVariableBinding.isField() && (accessedVariableBinding.getModifiers() & Modifier.STATIC) == 0) {
                            if(declaredInSourceTypeDeclarationOrSuperclass(accessedVariableBinding)) {
                                if(!variableBindingCorrespondsToExtractedField(accessedVariableBinding)) {
                                    if(sourceFieldBindingsWithCreatedSetterMethod.contains(accessedVariableBinding.getKey())) {
                                        if(isParentAnonymousClassDeclaration(oldPrefixExpression))
                                            sourceClassParameterShouldBeFinal = true;
                                        sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter, ast,
                                                sourceClassParameter,
                                                modifiedSourceTypeName,
                                                newAccessedVariable,
                                                accessedVariableBinding);
                                    }
                                    else {
                                        if(isParentAnonymousClassDeclaration(oldAccessedVariable))
                                            fieldParameterFinalMap.put(new PlainVariable(accessedVariableBinding), true);
                                        handleAccessedFieldNotHavingSetterMethod(
                                                sourceMethod,
                                                newMethodDeclaration,
                                                targetRewriter,
                                                ast, fieldParameterMap,
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
        for(Expression expression : sourceFieldInstructions) {
            SimpleName simpleName = (SimpleName)expression;
            IBinding binding = simpleName.resolveBinding();
            if(binding != null && binding.getKind() == IBinding.VARIABLE) {
                IVariableBinding variableBinding = (IVariableBinding)binding;
                if(variableBinding.isField() && (variableBinding.getModifiers() & Modifier.STATIC) == 0) {
                    if(declaredInSourceTypeDeclarationOrSuperclass(variableBinding)) {
                        if(!variableBindingCorrespondsToExtractedField(variableBinding)) {
                            if(!isAssignmentChild(expression)) {
                                SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
                                if(sourceFieldBindingsWithCreatedSetterMethod.contains(variableBinding.getKey())) {
                                    if(isParentAnonymousClassDeclaration(simpleName))
                                        sourceClassParameterShouldBeFinal = true;
                                    sourceClassParameter = handleAccessedFieldHavingSetterMethod(
                                            sourceMethod,
                                            newMethodDeclaration,
                                            targetRewriter, ast,
                                            sourceClassParameter,
                                            modifiedSourceTypeName,
                                            expressionName,
                                            variableBinding);
                                }
                                else {
                                    if(isParentAnonymousClassDeclaration(simpleName))
                                        fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                    handleAccessedFieldNotHavingSetterMethod(
                                            sourceMethod, newMethodDeclaration,
                                            targetRewriter, ast, fieldParameterMap,
                                            expressionName, variableBinding);
                                }
                            }
                        }
                    }
                    else {
                        Type superclassType = sourceTypeDeclaration.getSuperclassType();
                        ITypeBinding superclassTypeBinding = null;
                        if(superclassType != null)
                            superclassTypeBinding = superclassType.resolveBinding();
                        while(superclassTypeBinding != null && !superclassTypeBinding.isEqualTo(variableBinding.getDeclaringClass())) {
                            superclassTypeBinding = superclassTypeBinding.getSuperclass();
                        }
                        if(superclassTypeBinding != null) {
                            IVariableBinding[] superclassFieldBindings = superclassTypeBinding.getDeclaredFields();
                            for(IVariableBinding superclassFieldBinding : superclassFieldBindings) {
                                if(superclassFieldBinding.isEqualTo(variableBinding)) {
                                    if(!isAssignmentChild(expression)) {
                                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                        Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                        SimpleName expressionName = (SimpleName)newFieldInstructions.get(i);
                                        if(isParentAnonymousClassDeclaration(simpleName))
                                            fieldParameterFinalMap.put(new PlainVariable(variableBinding), true);
                                        if(!containsVariable(variableBinding, additionalArgumentsAddedToMovedMethod)) {
                                            SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, variableBinding, targetRewriter);
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
        for(Expression expression : sourceMethodInvocations) {
            if(expression instanceof MethodInvocation) {
                MethodInvocation methodInvocation = (MethodInvocation)expression;
                if(methodInvocation.getExpression() == null || methodInvocation.getExpression() instanceof ThisExpression) {
                    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                    if(methodBinding.getDeclaringClass().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
                        MethodDeclaration[] sourceMethodDeclarations = sourceTypeDeclaration.getMethods();
                        for(MethodDeclaration sourceMethodDeclaration : sourceMethodDeclarations) {
                            if(sourceMethodDeclaration.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                                if(!methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding()) &&
                                        !sourceMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
                                    SimpleName fieldName = MethodDeclarationUtility.isGetter(sourceMethodDeclaration);
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    int modifiers = sourceMethodDeclaration.getModifiers();
                                    MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
                                    if((modifiers & Modifier.STATIC) != 0) {
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier()), null);
                                        if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                                        }
                                    }
                                    else if(fieldName != null) {
                                        IVariableBinding fieldBinding = (IVariableBinding)fieldName.resolveBinding();
                                        if(!variableBindingCorrespondsToExtractedField(fieldBinding)) {
                                            if((fieldBinding.getModifiers() & Modifier.STATIC) != 0) {
                                                SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
                                                QualifiedName qualifiedName = ast.newQualifiedName(qualifier, ast.newSimpleName(fieldName.getIdentifier()));
                                                targetRewriter.replace(newMethodInvocation, qualifiedName, null);
                                                setPublicModifierToSourceField(fieldBinding);
                                            }
                                            else {
                                                String parameterNameString = createNameForParameterizedFieldAccess(fieldName.getIdentifier());
                                                targetRewriter.replace(newMethodInvocation, ast.newSimpleName(parameterNameString), null);
                                                //targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
                                                if(isParentAnonymousClassDeclaration(methodInvocation))
                                                    fieldParameterFinalMap.put(new PlainVariable(fieldBinding), true);
                                                if(!containsVariable(fieldBinding, additionalArgumentsAddedToMovedMethod)) {
                                                    SingleVariableDeclaration fieldParameter = addParameterToMovedMethod(newMethodDeclaration, fieldBinding, targetRewriter);
                                                    addVariable(fieldBinding, additionalArgumentsAddedToMovedMethod);
                                                    additionalParametersAddedToMovedMethod.add(fieldParameter);
                                                    fieldParameterMap.put(new PlainVariable(fieldBinding), fieldParameter);
                                                }
                                            }
                                        }
                                        else {
                                            targetRewriter.replace(newMethodInvocation, ast.newSimpleName(fieldName.getIdentifier()), null);
                                        }
                                    }
                                    else {
                                        if(isParentAnonymousClassDeclaration(methodInvocation))
                                            sourceClassParameterShouldBeFinal = true;
                                        if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
                                        if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                                        }
                                    }
                                }
                                else {
                                    if(!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                                        methodInvocations.add(methodInvocation);
                                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                                    }
                                    else {
                                        Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                                        methodInvocations.add(methodInvocation);
                                    }
                                    if(!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                                        methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
                                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                                    }
                                    else {
                                        Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                                        methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
                                    }
                                }
                            }
                        }
                    }
                    else {
                        Type superclassType = sourceTypeDeclaration.getSuperclassType();
                        ITypeBinding superclassTypeBinding = null;
                        if(superclassType != null)
                            superclassTypeBinding = superclassType.resolveBinding();
                        while(superclassTypeBinding != null && !methodBinding.getDeclaringClass().isEqualTo(superclassTypeBinding)) {
                            superclassTypeBinding = superclassTypeBinding.getSuperclass();
                        }
                        if(superclassTypeBinding != null) {
                            IMethodBinding[] superclassMethodBindings = superclassTypeBinding.getDeclaredMethods();
                            for(IMethodBinding superclassMethodBinding : superclassMethodBindings) {
                                if(superclassMethodBinding.isEqualTo(methodBinding)) {
                                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                                    MethodInvocation newMethodInvocation = (MethodInvocation)newMethodInvocations.get(j);
                                    if((superclassMethodBinding.getModifiers() & Modifier.STATIC) != 0) {
                                        SimpleName qualifier = ast.newSimpleName(sourceTypeDeclaration.getName().getIdentifier());
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, qualifier, null);
                                    }
                                    else {
                                        if(isParentAnonymousClassDeclaration(methodInvocation))
                                            sourceClassParameterShouldBeFinal = true;
                                        if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                                        }
                                        targetRewriter.set(newMethodInvocation, MethodInvocation.EXPRESSION_PROPERTY, parameterName, null);
                                        if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodBinding.getKey())) {
                                            TypeDeclaration superclassTypeDeclaration = RefactoringUtility.findDeclaringTypeDeclaration(superclassMethodBinding, sourceTypeDeclaration);
                                            if(superclassTypeDeclaration != null) {
                                                setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), superclassTypeDeclaration);
                                            }
                                            sourceMethodBindingsChangedWithPublicModifier.add(methodBinding.getKey());
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            //check if it is a statically imported method
                            List<ImportDeclaration> sourceImportDeclarations = sourceCompilationUnit.imports();
                            for(ImportDeclaration importDeclaration : sourceImportDeclarations) {
                                if(importDeclaration.isStatic()) {
                                    IBinding binding = importDeclaration.resolveBinding();
                                    //A single-static-import declaration imports all accessible static members with a given simple name from a type.
                                    //binding.isEqualTo(methodBinding) will not work when the static import actually imports multiple overloaded methods
                                    if(binding != null && binding.getKind() == IBinding.METHOD) {
                                        IMethodBinding importedMethodBinding = (IMethodBinding)binding;
                                        if(importedMethodBinding.getName().equals(methodBinding.getName()) &&
                                                importedMethodBinding.getDeclaringClass().getQualifiedName().equals(methodBinding.getDeclaringClass().getQualifiedName())) {
                                            this.staticallyImportedMethods.add(importedMethodBinding);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if(methodBindingCorrespondsToExtractedMethod(methodInvocation.resolveMethodBinding())) {
                    if(!oldMethodInvocationsWithinExtractedMethods.containsKey(sourceMethod)) {
                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                        methodInvocations.add(methodInvocation);
                        oldMethodInvocationsWithinExtractedMethods.put(sourceMethod, methodInvocations);
                    }
                    else {
                        Set<MethodInvocation> methodInvocations = oldMethodInvocationsWithinExtractedMethods.get(sourceMethod);
                        methodInvocations.add(methodInvocation);
                    }
                    if(!newMethodInvocationsWithinExtractedMethods.containsKey(newMethodDeclaration)) {
                        LinkedHashSet<MethodInvocation> methodInvocations = new LinkedHashSet<MethodInvocation>();
                        methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
                        newMethodInvocationsWithinExtractedMethods.put(newMethodDeclaration, methodInvocations);
                    }
                    else {
                        Set<MethodInvocation> methodInvocations = newMethodInvocationsWithinExtractedMethods.get(newMethodDeclaration);
                        methodInvocations.add((MethodInvocation)newMethodInvocations.get(j));
                    }
                    if(methodInvocation.getExpression() != null && methodInvocation.getExpression().resolveTypeBinding().isEqualTo(sourceTypeDeclaration.resolveBinding())) {
                        if(!sourceMethodBindingsChangedWithPublicModifier.contains(methodInvocation.resolveMethodBinding().getKey())) {
                            setPublicModifierToSourceMethod(methodInvocation.resolveMethodBinding(), sourceTypeDeclaration);
                            sourceMethodBindingsChangedWithPublicModifier.add(methodInvocation.resolveMethodBinding().getKey());
                        }
                    }
                }
            }
            j++;
        }
        //replaceThisExpressionWithSourceClassParameterInMethodInvocationArguments
        int k=0;
        for(Expression invocation : newMethodInvocations) {
            if(invocation instanceof MethodInvocation) {
                MethodInvocation methodInvocation = (MethodInvocation)invocation;
                List<Expression> arguments = methodInvocation.arguments();
                for(Expression argument : arguments) {
                    if(argument instanceof ThisExpression) {
                        if(isParentAnonymousClassDeclaration(sourceMethodInvocations.get(k)))
                            sourceClassParameterShouldBeFinal = true;
                        Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                        Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                        if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                            sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                            addThisVariable(additionalArgumentsAddedToMovedMethod);
                            additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                        }
                        ListRewrite argumentRewrite = targetRewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
                        argumentRewrite.replace(argument, parameterName, null);
                    }
                }
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInClassInstanceCreationArguments
        List<Expression> sourceClassInstanceCreations = expressionExtractor.getClassInstanceCreations(sourceMethod.getBody());
        List<Expression> newClassInstanceCreations = expressionExtractor.getClassInstanceCreations(newMethodDeclaration.getBody());
        k = 0;
        for(Expression creation : newClassInstanceCreations) {
            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)creation;
            List<Expression> arguments = classInstanceCreation.arguments();
            for(Expression argument : arguments) {
                if(argument instanceof ThisExpression) {
                    if(isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
                        sourceClassParameterShouldBeFinal = true;
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    ListRewrite argumentRewrite = targetRewriter.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
                    argumentRewrite.replace(argument, parameterName, null);
                }
            }
            ClassInstanceCreation oldClassInstanceCreation = (ClassInstanceCreation)sourceClassInstanceCreations.get(k);
            ITypeBinding classInstanceCreationTypeBinding = oldClassInstanceCreation.resolveTypeBinding();
            if(classInstanceCreationTypeBinding.isNested() && oldClassInstanceCreation.getAnonymousClassDeclaration() == null &&
                    sourceTypeDeclaration.resolveBinding().isEqualTo(classInstanceCreationTypeBinding.getDeclaringClass())) {
                if(isParentAnonymousClassDeclaration(sourceClassInstanceCreations.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                if((classInstanceCreationTypeBinding.getModifiers() & Modifier.STATIC) != 0) {
                    Type qualifierType = ast.newSimpleType(ast.newSimpleName(sourceTypeDeclaration.resolveBinding().getName()));
                    QualifiedType qualifiedType = ast.newQualifiedType(qualifierType, ast.newSimpleName(classInstanceCreationTypeBinding.getName()));
                    targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY, qualifiedType, null);
                    requiredImportDeclarationsInExtractedClass.add(classInstanceCreationTypeBinding);
                    setPublicModifierToSourceMemberType(classInstanceCreationTypeBinding);
                }
                else {
                    Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                    Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                    if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                        sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                        addThisVariable(additionalArgumentsAddedToMovedMethod);
                        additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                    }
                    targetRewriter.set(classInstanceCreation, ClassInstanceCreation.EXPRESSION_PROPERTY, ast.newSimpleName(modifiedSourceTypeName), null);
                    Type oldClassInstanceCreationType = oldClassInstanceCreation.getType();
                    SimpleName simpleNameType = null;
                    if(oldClassInstanceCreationType instanceof QualifiedType) {
                        QualifiedType qualifiedType = (QualifiedType)oldClassInstanceCreationType;
                        simpleNameType = qualifiedType.getName();
                    }
                    else if(oldClassInstanceCreationType instanceof SimpleType) {
                        SimpleType simpleType = (SimpleType)oldClassInstanceCreationType;
                        if(simpleType.getName() instanceof QualifiedName) {
                            QualifiedName qualifiedName = (QualifiedName)simpleType.getName();
                            simpleNameType = qualifiedName.getName();
                        }
                    }
                    if(simpleNameType != null) {
                        targetRewriter.set(classInstanceCreation, ClassInstanceCreation.TYPE_PROPERTY,
                                ast.newSimpleType(ast.newSimpleName(simpleNameType.getIdentifier())), null);
                    }
                }
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInVariableDeclarationInitializers
        StatementExtractor statementExtractor = new StatementExtractor();
        List<VariableDeclarationFragment> sourceVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
        List<VariableDeclarationFragment> newVariableDeclarationFragments = new ArrayList<VariableDeclarationFragment>();
        List<Statement> sourceVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(sourceMethod.getBody());
        for(Statement statement : sourceVariableDeclarationStatements) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
            List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
            sourceVariableDeclarationFragments.addAll(fragments);
        }
        List<Statement> newVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(newMethodDeclaration.getBody());
        for(Statement statement : newVariableDeclarationStatements) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
            List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
            newVariableDeclarationFragments.addAll(fragments);
        }
        List<Expression> sourceVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(sourceMethod.getBody());
        for(Expression expression : sourceVariableDeclarationExpressions) {
            VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
            List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
            sourceVariableDeclarationFragments.addAll(fragments);
        }
        List<Expression> newVariableDeclarationExpressions = expressionExtractor.getVariableDeclarationExpressions(newMethodDeclaration.getBody());
        for(Expression expression : newVariableDeclarationExpressions) {
            VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
            List<VariableDeclarationFragment> fragments = variableDeclarationExpression.fragments();
            newVariableDeclarationFragments.addAll(fragments);
        }
        k = 0;
        for(VariableDeclarationFragment fragment : newVariableDeclarationFragments) {
            Expression initializer = fragment.getInitializer();
            if(initializer instanceof ThisExpression) {
                if(isParentAnonymousClassDeclaration(sourceVariableDeclarationFragments.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                targetRewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, parameterName, null);
            }
            k++;
        }
        //replaceThisExpressionWithSourceClassParameterInReturnStatementExpressions
        List<Statement> sourceReturnStatements = statementExtractor.getReturnStatements(sourceMethod.getBody());
        List<Statement> newReturnStatements = statementExtractor.getReturnStatements(newMethodDeclaration.getBody());
        k = 0;
        for(Statement statement : newReturnStatements) {
            ReturnStatement newReturnStatement = (ReturnStatement)statement;
            if(newReturnStatement.getExpression() instanceof ThisExpression) {
                if(isParentAnonymousClassDeclaration(sourceReturnStatements.get(k)))
                    sourceClassParameterShouldBeFinal = true;
                Set<PlainVariable> additionalArgumentsAddedToMovedMethod = additionalArgumentsAddedToExtractedMethods.get(sourceMethod);
                Set<SingleVariableDeclaration> additionalParametersAddedToMovedMethod = additionalParametersAddedToExtractedMethods.get(sourceMethod);
                if(!containsThisVariable(additionalArgumentsAddedToMovedMethod)) {
                    sourceClassParameter = addSourceClassParameterToMovedMethod(newMethodDeclaration, targetRewriter);
                    addThisVariable(additionalArgumentsAddedToMovedMethod);
                    additionalParametersAddedToMovedMethod.add(sourceClassParameter);
                }
                targetRewriter.set(newReturnStatement, ReturnStatement.EXPRESSION_PROPERTY, parameterName, null);
            }
            k++;
        }
        if(sourceClassParameter != null && sourceClassParameterShouldBeFinal) {
            ListRewrite modifiersRewrite = targetRewriter.getListRewrite(sourceClassParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
            modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
        }
        for(PlainVariable fieldName : fieldParameterFinalMap.keySet()) {
            if(fieldParameterFinalMap.get(fieldName) == true) {
                SingleVariableDeclaration fieldParameter = fieldParameterMap.get(fieldName);
                if(fieldParameter != null) {
                    ListRewrite modifiersRewrite = targetRewriter.getListRewrite(fieldParameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                    modifiersRewrite.insertLast(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD), null);
                }
            }
        }
    }
}