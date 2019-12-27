package refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.*;
import core.ast.inheritance.InheritanceTree;
import core.ast.util.ExpressionExtractor;
import core.ast.util.TypeVisitor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("restriction")
public class ReplaceTypeCodeWithStateStrategy extends PolymorphismRefactoring {
    private PsiVariable returnedVariable;
    private Set<PsiType> requiredImportDeclarationsBasedOnSignature;
    private Set<PsiType> requiredImportDeclarationsForContext;
    private Set<PsiClassType> thrownExceptions;
    private Map<PsiField, String> staticFieldMap;
    private Map<PsiField, String> additionalStaticFieldMap;
    private String abstractClassName;

    public ReplaceTypeCodeWithStateStrategy(PsiFile sourceFile,
                                            Project project,
                                            PsiClass sourceTypeDeclaration,
                                            TypeCheckElimination typeCheckElimination) {
        super(sourceFile, project, sourceTypeDeclaration, typeCheckElimination);
        this.returnedVariable = typeCheckElimination.getTypeCheckMethodReturnedVariable();
        this.requiredImportDeclarationsBasedOnSignature = new LinkedHashSet<>();
        this.requiredImportDeclarationsForContext = new LinkedHashSet<>();
        this.thrownExceptions = typeCheckElimination.getThrownExceptions();
        this.staticFieldMap = new LinkedHashMap<>();
        for (PsiField simpleName : typeCheckElimination.getStaticFields()) {
            this.staticFieldMap.put(simpleName, generateSubclassName(simpleName));
        }
        this.additionalStaticFieldMap = new LinkedHashMap<>();
        for (PsiField simpleName : typeCheckElimination.getAdditionalStaticFields()) {
            this.additionalStaticFieldMap.put(simpleName, generateSubclassName(simpleName));
        }
        this.abstractClassName = typeCheckElimination.getAbstractClassName();
        checkInitialConditions();
    }

    @Override
    public void apply() {
        if (typeCheckElimination.getTypeField() != null) {
            modifyTypeFieldAssignmentsInContextClass(true);
            modifyTypeFieldAccessesInContextClass(true);
        }
        createStateStrategyHierarchy();
        if (typeCheckElimination.getTypeField() != null) {
            modifyContext();
        } else if (typeCheckElimination.getTypeLocalVariable() != null || typeCheckElimination.getTypeMethodInvocation() != null) {
            modifyTypeCheckMethod();
        }
    }

    private void checkInitialConditions() {
        if (typeCheckElimination.getTypeField() != null) {
            modifyTypeFieldAssignmentsInContextClass(false);
            modifyTypeFieldAccessesInContextClass(false);
        } else if (typeCheckElimination.getTypeLocalVariable() != null) {
            identifyTypeLocalVariableAssignmentsInTypeCheckMethod();
            identifyTypeLocalVariableAccessesInTypeCheckMethod();
        }
    }

    private void modifyContext() {
        PsiField fragment = typeCheckElimination.getTypeField();
        fragment.normalizeDeclaration();
        generateSetterMethodForStateField();
        generateGetterMethodForStateField();
        replaceConditionalStructureWithPolymorphicMethodInvocation();
        replacePrimitiveStateField();

        generateGettersForAccessedFields();
        generateSettersForAssignedFields();
        setPublicModifierToStaticFields();
        setPublicModifierToAccessedMethods();

        addRequiredImportDeclarationsToContext();
    }

    private PsiField createStateFieldVariableDeclarationFragment(String type) {
        PsiField typeFragment = elementFactory.createFieldFromText(
                type + " " + typeCheckElimination.getTypeField().getName() + ";",
                null
        );
        PsiExpression typeFieldInitializer = typeCheckElimination.getTypeField().getInitializer();
        Set<PsiField> allStaticFieldNames = new LinkedHashSet<>(typeCheckElimination.getStaticFields());
        allStaticFieldNames.addAll(additionalStaticFieldMap.keySet());

        if (typeFieldInitializer != null) {
            PsiReferenceExpression typeFieldInitializerReference = null;
            if (typeFieldInitializer instanceof PsiReferenceExpression) {
                typeFieldInitializerReference = (PsiReferenceExpression) typeFieldInitializer;
            } else if (typeFieldInitializer instanceof PsiLiteralExpression) {
                PsiLiteralExpression typeFieldInitializerLiteral = (PsiLiteralExpression) typeFieldInitializer;
                for (PsiField staticFieldName : allStaticFieldNames) {
                    Object constantValue = staticFieldName.computeConstantValue();
                    if (constantValue instanceof Integer) {
                        Integer constantIntegerValue = (Integer) constantValue;
                        if (constantIntegerValue.toString().equals(typeFieldInitializerLiteral.getText())) {
                            String subclassName = getTypeNameForNamedConstant(staticFieldName);
                            typeFragment.setInitializer(elementFactory.createExpressionFromText(
                                    "new " + subclassName + "()",
                                    null
                            ));
                            break;
                        }
                    }
                }
            }

            if (typeFieldInitializerReference != null) {
                for (PsiField staticFieldName : allStaticFieldNames) {
                    if (staticFieldName.equals(typeFieldInitializerReference.resolve())) {
                        String subclassName = getTypeNameForNamedConstant(staticFieldName);
                        typeFragment.setInitializer(elementFactory.createExpressionFromText(
                                "new " + subclassName + "()",
                                null
                        ));
                        break;
                    }
                }
            }
        } else {
            for (PsiField staticFieldName : allStaticFieldNames) {
                Object constantValue = staticFieldName.computeConstantValue();
                if (constantValue instanceof Integer) {
                    Integer constantIntegerValue = (Integer) constantValue;
                    if (constantIntegerValue.toString().equals("0")) {
                        String subclassName = getTypeNameForNamedConstant(staticFieldName);
                        typeFragment.setInitializer(elementFactory.createExpressionFromText(
                                "new " + subclassName + "()",
                                null
                        ));
                        break;
                    }
                }
            }
        }
        return typeFragment;
    }

    private void replacePrimitiveStateField() {
        PsiField typeFragment = createStateFieldVariableDeclarationFragment(abstractClassName);
        typeCheckElimination.getTypeField().replace(typeFragment);
    }

    private void generateSetterMethodForStateField() { // TODO: fix enum constants
        PsiMethod setterMethod = typeCheckElimination.getTypeFieldSetterMethod();
        List<PsiField> staticFieldNames = new ArrayList<>(staticFieldMap.keySet());
        List<String> subclassNames = new ArrayList<>(staticFieldMap.values());
        PsiSwitchStatement switchStatement = (PsiSwitchStatement) elementFactory.createStatementFromText(
                "switch(a) {}",
                null
        );
        PsiCodeBlock switchStatementStatementsRewrite = switchStatement.getBody();
        int i = 0;
        for (PsiField staticFieldName : staticFieldNames) {
            PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) elementFactory.createStatementFromText(
                    "case a:",
                    null
            );
            String staticFieldNameDeclaringClass = null;

            boolean isEnumConstant = staticFieldName instanceof PsiEnumConstant;
            if (!sourceTypeDeclaration.equals(staticFieldName.getContainingClass())) {
                staticFieldNameDeclaringClass = staticFieldName.getContainingClass().getName();
            }

            PsiExpression caseExpression = switchCase.getCaseValues().getExpressions()[0];
            if (staticFieldNameDeclaringClass == null || isEnumConstant) {
                caseExpression.replace(elementFactory.createExpressionFromText(
                        staticFieldName.getName(),
                        null
                ));
            } else {
                caseExpression.replace(elementFactory.createExpressionFromText(
                        staticFieldNameDeclaringClass + "." + staticFieldName.getName(),
                        null
                ));
            }

            switchStatementStatementsRewrite.add(switchCase);
            PsiStatement assignmentStatement = elementFactory.createStatementFromText(
                    "this." + typeCheckElimination.getTypeField().getName() + " = " + "new " + subclassNames.get(i) + "();",
                    null
            );
            switchStatementStatementsRewrite.add(assignmentStatement);
            switchStatementStatementsRewrite.add(elementFactory.createStatementFromText("break;", null));
            i++;
        }

        for (PsiField staticFieldName : additionalStaticFieldMap.keySet()) {
            PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) elementFactory.createStatementFromText(
                    "case a:",
                    null
            );
            String staticFieldNameDeclaringClass = null;

            boolean isEnumConstant = staticFieldName instanceof PsiEnumConstant;
            if (!sourceTypeDeclaration.equals(staticFieldName.getContainingClass())) {
                staticFieldNameDeclaringClass = staticFieldName.getContainingClass().getName();
            }

            PsiExpression caseExpression = switchCase.getCaseValues().getExpressions()[0];
            if (staticFieldNameDeclaringClass == null || isEnumConstant) {
                caseExpression.replace(elementFactory.createExpressionFromText(
                        staticFieldName.getName(),
                        null
                ));
            } else {
                caseExpression.replace(elementFactory.createExpressionFromText(
                        staticFieldNameDeclaringClass + "." + staticFieldName.getName(),
                        null
                ));
            }

            switchStatementStatementsRewrite.add(switchCase);
            PsiStatement assignmentStatement = elementFactory.createStatementFromText(
                    "this." + typeCheckElimination.getTypeField().getName() + " = " + "new " + additionalStaticFieldMap.get(staticFieldName) + "();",
                    null
            );
            switchStatementStatementsRewrite.add(assignmentStatement);
            switchStatementStatementsRewrite.add(elementFactory.createStatementFromText("break;", null));
        }

        PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) elementFactory.createStatementFromText(
                "default:",
                null
        );
        switchStatementStatementsRewrite.add(switchCase);
        PsiStatement assignmentStatement = elementFactory.createStatementFromText(
                "this." + typeCheckElimination.getTypeField().getName() + " = " + "null;",
                null
        );
        switchStatementStatementsRewrite.add(assignmentStatement);
        switchStatementStatementsRewrite.add(elementFactory.createStatementFromText("break;", null));

        if (setterMethod != null) {
            PsiParameterList setterMethodParameters = setterMethod.getParameterList();
            if (setterMethodParameters.getParametersCount() == 1) {
                switchStatement.getExpression().replace(elementFactory.createExpressionFromText(
                        setterMethodParameters.getParameters()[0].getName(),
                        null
                ));
            }
            PsiCodeBlock setterMethodBody = setterMethod.getBody();
            PsiStatement[] setterMethodBodyStatements = setterMethodBody.getStatements();
            if (setterMethodBodyStatements.length == 1) {
                setterMethodBodyStatements[0].replace(switchStatement);
            }
        } else {
            PsiMethod setterMethodDeclaration = elementFactory.createMethod(setterMethodName(), PsiType.VOID);
            PsiUtil.setModifierProperty(setterMethodDeclaration, PsiModifier.PUBLIC, true);
            PsiParameterList setterMethodParameterRewrite = setterMethodDeclaration.getParameterList();
            PsiField typeField = typeCheckElimination.getTypeField();
            PsiParameter parameter = elementFactory.createParameter(typeField.getName(), typeField.getType());
            setterMethodParameterRewrite.add(parameter);

            switchStatement.getExpression().replace(elementFactory.createExpressionFromText(
                    typeField.getName(),
                    null
            ));
            PsiCodeBlock setterMethodBody = setterMethodDeclaration.getBody();
            setterMethodBody.add(switchStatement);
            sourceTypeDeclaration.add(setterMethodDeclaration);
        }
    }

    private String setterMethodName() {
        String defaultName = "set" + abstractClassName;
        for (PsiMethod method : sourceTypeDeclaration.getMethods()) {
            if (method.getName().equals(defaultName))
                return defaultName + "2";
        }
        return defaultName;
    }

    private void generateGetterMethodForStateField() {
        PsiMethod getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
        if (getterMethod != null) {
            PsiCodeBlock getterMethodBody = getterMethod.getBody();
            PsiStatement[] getterMethodBodyStatements = getterMethodBody.getStatements();
            if (getterMethodBodyStatements.length == 1) {
                PsiStatement returnStatement = elementFactory.createStatementFromText(
                        "return " + typeCheckElimination.getTypeField().getName() + "." + getterMethod.getName() + "();",
                        null
                );
                getterMethodBodyStatements[0].replace(returnStatement);
            }
        } else {
            PsiMethod getterMethodDeclaration = elementFactory.createMethod(
                    "get" + abstractClassName,
                    typeCheckElimination.getTypeField().getType()
            );
            PsiUtil.setModifierProperty(getterMethodDeclaration, PsiModifier.PUBLIC, true);
            PsiStatement returnStatement = elementFactory.createStatementFromText(
                    "return " + typeCheckElimination.getTypeField().getName() + ".get" + abstractClassName + "();",
                    null
            );
            getterMethodDeclaration.getBody().add(returnStatement);
            sourceTypeDeclaration.add(getterMethodDeclaration);
        }
    }

    private void replaceConditionalStructureWithPolymorphicMethodInvocation() {
        if (returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
            PsiMethodCallExpression abstractMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    typeCheckElimination.getTypeField().getName() + "." + typeCheckElimination.getAbstractMethodName() + "()",
                    null
            );
            PsiExpressionList methodInvocationArgumentsRewrite = abstractMethodInvocation.getArgumentList();
            for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                if (!abstractMethodParameter.equals(returnedVariable)) {
                    PsiExpression argument = elementFactory.createExpressionFromText(abstractMethodParameter.getName(), null);
                    methodInvocationArgumentsRewrite.add(argument);
                }
            }
            for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                if (!fragment.equals(returnedVariable)) {
                    PsiExpression argument = elementFactory.createExpressionFromText(fragment.getName(), null);
                    methodInvocationArgumentsRewrite.add(argument);
                }
            }
            if (sourceTypeRequiredForExtraction()) {
                methodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText("this", null));
            }
            PsiStatement expressionStatement = elementFactory.createStatementFromText(
                    abstractMethodInvocation.getText() + ";",
                    null
            );
            typeCheckElimination.getTypeCheckCodeFragment().replace(expressionStatement);
        } else {
            PsiMethodCallExpression abstractMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    typeCheckElimination.getTypeField().getName() + "." + typeCheckElimination.getAbstractMethodName() + "();",
                    null
            );
            PsiExpressionList methodInvocationArgumentsRewrite = abstractMethodInvocation.getArgumentList();
            if (returnedVariable != null) {
                PsiExpression argument = elementFactory.createExpressionFromText(returnedVariable.getName(), null);
                methodInvocationArgumentsRewrite.add(argument);
            }
            for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                if (!abstractMethodParameter.equals(returnedVariable)) {
                    PsiExpression argument = elementFactory.createExpressionFromText(abstractMethodParameter.getName(), null);
                    methodInvocationArgumentsRewrite.add(argument);
                }
            }
            for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                if (!fragment.equals(returnedVariable)) {
                    PsiExpression argument = elementFactory.createExpressionFromText(fragment.getName(), null);
                    methodInvocationArgumentsRewrite.add(argument);
                }
            }
            if (sourceTypeRequiredForExtraction()) {
                methodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText("this", null));
            }
            if (returnedVariable != null) {
                PsiStatement expressionStatement = elementFactory.createStatementFromText(
                        returnedVariable.getName() + " = " + abstractMethodInvocation.getText() + ";",
                        null
                );
                typeCheckElimination.getTypeCheckCodeFragment().replace(expressionStatement);
            } else {
                PsiStatement expressionStatement = elementFactory.createStatementFromText(
                        "return " + abstractMethodInvocation.getText() + ";",
                        null
                );
                typeCheckElimination.getTypeCheckCodeFragment().replace(expressionStatement);
            }
        }
    }

    //
//	private void initializeReturnedVariableDeclaration() { // TODO: do we really need it???
//		ASTRewrite sourceRewriter = ASTRewrite.create(sourceTypeDeclaration.getAST());
//		AST contextAST = sourceTypeDeclaration.getAST();
//		if(returnedVariable != null) {
//			IVariableBinding returnedVariableBinding = returnedVariable.resolveBinding();
//			if(returnedVariable instanceof VariableDeclarationFragment && !returnedVariableBinding.isField()) {
//				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)returnedVariable;
//				if(variableDeclarationFragment.getInitializer() == null) {
//					Expression defaultValue = generateDefaultValue(sourceRewriter, contextAST, returnedVariableBinding.getType());
//					sourceRewriter.set(variableDeclarationFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, defaultValue, null);
//				}
//			}
//		}
//		try {
//			TextEdit sourceEdit = sourceRewriter.rewriteAST();
//			ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
//			CompilationUnitChange change = compilationUnitChanges.get(sourceICompilationUnit);
//			change.getEdit().addChild(sourceEdit);
//			change.addTextEditGroup(new TextEditGroup("Initialize returned variable", new TextEdit[] {sourceEdit}));
//		} catch (JavaModelException e) {
//			e.printStackTrace();
//		}
//	}
//
    private void addRequiredImportDeclarationsToContext() {
        addImports(getPsiImportList(sourceFile), requiredImportDeclarationsForContext);
    }

    private void modifyTypeCheckMethod() {
        createGetterMethodForStateObject();
        replaceConditionalStructureWithPolymorphicMethodInvocationThroughStateObject();

        generateGettersForAccessedFields();
        generateSettersForAssignedFields();
        setPublicModifierToStaticFields();
        setPublicModifierToAccessedMethods();

        addRequiredImportDeclarationsToContext();
    }

    private void createGetterMethodForStateObject() {
        if (!typeObjectGetterMethodAlreadyExists()) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) elementFactory.createStatementFromText(
                    "switch(a) {}",
                    null
            );
            PsiCodeBlock switchStatementStatementsRewrite = switchStatement.getBody();
            List<PsiField> staticFieldNames = new ArrayList<>(staticFieldMap.keySet());
            List<String> subclassNames = new ArrayList<>(staticFieldMap.values());
            int i = 0;
            for (PsiField staticFieldName : staticFieldNames) {
                PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) elementFactory.createStatementFromText(
                        "case a:",
                        null
                );
                String staticFieldNameDeclaringClass = null;
                boolean isEnumConstant = staticFieldName instanceof PsiEnumConstant;
                if (!sourceTypeDeclaration.equals(staticFieldName.getContainingClass())) {
                    staticFieldNameDeclaringClass = staticFieldName.getContainingClass().getName();
                }

                PsiExpression caseExpression = switchCase.getCaseValues().getExpressions()[0];
                if (staticFieldNameDeclaringClass == null || isEnumConstant) {
                    caseExpression.replace(elementFactory.createExpressionFromText(
                            staticFieldName.getName(),
                            null
                    ));
                } else {
                    caseExpression.replace(elementFactory.createExpressionFromText(
                            staticFieldNameDeclaringClass + "." + staticFieldName.getName(),
                            null
                    ));
                }

                switchStatementStatementsRewrite.add(switchCase);
                PsiStatement returnStatement = elementFactory.createStatementFromText(
                        "return new " + subclassNames.get(i) + "();",
                        null
                );
                switchStatementStatementsRewrite.add(returnStatement);
                i++;
            }

            for (PsiField staticFieldName : additionalStaticFieldMap.keySet()) {
                PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) elementFactory.createStatementFromText(
                        "case a:",
                        null
                );
                String staticFieldNameDeclaringClass = null;
                boolean isEnumConstant = staticFieldName instanceof PsiEnumConstant;
                if (!sourceTypeDeclaration.equals(staticFieldName.getContainingClass())) {
                    staticFieldNameDeclaringClass = staticFieldName.getContainingClass().getName();
                }

                PsiExpression caseExpression = switchCase.getCaseValues().getExpressions()[0];
                if (staticFieldNameDeclaringClass == null) {
                    caseExpression.replace(elementFactory.createExpressionFromText(
                            staticFieldName.getName(),
                            null
                    ));
                } else {
                    caseExpression.replace(elementFactory.createExpressionFromText(
                            staticFieldNameDeclaringClass + "." + staticFieldName.getName(),
                            null
                    ));
                }
                switchStatementStatementsRewrite.add(switchCase);
                PsiStatement returnStatement = elementFactory.createStatementFromText(
                        "return new " + additionalStaticFieldMap.get(staticFieldName) + "();",
                        null
                );
                switchStatementStatementsRewrite.add(returnStatement);
            }

            String methodName = "get" + abstractClassName + "Object";
            PsiType returnType = elementFactory.createTypeFromText(abstractClassName, null);
            PsiMethod getterMethodDeclaration = elementFactory.createMethod(methodName, returnType);
            PsiUtil.setModifierProperty(getterMethodDeclaration, PsiModifier.PRIVATE, true);

            if (typeCheckElimination.getTypeCheckMethod().getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                PsiUtil.setModifierProperty(getterMethodDeclaration, PsiModifier.STATIC, true);
            }

            PsiParameterList getterMethodParameterRewrite = getterMethodDeclaration.getParameterList();
            PsiType parameterType = null;
            String parameterName = null;
            if (typeCheckElimination.getTypeLocalVariable() != null) {
                PsiVariable typeLocalVariable = typeCheckElimination.getTypeLocalVariable();
                parameterName = typeLocalVariable.getName();
                parameterType = typeLocalVariable.getType();
            } else if (typeCheckElimination.getForeignTypeField() != null) {
                PsiField foreignTypeField = typeCheckElimination.getForeignTypeField();
                parameterType = foreignTypeField.getType();
                parameterName = foreignTypeField.getName();
            }

            PsiParameter parameter = elementFactory.createParameter(parameterName, parameterType);
            getterMethodParameterRewrite.add(parameter);

            switchStatement.getExpression().replace(elementFactory.createExpressionFromText(
                    parameterName,
                    null
            ));

            PsiCodeBlock getterMethodBody = getterMethodDeclaration.getBody();
            getterMethodBody.add(switchStatement);
            PsiStatement defaultReturnStatement = elementFactory.createStatementFromText(
                    "return null;",
                    null
            );
            getterMethodBody.add(defaultReturnStatement);
            sourceTypeDeclaration.add(getterMethodDeclaration);
        }
    }

    private void replaceConditionalStructureWithPolymorphicMethodInvocationThroughStateObject() {
        if (returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
            PsiMethodCallExpression abstractMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    typeCheckElimination.getAbstractMethodName() + "()",
                    null
            );
            PsiMethodCallExpression invokerMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    "get" + abstractClassName + "Object()",
                    null
            );
            PsiExpressionList invokerMethodInvocationArgumentsRewrite = invokerMethodInvocation.getArgumentList();
            if (typeCheckElimination.getTypeLocalVariable() != null) {
                invokerMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                        typeCheckElimination.getTypeLocalVariable().getName(),
                        null
                ));
            } else if (typeCheckElimination.getTypeMethodInvocation() != null) {
                invokerMethodInvocationArgumentsRewrite.add(typeCheckElimination.getTypeMethodInvocation());
            }

            abstractMethodInvocation.getMethodExpression().setQualifierExpression(invokerMethodInvocation);
            PsiExpressionList abstractMethodInvocationArgumentsRewrite = abstractMethodInvocation.getArgumentList();
            for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                if (!abstractMethodParameter.equals(returnedVariable)) {
                    abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                            abstractMethodParameter.getName(),
                            null
                    ));
                }
            }
            for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                if (!fragment.equals(returnedVariable)) {
                    abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                            fragment.getName(),
                            null
                    ));
                }
            }
            if (sourceTypeRequiredForExtraction()) {
                abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText("this", null));
            }
            PsiStatement expressionStatement = elementFactory.createStatementFromText(
                    abstractMethodInvocation.getText() + ";",
                    null
            );
            typeCheckElimination.getTypeCheckCodeFragment().replace(expressionStatement);
        } else {
            PsiMethodCallExpression abstractMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    typeCheckElimination.getAbstractMethodName() + "()",
                    null
            );
            PsiMethodCallExpression invokerMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                    "get" + abstractClassName + "Object()",
                    null
            );
            PsiExpressionList invokerMethodInvocationArgumentsRewrite = invokerMethodInvocation.getArgumentList();
            if (typeCheckElimination.getTypeLocalVariable() != null) {
                invokerMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                        typeCheckElimination.getTypeLocalVariable().getName(),
                        null
                ));
            } else if (typeCheckElimination.getTypeMethodInvocation() != null) {
                invokerMethodInvocationArgumentsRewrite.add(typeCheckElimination.getTypeMethodInvocation());
            }
            abstractMethodInvocation.getMethodExpression().setQualifierExpression(invokerMethodInvocation);
            PsiExpressionList abstractMethodInvocationArgumentsRewrite = abstractMethodInvocation.getArgumentList();
            if (returnedVariable != null) {
                abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(returnedVariable.getName(), null));
            }
            for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                if (!abstractMethodParameter.equals(returnedVariable)) {
                    abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                            abstractMethodParameter.getName(),
                            null
                    ));
                }
            }
            for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                if (!fragment.equals(returnedVariable)) {
                    abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText(
                            fragment.getName(),
                            null
                    ));
                }
            }
            if (sourceTypeRequiredForExtraction()) {
                abstractMethodInvocationArgumentsRewrite.add(elementFactory.createExpressionFromText("this", null));
            }

            if (returnedVariable != null) {
                PsiStatement expressionStatement = elementFactory.createStatementFromText(
                        returnedVariable.getName() + " = " + abstractMethodInvocation.getText() + ";",
                        null
                );
                typeCheckElimination.getTypeCheckCodeFragment().replace(expressionStatement);
            } else {
                PsiStatement returnStatement = elementFactory.createStatementFromText(
                        "return " + abstractMethodInvocation.getText() + ";",
                        null
                );
                typeCheckElimination.getTypeCheckCodeFragment().replace(returnStatement);
            }
        }
    }

    private boolean typeObjectGetterMethodAlreadyExists() {
        InheritanceTree tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
        if (tree != null) {
            PsiMethod[] contextMethods = sourceTypeDeclaration.getMethods();
            DefaultMutableTreeNode rootNode = tree.getRootNode();
            String rootClassName = (String) rootNode.getUserObject();
            DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
            List<String> subclassNames = new ArrayList<String>();
            while (leaf != null) {
                subclassNames.add((String) leaf.getUserObject());
                leaf = leaf.getNextLeaf();
            }
            for (PsiMethod contextMethod : contextMethods) {
                PsiType returnType = contextMethod.getReturnType();
                if (returnType != null) {
                    if (returnType.getCanonicalText().equals(rootClassName)) {
                        PsiCodeBlock contextMethodBody = contextMethod.getBody();
                        if (contextMethodBody != null) {
                            PsiStatement[] statements = contextMethodBody.getStatements();
                            if (statements.length > 0 && statements[0] instanceof PsiSwitchStatement) {
                                PsiSwitchStatement switchStatement = (PsiSwitchStatement) statements[0];
                                PsiStatement[] statements2 = switchStatement.getBody().getStatements();
                                int matchCounter = 0;
                                for (PsiStatement statement2 : statements2) {
                                    if (statement2 instanceof PsiReturnStatement) {
                                        PsiReturnStatement returnStatement = (PsiReturnStatement) statement2;
                                        PsiExpression returnStatementExpression = returnStatement.getReturnValue();
                                        if (returnStatementExpression instanceof PsiNewExpression) {
                                            PsiNewExpression classInstanceCreation = (PsiNewExpression) returnStatementExpression;
                                            PsiClass createdClass = (PsiClass) classInstanceCreation.getClassReference().resolve();
                                            if (subclassNames.contains(createdClass.getQualifiedName())) {
                                                matchCounter++;
                                            }
                                        }
                                    }
                                }
                                if (matchCounter == subclassNames.size())
                                    return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void createStateStrategyHierarchy() {
        PsiDirectory contextContainer = sourceFile.getParent();
        InheritanceTree tree = typeCheckElimination.getInheritanceTreeMatchingWithStaticTypes();
        PsiFile stateStrategyFile;
        boolean stateStrategyAlreadyExists = true;
        if (tree != null) {
            DefaultMutableTreeNode rootNode = tree.getRootNode();
            stateStrategyFile = ClassUtil.findPsiClass(PsiManager.getInstance(project), (String) rootNode.getUserObject()).getContainingFile();
        } else {
            stateStrategyFile = contextContainer.findFile(abstractClassName + ".java");
            if (stateStrategyFile == null) {
                stateStrategyAlreadyExists = false;
                stateStrategyFile = contextContainer.createFile(abstractClassName + ".java");
            }
        }

        PsiClass stateStrategyTypeDeclaration = null;
        if (stateStrategyAlreadyExists) {
            PsiElement[] abstractTypeDeclarations = stateStrategyFile.getChildren();
            for (PsiElement child : abstractTypeDeclarations) {
                if (child instanceof PsiClass) {
                    PsiClass typeDeclaration = (PsiClass) child;
                    if (typeDeclaration.getName().equals(abstractClassName)) {
                        stateStrategyTypeDeclaration = typeDeclaration;
                        requiredImportDeclarationsForContext.add(PsiTypesUtil.getClassType(stateStrategyTypeDeclaration));
                        PsiUtil.setModifierProperty(stateStrategyTypeDeclaration, PsiModifier.ABSTRACT, true);
                        break;
                    }
                }
            }
        } else {
            String packageName = PsiUtil.getPackageName(sourceTypeDeclaration);
            if (packageName != null && !packageName.isEmpty()) {
                PsiPackageStatement packageStatement = elementFactory.createPackageStatement(packageName);
                stateStrategyFile.add(packageStatement);
            }
            stateStrategyTypeDeclaration = elementFactory.createClass(abstractClassName);
            PsiUtil.setModifierProperty(stateStrategyTypeDeclaration, PsiModifier.PUBLIC, true);
            PsiUtil.setModifierProperty(stateStrategyTypeDeclaration, PsiModifier.ABSTRACT, true);
        }

        PsiMethod getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
        if (typeCheckElimination.getTypeField() != null) {
            PsiMethod abstractGetterMethodDeclaration;
            if (getterMethod != null) {
                abstractGetterMethodDeclaration = getterMethod;
            } else {
                PsiField typeField = typeCheckElimination.getTypeField();
                PsiType returnType = typeField.getType();
                abstractGetterMethodDeclaration = elementFactory.createMethod("get" + abstractClassName, returnType);
            }
            abstractGetterMethodDeclaration = (PsiMethod) stateStrategyTypeDeclaration.add(abstractGetterMethodDeclaration);
            PsiUtil.setModifierProperty(abstractGetterMethodDeclaration, PsiModifier.PUBLIC, true);
            PsiUtil.setModifierProperty(abstractGetterMethodDeclaration, PsiModifier.ABSTRACT, true);
            abstractGetterMethodDeclaration.getBody().replace(semicolon);
        }

        PsiType abstractMethodReturnType;
        if (returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
            abstractMethodReturnType = PsiType.VOID;
        } else {
            if (returnedVariable != null) {
                abstractMethodReturnType = returnedVariable.getType();
            } else {
                abstractMethodReturnType = typeCheckElimination.getTypeCheckMethodReturnType();
            }
        }
        PsiMethod abstractMethodDeclaration = elementFactory.createMethod(typeCheckElimination.getAbstractMethodName(), abstractMethodReturnType);
        PsiUtil.setModifierProperty(abstractMethodDeclaration, PsiModifier.PUBLIC, true);
        PsiUtil.setModifierProperty(abstractMethodDeclaration, PsiModifier.ABSTRACT, true);
        abstractMethodDeclaration.getBody().replace(semicolon);

        PsiParameterList abstractMethodParametersRewrite = abstractMethodDeclaration.getParameterList();
        if (returnedVariable != null) {
            PsiParameter parameter = elementFactory.createParameter(returnedVariable.getName(), returnedVariable.getType());
            abstractMethodParametersRewrite.add(parameter);
        }
        for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
            if (!abstractMethodParameter.equals(returnedVariable)) {
                abstractMethodParametersRewrite.add(abstractMethodParameter);
            }
        }
        for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
            if (!fragment.equals(returnedVariable)) {
                PsiParameter parameter = elementFactory.createParameter(fragment.getName(), fragment.getType());
                abstractMethodParametersRewrite.add(parameter);
            }
        }

        if (sourceTypeRequiredForExtraction()) {
            PsiType parameterType = PsiTypesUtil.getClassType(sourceTypeDeclaration);
            String parameterName = sourceTypeDeclaration.getName();
            parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
            abstractMethodParametersRewrite.add(elementFactory.createParameter(parameterName, parameterType));
        }

        for (PsiClassType typeBinding : thrownExceptions) {
            abstractMethodDeclaration.getThrowsList().add(elementFactory.createReferenceElementByType(typeBinding));
        }
        stateStrategyTypeDeclaration.add(abstractMethodDeclaration);

        generateRequiredImportDeclarationsBasedOnSignature();

        if (!stateStrategyAlreadyExists) {
            stateStrategyFile.add(stateStrategyTypeDeclaration);
        }

        addImports(getPsiImportList(stateStrategyFile), requiredImportDeclarationsBasedOnSignature);

        List<ArrayList<PsiStatement>> typeCheckStatements = typeCheckElimination.getTypeCheckStatements();
        List<String> subclassNames = new ArrayList<String>(staticFieldMap.values());
        subclassNames.addAll(additionalStaticFieldMap.values());
        if (tree != null) {
            DefaultMutableTreeNode rootNode = tree.getRootNode();
            DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
            while (leaf != null) {
                String qualifiedSubclassName = (String) leaf.getUserObject();
                String subclassName = null;
                if (qualifiedSubclassName.contains("."))
                    subclassName = qualifiedSubclassName.substring(qualifiedSubclassName.lastIndexOf(".") + 1, qualifiedSubclassName.length());
                else
                    subclassName = qualifiedSubclassName;
                if (!subclassNames.contains(subclassName))
                    subclassNames.add(subclassName);
                leaf = leaf.getNextLeaf();
            }
        }

        List<PsiField> staticFields = new ArrayList<>(staticFieldMap.keySet());
        staticFields.addAll(additionalStaticFieldMap.keySet());

        for (PsiExpression expression : typeCheckElimination.getTypeCheckExpressions()) {
            List<PsiField> leafStaticFields = typeCheckElimination.getStaticFields(expression);
            if (leafStaticFields.size() > 1) {
                List<String> leafSubclassNames = new ArrayList<>();
                for (PsiField leafStaticField : leafStaticFields) {
                    leafSubclassNames.add(getTypeNameForNamedConstant(leafStaticField));
                }
                ArrayList<PsiStatement> typeCheckStatements2 = typeCheckElimination.getTypeCheckStatements(expression);
                createIntermediateClassAndItsSubclasses(leafStaticFields, leafSubclassNames, typeCheckStatements2, tree, contextContainer);
                staticFields.removeAll(leafStaticFields);
                subclassNames.removeAll(leafSubclassNames);
                typeCheckStatements.remove(typeCheckStatements2);
            }
        }

        for (int i = 0; i < subclassNames.size(); i++) {
            ArrayList<PsiStatement> statements = null;
            DefaultMutableTreeNode remainingIfStatementExpression = null;
            if (i < typeCheckStatements.size()) {
                statements = typeCheckStatements.get(i);
                PsiExpression expression = typeCheckElimination.getExpressionCorrespondingToTypeCheckStatementList(statements);
                remainingIfStatementExpression = typeCheckElimination.getRemainingIfStatementExpression(expression);
            } else {
                statements = typeCheckElimination.getDefaultCaseStatements();
            }

            boolean subclassAlreadyExists = true;
            PsiFile subclassFile = null;
            if (tree != null) {
                DefaultMutableTreeNode rootNode = tree.getRootNode();
                DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
                while (leaf != null) {
                    String qualifiedSubclassName = (String) leaf.getUserObject();
                    if ((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + subclassNames.get(i))) || qualifiedSubclassName.equals(subclassNames.get(i))) {
                        subclassFile = ClassUtil.findPsiClass(PsiManager.getInstance(project), qualifiedSubclassName).getContainingFile();
                        break;
                    }
                    leaf = leaf.getNextLeaf();
                }
            } else {
                subclassFile = contextContainer.findFile(subclassNames.get(i) + ".java");
                if (subclassFile == null) {
                    subclassAlreadyExists = false;
                    subclassFile = contextContainer.createFile(subclassNames.get(i) + ".java");
                }
            }

            PsiClass subclassTypeDeclaration = null;
            if (subclassAlreadyExists) {
                PsiElement[] abstractTypeDeclarations = subclassFile.getChildren();
                for (PsiElement child : abstractTypeDeclarations) {
                    if (child instanceof PsiClass) {
                        PsiClass typeDeclaration = (PsiClass) child;
                        if (typeDeclaration.getName().equals(subclassNames.get(i))) {
                            subclassTypeDeclaration = typeDeclaration;
                            requiredImportDeclarationsForContext.add(PsiTypesUtil.getClassType(subclassTypeDeclaration));
                            break;
                        }
                    }
                }
            } else {
                String packageName = PsiUtil.getPackageName(sourceTypeDeclaration);
                if (packageName != null && !packageName.isEmpty()) {
                    PsiPackageStatement packageStatement = elementFactory.createPackageStatement(packageName);
                    subclassFile.add(packageStatement);
                }
                PsiDocComment subclassJavaDoc = elementFactory.createDocCommentFromText("/** **/", null);

                PsiField staticFieldNameBinding = staticFields.get(i);
                PsiClass staticFieldNameDeclaringClass = staticFieldNameBinding.getContainingClass();

                PsiDocTag subclassTag = elementFactory.createDocTagFromText(
                        "@see " + staticFieldNameDeclaringClass.getQualifiedName() + "#" + staticFieldNameBinding.getName()
                );

                subclassJavaDoc.add(subclassTag);

                String subclassName = subclassNames.get(i);
                subclassTypeDeclaration = elementFactory.createClass(subclassName);
                subclassTypeDeclaration.getExtendsList().add(elementFactory.createReferenceFromText(abstractClassName, null));
                PsiUtil.setModifierProperty(subclassTypeDeclaration, PsiModifier.PUBLIC, true);
                subclassTypeDeclaration.addBefore(subclassJavaDoc, subclassTypeDeclaration.getFirstChild());
            }

            if (typeCheckElimination.getTypeField() != null) {
                if (getterMethod != null) {
                    PsiMethod concreteGetterMethodDeclaration = elementFactory.createMethod(getterMethod.getName(), getterMethod.getReturnType());
                    PsiUtil.setModifierProperty(concreteGetterMethodDeclaration, PsiModifier.PUBLIC, true);
                    PsiField staticFieldNameBinding = staticFields.get(i);
                    PsiClass staticFieldDeclaringClass = staticFieldNameBinding.getContainingClass();
                    String staticFieldDeclaringClassQualifiedName = staticFieldDeclaringClass.getQualifiedName();
                    String staticFieldNameDeclaringClass;

                    String packageName = PsiUtil.getPackageName(staticFieldDeclaringClass);
                    if (packageName != null && !packageName.isEmpty()) {
                        staticFieldNameDeclaringClass = staticFieldDeclaringClassQualifiedName.substring(packageName.length() + 1);
                    } else {
                        staticFieldNameDeclaringClass = staticFieldDeclaringClassQualifiedName;
                    }

                    PsiStatement returnStatement = elementFactory.createStatementFromText(
                            "return " + staticFieldNameDeclaringClass + "." + staticFields.get(i).getName() + ";",
                            null
                    );
                    concreteGetterMethodDeclaration.getBody().add(returnStatement);
                    subclassTypeDeclaration.add(concreteGetterMethodDeclaration);
                } else {
                    String methodName = "get" + abstractClassName;
                    PsiField typeField = typeCheckElimination.getTypeField();
                    PsiType returnType = typeField.getType();
                    PsiMethod concreteGetterMethodDeclaration = elementFactory.createMethod(methodName, returnType);
                    PsiUtil.setModifierProperty(concreteGetterMethodDeclaration, PsiModifier.PUBLIC, true);
                    PsiField staticFieldNameBinding = staticFields.get(i);
                    PsiClass staticFieldNameDeclaringClass = staticFieldNameBinding.getContainingClass();
                    PsiStatement returnStatement = elementFactory.createStatementFromText(
                            "return " + staticFieldNameDeclaringClass.getQualifiedName() + "." + staticFieldNameBinding.getName() + ";",
                            null
                    );
                    concreteGetterMethodDeclaration.getBody().add(returnStatement);
                    subclassTypeDeclaration.add(concreteGetterMethodDeclaration);
                }
            }

            PsiType returnType;
            if (returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
                returnType = PsiType.VOID;
            } else {
                if (returnedVariable != null) {
                    returnType = returnedVariable.getType();
                } else {
                    returnType = typeCheckElimination.getTypeCheckMethodReturnType();
                }
            }
            PsiMethod concreteMethodDeclaration = elementFactory.createMethod(typeCheckElimination.getAbstractMethodName(), returnType);
            PsiUtil.setModifierProperty(concreteMethodDeclaration, PsiModifier.PUBLIC, true);

            PsiParameterList concreteMethodParametersRewrite = concreteMethodDeclaration.getParameterList();
            if (returnedVariable != null) {
                PsiParameter parameter = elementFactory.createParameter(returnedVariable.getName(), returnedVariable.getType());
                concreteMethodParametersRewrite.add(parameter);
            }
            for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
                if (!abstractMethodParameter.equals(returnedVariable)) {
                    concreteMethodParametersRewrite.add(abstractMethodParameter);
                }
            }
            for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
                if (!fragment.equals(returnedVariable)) {
                    PsiParameter parameter = elementFactory.createParameter(fragment.getName(), fragment.getType());
                    concreteMethodParametersRewrite.add(parameter);
                }
            }
            Set<PsiField> accessedFields = typeCheckElimination.getAccessedFields();
            Set<PsiField> assignedFields = typeCheckElimination.getAssignedFields();
            Set<PsiMethod> accessedMethods = typeCheckElimination.getAccessedMethods();
            Set<PsiMethod> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
            Set<PsiField> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
            Set<PsiField> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();

            if (sourceTypeRequiredForExtraction()) {
                PsiType parameterType = PsiTypesUtil.getClassType(sourceTypeDeclaration);
                String parameterName = sourceTypeDeclaration.getName();
                parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
                concreteMethodParametersRewrite.add(elementFactory.createParameter(parameterName, parameterType));
            }

            for (PsiClassType typeBinding : thrownExceptions) {
                concreteMethodDeclaration.getThrowsList().add(elementFactory.createReferenceElementByType(typeBinding));
            }

            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            PsiCodeBlock concreteMethodBody = concreteMethodDeclaration.getBody();
            PsiCodeBlock ifStatementBody = null;
            if (remainingIfStatementExpression != null) {
                PsiExpression enclosingIfStatementExpression = constructExpression(remainingIfStatementExpression);
                PsiExpression newEnclosingIfStatementExpression = elementFactory.createExpressionFromText(
                        enclosingIfStatementExpression.getText(),
                        typeCheckElimination.getTypeCheckCodeFragment()
                );
                List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(enclosingIfStatementExpression);
                List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
                List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
                List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
                modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
                modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
                replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);
                PsiIfStatement enclosingIfStatement = (PsiIfStatement) elementFactory.createStatementFromText(
                        "if(true){}",
                        null
                );
                enclosingIfStatement.getCondition().replace(newEnclosingIfStatementExpression);
                enclosingIfStatement = (PsiIfStatement) concreteMethodBody.add(enclosingIfStatement);
                ifStatementBody = ((PsiBlockStatement) enclosingIfStatement.getThenBranch()).getCodeBlock();
            }

            for (PsiStatement statement : statements) {
                PsiStatement newStatement = elementFactory.createStatementFromText(statement.getText(), typeCheckElimination.getTypeCheckCodeFragment());
                List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
                List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
                List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
                List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
                modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
                modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
                replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);
                replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement);
                if (ifStatementBody != null) {
                    ifStatementBody.add(newStatement);
                } else {
                    concreteMethodBody.add(newStatement);
                }
            }

            if (returnedVariable != null) {
                concreteMethodBody.add(elementFactory.createStatementFromText(
                        "return " + returnedVariable.getName() + ";",
                        null
                ));
            }
            subclassTypeDeclaration.add(concreteMethodDeclaration);

            if (!subclassAlreadyExists) {
                subclassFile.add(subclassTypeDeclaration);
            }

            Set<PsiType> requiredImportDeclarations = Stream.of(
                    getRequiredImportDeclarationsBasedOnBranch(statements),
                    requiredImportDeclarationsBasedOnSignature
            ).flatMap(Collection::stream).collect(Collectors.toSet());
            addImports(getPsiImportList(subclassFile), requiredImportDeclarations);
        }
    }

    private void createIntermediateClassAndItsSubclasses(List<PsiField> staticFields,
                                                         List<String> subclassNames,
                                                         ArrayList<PsiStatement> typeCheckStatements,
                                                         InheritanceTree tree,
                                                         PsiDirectory contextContainer) {
        String intermediateClassName = commonSubstring(subclassNames);
        PsiExpression expression = typeCheckElimination.getExpressionCorrespondingToTypeCheckStatementList(typeCheckStatements);
        DefaultMutableTreeNode remainingIfStatementExpression = typeCheckElimination.getRemainingIfStatementExpression(expression);
        PsiFile intermediateClassFile = null;
        boolean intermediateClassAlreadyExists = true;
        if (tree != null) {
            DefaultMutableTreeNode rootNode = tree.getRootNode();
            DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
            while (leaf != null) {
                String qualifiedSubclassName = (String) leaf.getUserObject();
                if ((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + intermediateClassName)) || qualifiedSubclassName.equals(intermediateClassName)) {
                    intermediateClassFile = ClassUtil.findPsiClass(PsiManager.getInstance(project), qualifiedSubclassName).getContainingFile();
                    break;
                }
                leaf = leaf.getNextLeaf();
            }
        } else {
            intermediateClassFile = contextContainer.findFile(intermediateClassName + ".java");
            if (intermediateClassFile == null) {
                intermediateClassAlreadyExists = false;
                intermediateClassFile = contextContainer.createFile(intermediateClassName + ".java");
            }
        }

        PsiClass intermediateClassTypeDeclaration = null;
        if (intermediateClassAlreadyExists) {
            PsiElement[] abstractTypeDeclarations = intermediateClassFile.getChildren();
            for (PsiElement child : abstractTypeDeclarations) {
                if (child instanceof PsiClass) {
                    PsiClass typeDeclaration = (PsiClass) child;
                    if (intermediateClassName.equals(typeDeclaration.getName())) {
                        intermediateClassTypeDeclaration = typeDeclaration;
                        requiredImportDeclarationsForContext.add(PsiTypesUtil.getClassType(intermediateClassTypeDeclaration));
                        break;
                    }
                }
            }
        } else {
            String packageName = PsiUtil.getPackageName(sourceTypeDeclaration);
            if (packageName != null && !packageName.isEmpty()) {
                PsiPackageStatement packageStatement = elementFactory.createPackageStatement(packageName);
                intermediateClassFile.add(packageStatement);
            }
            intermediateClassTypeDeclaration = elementFactory.createClass(intermediateClassName);
            intermediateClassTypeDeclaration.getExtendsList().add(elementFactory.createReferenceFromText(abstractClassName, null));
            PsiUtil.setModifierProperty(intermediateClassTypeDeclaration, PsiModifier.PUBLIC, true);
            PsiUtil.setModifierProperty(intermediateClassTypeDeclaration, PsiModifier.ABSTRACT, true);
        }

        PsiType concreteMethodReturnType;
        if (returnedVariable == null && !typeCheckElimination.typeCheckCodeFragmentContainsReturnStatement()) {
            concreteMethodReturnType = PsiType.VOID;
        } else {
            if (returnedVariable != null) {
                concreteMethodReturnType = returnedVariable.getType();
            } else {
                concreteMethodReturnType = typeCheckElimination.getTypeCheckMethodReturnType();
            }
        }
        PsiMethod concreteMethodDeclaration = elementFactory.createMethod(typeCheckElimination.getAbstractMethodName(), concreteMethodReturnType);
        PsiUtil.setModifierProperty(concreteMethodDeclaration, PsiModifier.PUBLIC, true);

        PsiParameterList concreteMethodParametersRewrite = concreteMethodDeclaration.getParameterList();
        if (returnedVariable != null) {
            PsiParameter parameter = elementFactory.createParameter(returnedVariable.getName(), returnedVariable.getType());
            concreteMethodParametersRewrite.add(parameter);
        }
        for (PsiParameter abstractMethodParameter : typeCheckElimination.getAccessedParameters()) {
            if (!abstractMethodParameter.equals(returnedVariable)) {
                concreteMethodParametersRewrite.add(abstractMethodParameter);
            }
        }
        for (PsiVariable fragment : typeCheckElimination.getAccessedLocalVariables()) {
            if (!fragment.equals(returnedVariable)) {
                PsiParameter parameter = elementFactory.createParameter(fragment.getName(), fragment.getType());
                concreteMethodParametersRewrite.add(parameter);
            }
        }

        if (sourceTypeRequiredForExtraction()) {
            PsiType parameterType = PsiTypesUtil.getClassType(sourceTypeDeclaration);
            String parameterName = sourceTypeDeclaration.getName();
            parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
            concreteMethodParametersRewrite.add(elementFactory.createParameter(parameterName, parameterType));
        }

        for (PsiClassType typeBinding : thrownExceptions) { // TODO: Use refactoring utility
            concreteMethodDeclaration.getThrowsList().add(elementFactory.createReferenceElementByType(typeBinding));
        }

        Set<PsiField> accessedFields = typeCheckElimination.getAccessedFields();
        Set<PsiField> assignedFields = typeCheckElimination.getAssignedFields();
        Set<PsiMethod> accessedMethods = typeCheckElimination.getAccessedMethods();
        Set<PsiMethod> superAccessedMethods = typeCheckElimination.getSuperAccessedMethods();
        Set<PsiField> superAccessedFields = typeCheckElimination.getSuperAccessedFieldBindings();
        Set<PsiField> superAssignedFields = typeCheckElimination.getSuperAssignedFieldBindings();

        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        PsiCodeBlock concreteMethodBody = concreteMethodDeclaration.getBody();
        PsiCodeBlock ifStatementBody = null;
        if (remainingIfStatementExpression != null) {
            PsiExpression enclosingIfStatementExpression = constructExpression(remainingIfStatementExpression);
            PsiExpression newEnclosingIfStatementExpression = elementFactory.createExpressionFromText(
                    enclosingIfStatementExpression.getText(),
                    typeCheckElimination.getTypeCheckCodeFragment()
            );
            List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(enclosingIfStatementExpression);
            List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newEnclosingIfStatementExpression);
            List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(enclosingIfStatementExpression);
            List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newEnclosingIfStatementExpression);
            modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
            modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
            replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);
            PsiIfStatement enclosingIfStatement = (PsiIfStatement) elementFactory.createStatementFromText(
                    "if(true){}",
                    null
            );
            enclosingIfStatement.getCondition().replace(newEnclosingIfStatementExpression);
            enclosingIfStatement = (PsiIfStatement) concreteMethodBody.add(enclosingIfStatement);
            ifStatementBody = ((PsiBlockStatement) enclosingIfStatement.getThenBranch()).getCodeBlock();
        }

        for (PsiStatement statement : typeCheckStatements) {
            PsiStatement newStatement = elementFactory.createStatementFromText(statement.getText(), typeCheckElimination.getTypeCheckCodeFragment());
            List<PsiExpression> oldVariableInstructions = expressionExtractor.getVariableInstructions(statement);
            List<PsiExpression> newVariableInstructions = expressionExtractor.getVariableInstructions(newStatement);
            List<PsiExpression> oldMethodInvocations = expressionExtractor.getMethodInvocations(statement);
            List<PsiExpression> newMethodInvocations = expressionExtractor.getMethodInvocations(newStatement);
            modifySourceVariableInstructionsInSubclass(oldVariableInstructions, newVariableInstructions, accessedFields, assignedFields, superAccessedFields, superAssignedFields);
            modifySourceMethodInvocationsInSubclass(oldMethodInvocations, newMethodInvocations, accessedMethods, superAccessedMethods);
            replaceThisExpressionWithContextParameterInMethodInvocationArguments(oldMethodInvocations, newMethodInvocations);
            replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(newStatement);
            if (ifStatementBody != null) {
                ifStatementBody.add(newStatement);
            } else {
                concreteMethodBody.add(newStatement);
            }
        }

        if (returnedVariable != null) {
            concreteMethodBody.add(elementFactory.createStatementFromText(
                    "return " + returnedVariable.getName() + ";",
                    null
            ));
        }

        intermediateClassTypeDeclaration.add(concreteMethodDeclaration);

        if (!intermediateClassAlreadyExists) {
            intermediateClassFile.add(intermediateClassTypeDeclaration);
        }

        Set<PsiType> requiredImportDeclarations = Stream.of(
                getRequiredImportDeclarationsBasedOnBranch(typeCheckStatements),
                requiredImportDeclarationsBasedOnSignature
        ).flatMap(Collection::stream).collect(Collectors.toSet());
        addImports(getPsiImportList(intermediateClassFile), requiredImportDeclarations);

        for (int i = 0; i < subclassNames.size(); i++) {
            PsiFile subclassFile = null;
            boolean subclassAlreadyExists = true;
            if (tree != null) {
                DefaultMutableTreeNode rootNode = tree.getRootNode();
                DefaultMutableTreeNode leaf = rootNode.getFirstLeaf();
                while (leaf != null) {
                    String qualifiedSubclassName = (String) leaf.getUserObject();
                    if ((qualifiedSubclassName.contains(".") && qualifiedSubclassName.endsWith("." + subclassNames.get(i))) || qualifiedSubclassName.equals(subclassNames.get(i))) {
                        subclassFile = ClassUtil.findPsiClass(PsiManager.getInstance(project), qualifiedSubclassName).getContainingFile();
                        break;
                    }
                    leaf = leaf.getNextLeaf();
                }
            } else {
                subclassFile = contextContainer.findFile(subclassNames.get(i) + ".java");
                if (subclassFile == null) {
                    subclassAlreadyExists = false;
                    subclassFile = contextContainer.createFile(subclassNames.get(i) + ".java");
                }
            }


            PsiClass subclassTypeDeclaration = null;
            if (subclassAlreadyExists) {
                PsiElement[] abstractTypeDeclarations = subclassFile.getChildren();
                for (PsiElement child : abstractTypeDeclarations) {
                    if (child instanceof PsiClass) {
                        PsiClass typeDeclaration = (PsiClass) child;
                        if (typeDeclaration.getName().equals(subclassNames.get(i))) {
                            subclassTypeDeclaration = typeDeclaration;
                            requiredImportDeclarationsForContext.add(PsiTypesUtil.getClassType(subclassTypeDeclaration));
                            break;
                        }
                    }
                }
            } else {
                String packageName = PsiUtil.getPackageName(sourceTypeDeclaration);
                if (packageName != null && !packageName.isEmpty()) {
                    PsiPackageStatement packageStatement = elementFactory.createPackageStatement(packageName);
                    subclassFile.add(packageStatement);
                }
                PsiDocComment subclassJavaDoc = elementFactory.createDocCommentFromText("", null);

                PsiField staticFieldNameBinding = staticFields.get(i);
                PsiClass staticFieldNameDeclaringClass = staticFieldNameBinding.getContainingClass();

                PsiDocTag subclassTag = elementFactory.createDocTagFromText(
                        "@see " + staticFieldNameDeclaringClass.getQualifiedName() + "#" + staticFieldNameBinding.getName()
                );

                subclassJavaDoc.add(subclassTag);

                String subclassName = subclassNames.get(i);
                subclassTypeDeclaration = elementFactory.createClass(subclassName);
                subclassTypeDeclaration.getExtendsList().add(elementFactory.createReferenceFromText(intermediateClassName, null));
                PsiUtil.setModifierProperty(subclassTypeDeclaration, PsiModifier.PUBLIC, true);
                subclassTypeDeclaration.addBefore(subclassJavaDoc, subclassTypeDeclaration.getFirstChild());
            }

            PsiMethod getterMethod = typeCheckElimination.getTypeFieldGetterMethod();
            if (typeCheckElimination.getTypeField() != null) {
                if (getterMethod != null) {
                    PsiMethod concreteMethod = elementFactory.createMethod(getterMethod.getName(), getterMethod.getReturnType());
                    PsiUtil.setModifierProperty(concreteMethod, PsiModifier.PUBLIC, true);
                    PsiCodeBlock concreteGetterMethodBody = concreteMethod.getBody();
                    PsiField staticFieldNameBinding = staticFields.get(i);
                    PsiClass staticFieldNameDeclaringClass = staticFieldNameBinding.getContainingClass();
                    PsiStatement returnStatement = elementFactory.createStatementFromText(
                            "return " + staticFieldNameDeclaringClass.getQualifiedName() + "." + staticFieldNameBinding.getName() + ";",
                            null
                    );
                    concreteGetterMethodBody.add(returnStatement);
                    subclassTypeDeclaration.add(concreteMethod);
                } else {
                    String methodName = "get" + abstractClassName;
                    PsiField typeField = typeCheckElimination.getTypeField();
                    PsiType returnType = typeField.getType();
                    PsiMethod concreteGetterMethodDeclaration = elementFactory.createMethod(methodName, returnType);
                    PsiUtil.setModifierProperty(concreteGetterMethodDeclaration, PsiModifier.PUBLIC, true);
                    PsiField staticFieldNameBinding = staticFields.get(i);
                    PsiClass staticFieldNameDeclaringClass = staticFieldNameBinding.getContainingClass();
                    PsiStatement returnStatement = elementFactory.createStatementFromText(
                            "return " + staticFieldNameDeclaringClass.getQualifiedName() + "." + staticFieldNameBinding.getName() + ";",
                            null
                    );
                    concreteGetterMethodDeclaration.getBody().add(returnStatement);
                    subclassTypeDeclaration.add(concreteGetterMethodDeclaration);
                }
            }

            if (!subclassAlreadyExists) {
                subclassFile.add(subclassTypeDeclaration);
            }
        }
    }

    private void modifyTypeFieldAssignmentsInContextClass(boolean modify) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        PsiMethod typeFieldSetterMethod = typeCheckElimination.getTypeFieldSetterMethod();
        for (PsiMethod methodDeclaration : contextMethods) {
            if (!methodDeclaration.equals(typeFieldSetterMethod)) {
                PsiCodeBlock methodBody = methodDeclaration.getBody();
                if (methodBody != null) {
                    PsiStatement[] statements = methodBody.getStatements();
                    for (PsiStatement statement : statements) {
                        List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
                        for (PsiExpression expression : assignments) {
                            PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                            PsiExpression leftHandSide = assignment.getLExpression();
                            PsiReferenceExpression assignedVariable = null;
                            PsiExpression invoker = null;
                            if (leftHandSide instanceof PsiReferenceExpression) {
                                assignedVariable = (PsiReferenceExpression) leftHandSide;
                                invoker = assignedVariable.getQualifierExpression();
                            }

                            PsiExpression rightHandSide = assignment.getRExpression();
                            List<PsiExpression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
                            if (assignedVariable != null) {
                                PsiElement leftHandBinding = assignedVariable.resolve();
                                if (leftHandBinding instanceof PsiVariable) {
                                    PsiVariable assignedVariableBinding = (PsiVariable) leftHandBinding;
                                    if (typeCheckElimination.getTypeField().equals(assignedVariableBinding)) {
                                        if (modify && !nodeExistsInsideTypeCheckCodeFragment(assignment)) {
                                            String setterMethodInvocationName;
                                            if (typeCheckElimination.getTypeFieldSetterMethod() != null) {
                                                setterMethodInvocationName = typeCheckElimination.getTypeFieldSetterMethod().getName();
                                            } else {
                                                setterMethodInvocationName = setterMethodName();
                                            }
                                            String qualifierExpressionText = "";
                                            if (invoker != null) {
                                                qualifierExpressionText = invoker.getText() + ".";
                                            }
                                            PsiMethodCallExpression setterMethodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                                                    qualifierExpressionText + setterMethodInvocationName + "()",
                                                    null
                                            );
                                            PsiExpression argument = (PsiExpression) setterMethodInvocation.getArgumentList().add(rightHandSide);
                                            assignment.replace(setterMethodInvocation);
                                            accessedVariables = expressionExtractor.getVariableInstructions(argument);
                                        }
                                        for (PsiExpression expression2 : accessedVariables) {
                                            PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression2;
                                            PsiElement rightHandBinding = accessedVariable.resolve();
                                            if (rightHandBinding instanceof PsiVariable) {
                                                PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                                                if (accessedVariableBinding instanceof PsiField && accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC) &&
                                                        !containsVariable(staticFields, accessedVariable) && accessedVariableBinding.getType().equals(assignedVariableBinding.getType())) {
                                                    if (!containsStaticFieldKey(accessedVariable) && !modify)
                                                        additionalStaticFieldMap.put((PsiField) accessedVariableBinding, generateSubclassName(accessedVariableBinding));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            for (PsiExpression expression2 : accessedVariables) {
                                PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression2;
                                PsiElement rightHandBinding = accessedVariable.resolve();
                                if (rightHandBinding instanceof PsiVariable) {
                                    PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                                    if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(accessedVariableBinding)) {
                                        if (modify && !nodeExistsInsideTypeCheckCodeFragment(accessedVariable)) {
                                            String getterName;
                                            if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                                getterName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                            } else {
                                                getterName = "get" + abstractClassName;
                                            }
                                            PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                    getterName + "()",
                                                    null
                                            );
                                            accessedVariable.replace(getterMethodInvocation);
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

    private void modifyTypeFieldAccessesInContextClass(boolean modify) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        Set<PsiMethod> contextMethods = getAllMethodDeclarationsInSourceClass();
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        for (PsiMethod methodDeclaration : contextMethods) {
            PsiCodeBlock methodBody = methodDeclaration.getBody();
            if (methodBody != null) {
                PsiStatement[] statements = methodBody.getStatements();
                for (PsiStatement statement : statements) {
                    if (statement instanceof PsiSwitchStatement) {
                        PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
                        PsiExpression switchStatementExpression = switchStatement.getExpression();
                        PsiReferenceExpression accessedVariable = null;
                        if (switchStatementExpression instanceof PsiReferenceExpression) {
                            accessedVariable = (PsiReferenceExpression) switchStatementExpression;
                        }

                        if (accessedVariable != null) {
                            PsiElement switchStatementExpressionBinding = accessedVariable.resolve();
                            if (switchStatementExpressionBinding instanceof PsiVariable) {
                                PsiVariable accessedVariableBinding = (PsiVariable) switchStatementExpressionBinding;
                                if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(accessedVariableBinding)) {
                                    if (modify && !nodeExistsInsideTypeCheckCodeFragment(switchStatementExpression)) {
                                        String getterMethodName;
                                        if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                            getterMethodName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                        } else {
                                            getterMethodName = "get" + abstractClassName;
                                        }
                                        PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                getterMethodName + "()",
                                                null
                                        );
                                        switchStatementExpression.replace(getterMethodInvocation);
                                    }
                                    PsiStatement[] statements2 = switchStatement.getBody().getStatements();
                                    for (PsiStatement statement2 : statements2) {
                                        if (statement2 instanceof PsiSwitchLabelStatement && !((PsiSwitchLabelStatement) statement2).isDefaultCase()) {
                                            PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) statement2;
                                            PsiExpressionList switchCaseExpressions = switchCase.getCaseValues();
                                            for (PsiExpression switchCaseExpression : switchCaseExpressions.getExpressions()) {
                                                PsiReferenceExpression comparedVariable = null;
                                                if (switchCaseExpression instanceof PsiReferenceExpression) {
                                                    comparedVariable = (PsiReferenceExpression) switchCaseExpression;
                                                }
                                                if (comparedVariable != null) {
                                                    PsiElement switchCaseExpressionBinding = comparedVariable.resolve();
                                                    if (switchCaseExpressionBinding instanceof PsiVariable) {
                                                        PsiVariable comparedVariableBinding = (PsiVariable) switchCaseExpressionBinding;
                                                        if (comparedVariableBinding instanceof PsiField
                                                                && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                                && !containsVariable(staticFields, comparedVariable)
                                                                && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                                            if (!containsStaticFieldKey(comparedVariable) && !modify)
                                                                additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
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
                    List<PsiExpression> methodInvocations = expressionExtractor.getMethodInvocations(statement);
                    for (PsiExpression expression : methodInvocations) {
                        if (expression instanceof PsiMethodCallExpression) {
                            PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) expression;
                            PsiExpression[] arguments = methodInvocation.getArgumentList().getExpressions();
                            for (PsiExpression argument : arguments) {
                                PsiReferenceExpression accessedVariable = null;
                                if (argument instanceof PsiReferenceExpression) {
                                    accessedVariable = (PsiReferenceExpression) argument;
                                }

                                if (accessedVariable != null) {
                                    PsiElement argumentBinding = accessedVariable.resolve();
                                    if (argumentBinding != null && argumentBinding instanceof PsiVariable) {
                                        PsiVariable accessedVariableBinding = (PsiVariable) argumentBinding;
                                        if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(accessedVariableBinding)) {
                                            if (modify && !nodeExistsInsideTypeCheckCodeFragment(argument)) {
                                                String getterMethodName;
                                                if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                                    getterMethodName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                                } else {
                                                    getterMethodName = "get" + abstractClassName;
                                                }
                                                PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                        getterMethodName + "()",
                                                        null
                                                );
                                                argument.replace(getterMethodInvocation);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement);
                    for (PsiExpression expression : classInstanceCreations) {
                        PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
                        PsiExpression[] arguments = classInstanceCreation.getArgumentList().getExpressions();
                        for (PsiExpression argument : arguments) {
                            PsiReferenceExpression accessedVariable = null;
                            if (argument instanceof PsiReferenceExpression) {
                                accessedVariable = (PsiReferenceExpression) argument;
                            }

                            if (accessedVariable != null) {
                                PsiElement argumentBinding = accessedVariable.resolve();
                                if (argumentBinding instanceof PsiVariable) {
                                    PsiVariable accessedVariableBinding = (PsiVariable) argumentBinding;
                                    if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(argumentBinding)) {
                                        if (modify && !nodeExistsInsideTypeCheckCodeFragment(argument)) {
                                            String getterMethodName;
                                            if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                                getterMethodName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                            } else {
                                                getterMethodName = "get" + abstractClassName;
                                            }
                                            PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                    getterMethodName + "()",
                                                    null
                                            );
                                            argument.replace(getterMethodInvocation);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    List<PsiExpression> infixExpressions = expressionExtractor.getInfixExpressions(statement); // get PolyadicExpression's?
                    for (PsiExpression expression : infixExpressions) {
                        PsiBinaryExpression infixExpression = (PsiBinaryExpression) expression;
                        PsiExpression leftOperand = infixExpression.getLOperand();
                        PsiExpression rightOperand = infixExpression.getROperand();
                        PsiReferenceExpression accessedVariable = null;
                        PsiReferenceExpression comparedVariable = null;
                        boolean typeFieldIsReplaced = false;
                        if (leftOperand instanceof PsiReferenceExpression) {
                            accessedVariable = (PsiReferenceExpression) leftOperand;
                        }

                        if (rightOperand instanceof PsiReferenceExpression) {
                            comparedVariable = (PsiReferenceExpression) rightOperand;
                        }

                        if (accessedVariable != null) {
                            PsiElement leftOperandBinding = accessedVariable.resolve();
                            if (leftOperandBinding instanceof PsiVariable) {
                                PsiVariable accessedVariableBinding = (PsiVariable) leftOperandBinding;
                                if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(accessedVariableBinding)) {
                                    if (modify && !nodeExistsInsideTypeCheckCodeFragment(leftOperand) && !isAssignmentChild(infixExpression)) {
                                        String getterMethodName;
                                        if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                            getterMethodName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                        } else {
                                            getterMethodName = "get" + abstractClassName;
                                        }
                                        PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                getterMethodName + "()",
                                                null
                                        );
                                        leftOperand.replace(getterMethodInvocation);
                                    }
                                    typeFieldIsReplaced = true;
                                    if (comparedVariable != null) {
                                        PsiElement rightOperandBinding = comparedVariable.resolve();
                                        if (rightOperandBinding instanceof PsiVariable) {
                                            PsiVariable comparedVariableBinding = (PsiVariable) rightOperandBinding;
                                            if (comparedVariableBinding instanceof PsiField
                                                    && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                    && !containsVariable(staticFields, comparedVariable)
                                                    && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                                if (!containsStaticFieldKey(comparedVariable) && !modify)
                                                    additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!typeFieldIsReplaced) {
                            if (rightOperand instanceof PsiReferenceExpression) {
                                accessedVariable = (PsiReferenceExpression) rightOperand;
                            }

                            if (leftOperand instanceof PsiReferenceExpression) {
                                comparedVariable = (PsiReferenceExpression) leftOperand;
                            }

                            if (accessedVariable != null) {
                                PsiElement rightOperandBinding = accessedVariable.resolve();
                                if (rightOperandBinding instanceof PsiVariable) {
                                    PsiVariable accessedVariableBinding = (PsiVariable) rightOperandBinding;
                                    if (accessedVariableBinding instanceof PsiField && typeCheckElimination.getTypeField().equals(accessedVariableBinding)) {
                                        if (modify && !nodeExistsInsideTypeCheckCodeFragment(rightOperand) && !isAssignmentChild(infixExpression)) {
                                            String getterMethodName;
                                            if (typeCheckElimination.getTypeFieldGetterMethod() != null) {
                                                getterMethodName = typeCheckElimination.getTypeFieldGetterMethod().getName();
                                            } else {
                                                getterMethodName = "get" + abstractClassName;
                                            }
                                            PsiExpression getterMethodInvocation = elementFactory.createExpressionFromText(
                                                    getterMethodName + "()",
                                                    null
                                            );
                                            rightOperand.replace(getterMethodInvocation);
                                        }

                                        if (comparedVariable != null) {
                                            PsiElement leftOperandBinding = comparedVariable.resolve();
                                            if (leftOperandBinding instanceof PsiVariable) {
                                                PsiVariable comparedVariableBinding = (PsiVariable) leftOperandBinding;
                                                if (comparedVariableBinding instanceof PsiField
                                                        && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                        && !containsVariable(staticFields, comparedVariable)
                                                        && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                                    if (!containsStaticFieldKey(comparedVariable) && !modify)
                                                        additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
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
        List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(methodDeclaration.getBody());
        for (PsiExpression expression : classInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
            PsiAnonymousClass anonymousClassDeclaration = classInstanceCreation.getAnonymousClass();
            if (anonymousClassDeclaration != null) {
                PsiMethod[] declarationMethods = anonymousClassDeclaration.getMethods();
                methods.addAll(Arrays.asList(declarationMethods));
            }
        }
        return methods;
    }

    private Set<PsiMethod> getMethodDeclarationsWithinAnonymousClassDeclarations(PsiField fieldDeclaration) {
        Set<PsiMethod> methods = new LinkedHashSet<>();
        PsiExpression expression = fieldDeclaration.getInitializer();
        if (expression instanceof PsiNewExpression) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) expression;
            PsiAnonymousClass anonymousClassDeclaration = classInstanceCreation.getAnonymousClass();
            if (anonymousClassDeclaration != null) {
                PsiMethod[] declarationMethods = anonymousClassDeclaration.getMethods();
                methods.addAll(Arrays.asList(declarationMethods));
            }
        }
        return methods;
    }

    private boolean isAssignmentChild(PsiElement node) {
        if (node instanceof PsiAssignmentExpression)
            return true;
        else if (node instanceof PsiStatement)
            return false;
        else
            return isAssignmentChild(node.getParent());
    }

    private boolean nodeExistsInsideTypeCheckCodeFragment(PsiElement node) {
        PsiStatement statement = typeCheckElimination.getTypeCheckCodeFragment();
        int startPosition = statement.getTextOffset();
        int endPosition = startPosition + statement.getTextLength();
        if (node.getTextOffset() >= startPosition && node.getTextOffset() <= endPosition) {
            return true;
        } else {
            return false;
        }
    }

    private void generateRequiredImportDeclarationsBasedOnSignature() { //TODO: move to base class
        Set<PsiType> typeBindings = new LinkedHashSet<>();
        if (returnedVariable != null) {
            PsiType returnType = returnedVariable.getType();
            typeBindings.add(returnType);
        } else {
            PsiType returnType = typeCheckElimination.getTypeCheckMethodReturnType();
            typeBindings.add(returnType);
        }

        Set<PsiParameter> parameters = typeCheckElimination.getAccessedParameters();
        for (PsiParameter parameter : parameters) {
            if (!parameter.equals(returnedVariable)) {
                PsiType parameterType = parameter.getType();
                typeBindings.add(parameterType);
            }
        }

        Set<PsiVariable> accessedLocalVariables = typeCheckElimination.getAccessedLocalVariables();
        for (PsiVariable fragment : accessedLocalVariables) {
            if (!fragment.equals(returnedVariable)) {
                PsiType variableType = fragment.getType();
                typeBindings.add(variableType);
            }
        }

        if (typeCheckElimination.getAccessedFields().size() > 0 || typeCheckElimination.getAssignedFields().size() > 0 ||
                typeCheckElimination.getAccessedMethods().size() > 0 || typeCheckElimination.getSuperAccessedMethods().size() > 0 ||
                typeCheckElimination.getSuperAccessedFieldBindings().size() > 0 || typeCheckElimination.getSuperAssignedFieldBindings().size() > 0) {
            typeBindings.add(PsiTypesUtil.getClassType(sourceTypeDeclaration));
        }

        typeBindings.addAll(thrownExceptions);
//		RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsBasedOnSignature); // TODO: make it work
        requiredImportDeclarationsBasedOnSignature.addAll(typeBindings); // TODO: this is temporary
    }

    private Set<PsiType> getRequiredImportDeclarationsBasedOnBranch(ArrayList<PsiStatement> statements) { // TODO: move to base class and use RefactoringUtility
        Set<PsiType> typeBindings = new LinkedHashSet<>();
        for (PsiStatement statement : statements) {
            TypeVisitor typeVisitor = new TypeVisitor();
            statement.accept(typeVisitor);
            typeBindings.addAll(typeVisitor.getTypes());
        }
        return typeBindings;
    }

    //
//	private void addImportDeclaration(ITypeBinding typeBinding, CompilationUnit targetCompilationUnit, ASTRewrite targetRewriter) {
//		String qualifiedName = typeBinding.getQualifiedName();
//		String qualifiedPackageName = "";
//		if(qualifiedName.contains("."))
//			qualifiedPackageName = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
//		PackageDeclaration sourcePackageDeclaration = sourceCompilationUnit.getPackage();
//		String sourcePackageDeclarationName = "";
//		if(sourcePackageDeclaration != null)
//			sourcePackageDeclarationName = sourcePackageDeclaration.getName().getFullyQualifiedName();
//		if(!qualifiedPackageName.equals("") && !qualifiedPackageName.equals("java.lang") &&
//				!qualifiedPackageName.equals(sourcePackageDeclarationName) && !typeBinding.isNested()) {
//			List<ImportDeclaration> importDeclarationList = targetCompilationUnit.imports();
//			boolean found = false;
//			for(ImportDeclaration importDeclaration : importDeclarationList) {
//				if(!importDeclaration.isOnDemand()) {
//					if(qualifiedName.equals(importDeclaration.getName().getFullyQualifiedName())) {
//						found = true;
//						break;
//					}
//				}
//				else {
//					if(qualifiedPackageName.equals(importDeclaration.getName().getFullyQualifiedName())) {
//						found = true;
//						break;
//					}
//				}
//			}
//			if(!found) {
//				AST ast = targetCompilationUnit.getAST();
//				ImportDeclaration importDeclaration = ast.newImportDeclaration();
//				targetRewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, ast.newName(qualifiedName), null);
//				ListRewrite importRewrite = targetRewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
//				importRewrite.insertLast(importDeclaration, null);
//			}
//		}
//	}
//
    private void setPublicModifierToStaticFields() {
        PsiField[] fieldDeclarations = sourceTypeDeclaration.getFields();
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        for (PsiField simpleName : additionalStaticFieldMap.keySet()) {
            staticFields.add(simpleName);
        }
        for (PsiField fieldDeclaration : fieldDeclarations) {
            for (PsiField staticField : staticFields) {
                if (staticField.equals(fieldDeclaration)) {
                    PsiUtil.setModifierProperty(fieldDeclaration, PsiModifier.PUBLIC, true);
                }
            }
        }
    }

    private void identifyTypeLocalVariableAssignmentsInTypeCheckMethod() {
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        PsiCodeBlock methodBody = typeCheckElimination.getTypeCheckMethod().getBody();
        if (methodBody != null) {
            PsiStatement[] statements = methodBody.getStatements();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            for (PsiStatement statement : statements) {
                List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
                for (PsiExpression expression : assignments) {
                    PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                    PsiExpression leftHandSide = assignment.getLExpression();
                    PsiReferenceExpression assignedVariable = null;
                    PsiExpression invoker = null;
                    if (leftHandSide instanceof PsiReferenceExpression) {
                        assignedVariable = (PsiReferenceExpression) leftHandSide;
                        invoker = assignedVariable.getQualifierExpression();
                    }
                    PsiExpression rightHandSide = assignment.getRExpression();
                    List<PsiExpression> accessedVariables = expressionExtractor.getVariableInstructions(rightHandSide);
                    if (assignedVariable != null) {
                        PsiElement leftHandBinding = assignedVariable.resolve();
                        if (leftHandBinding instanceof PsiVariable) {
                            PsiVariable assignedVariableBinding = (PsiVariable) leftHandBinding;
                            if (typeCheckElimination.getTypeLocalVariable().equals(assignedVariableBinding)) {
                                for (PsiExpression expression2 : accessedVariables) {
                                    PsiReferenceExpression accessedVariable = (PsiReferenceExpression) expression2;
                                    PsiElement rightHandBinding = accessedVariable.resolve();
                                    if (rightHandBinding != null && rightHandBinding instanceof PsiVariable) {
                                        PsiVariable accessedVariableBinding = (PsiVariable) rightHandBinding;
                                        if (accessedVariableBinding instanceof PsiField
                                                && accessedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                && !containsVariable(staticFields, accessedVariable)
                                                && accessedVariableBinding.getType().equals(assignedVariableBinding.getType())) {
                                            if (!containsStaticFieldKey(accessedVariable))
                                                additionalStaticFieldMap.put((PsiField) accessedVariableBinding, generateSubclassName(accessedVariableBinding));
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

    private void identifyTypeLocalVariableAccessesInTypeCheckMethod() {
        List<PsiField> staticFields = typeCheckElimination.getStaticFields();
        PsiCodeBlock methodBody = typeCheckElimination.getTypeCheckMethod().getBody();
        if (methodBody != null) {
            PsiStatement[] statements = methodBody.getStatements();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            for (PsiStatement statement : statements) {
                if (statement instanceof PsiSwitchStatement) {
                    PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
                    PsiExpression switchStatementExpression = switchStatement.getExpression();
                    PsiReferenceExpression accessedVariable = null;
                    if (switchStatementExpression instanceof PsiReferenceExpression) {
                        accessedVariable = (PsiReferenceExpression) switchStatementExpression;
                    }

                    if (accessedVariable != null) {
                        PsiElement switchStatementExpressionBinding = accessedVariable.resolve();
                        if (switchStatementExpressionBinding instanceof PsiVariable) {
                            PsiVariable accessedVariableBinding = (PsiVariable) switchStatementExpressionBinding;
                            if (typeCheckElimination.getTypeLocalVariable().equals(accessedVariableBinding)) {
                                PsiStatement[] statements2 = switchStatement.getBody().getStatements();
                                for (PsiStatement statement2 : statements2) {
                                    if (statement2 instanceof PsiSwitchLabelStatement && !((PsiSwitchLabelStatement) statement2).isDefaultCase()) {
                                        PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) statement2;
                                        for (PsiExpression switchCaseExpression : switchCase.getCaseValues().getExpressions()) {
                                            PsiReferenceExpression comparedVariable = null;
                                            if (switchCaseExpression instanceof PsiReferenceExpression) {
                                                comparedVariable = (PsiReferenceExpression) switchCaseExpression;
                                            }
                                            if (comparedVariable != null) {
                                                PsiElement switchCaseExpressionBinding = comparedVariable.resolve();
                                                if (switchCaseExpressionBinding instanceof PsiVariable) {
                                                    PsiVariable comparedVariableBinding = (PsiVariable) switchCaseExpressionBinding;
                                                    if (comparedVariableBinding instanceof PsiField
                                                            && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                            && !containsVariable(staticFields, comparedVariable)
                                                            && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                                        if (!containsStaticFieldKey(comparedVariable))
                                                            additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
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

                List<PsiExpression> infixExpressions = expressionExtractor.getInfixExpressions(statement);
                for (PsiExpression expression : infixExpressions) {
                    PsiBinaryExpression infixExpression = (PsiBinaryExpression) expression;
                    PsiExpression leftOperand = infixExpression.getLOperand();
                    PsiExpression rightOperand = infixExpression.getROperand();
                    PsiReferenceExpression accessedVariable = null;
                    PsiReferenceExpression comparedVariable = null;
                    boolean typeLocalVariableIsFound = false;
                    if (leftOperand instanceof PsiReferenceExpression) {
                        accessedVariable = (PsiReferenceExpression) leftOperand;
                    }
                    if (rightOperand instanceof PsiReferenceExpression) {
                        comparedVariable = (PsiReferenceExpression) rightOperand;
                    }

                    if (accessedVariable != null) {
                        PsiElement leftOperandBinding = accessedVariable.resolve();
                        if (leftOperandBinding instanceof PsiVariable) {
                            PsiVariable accessedVariableBinding = (PsiVariable) leftOperandBinding;
                            if (typeCheckElimination.getTypeLocalVariable().equals(accessedVariableBinding)) {
                                typeLocalVariableIsFound = true;
                                if (comparedVariable != null) {
                                    PsiElement rightOperandBinding = comparedVariable.resolve();
                                    if (rightOperandBinding instanceof PsiVariable) {
                                        PsiVariable comparedVariableBinding = (PsiVariable) rightOperandBinding;
                                        if (comparedVariableBinding instanceof PsiField
                                                && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                && !containsVariable(staticFields, comparedVariable)
                                                && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                            if (!containsStaticFieldKey(comparedVariable))
                                                additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!typeLocalVariableIsFound) {
                        if (rightOperand instanceof PsiReferenceExpression) {
                            accessedVariable = (PsiReferenceExpression) rightOperand;
                        }
                        if (leftOperand instanceof PsiReferenceExpression) {
                            comparedVariable = (PsiReferenceExpression) leftOperand;
                        }

                        if (accessedVariable != null) {
                            PsiElement rightOperandBinding = accessedVariable.resolve();
                            if (rightOperandBinding instanceof PsiVariable) {
                                PsiVariable accessedVariableBinding = (PsiVariable) rightOperandBinding;
                                if (typeCheckElimination.getTypeLocalVariable().equals(accessedVariableBinding)) {
                                    if (comparedVariable != null) {
                                        PsiElement leftOperandBinding = comparedVariable.resolve();
                                        if (leftOperandBinding instanceof PsiVariable) {
                                            PsiVariable comparedVariableBinding = (PsiVariable) leftOperandBinding;
                                            if (comparedVariableBinding instanceof PsiField
                                                    && comparedVariableBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)
                                                    && !containsVariable(staticFields, comparedVariable)
                                                    && comparedVariableBinding.getType().equals(accessedVariableBinding.getType())) {
                                                if (!containsStaticFieldKey(comparedVariable))
                                                    additionalStaticFieldMap.put((PsiField) comparedVariableBinding, generateSubclassName(comparedVariableBinding));
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

    private boolean containsStaticFieldKey(PsiReferenceExpression simpleName) {
        for (PsiField keySimpleName : additionalStaticFieldMap.keySet()) {
            if (keySimpleName.equals(simpleName.resolve()))
                return true;
        }
        return false;
    }

    private boolean containsVariable(List<PsiField> staticFields, PsiReferenceExpression variable) {
        for (PsiField staticField : staticFields) {
            if (staticField.equals(variable.resolve()))
                return true;
        }
        return false;
    }

    private String generateSubclassName(PsiVariable variable) {
        String subclassName = "";
        StringTokenizer tokenizer = new StringTokenizer(variable.getName(), "_");
        while (tokenizer.hasMoreTokens()) {
            String tempName = tokenizer.nextToken().toLowerCase().toString();
            subclassName += tempName.subSequence(0, 1).toString().toUpperCase() +
                    tempName.subSequence(1, tempName.length()).toString();
        }
        return subclassName;
    }

    private String commonSubstring(List<String> subclassNames) {
        Map<String, Integer> rankMap = new LinkedHashMap<>();
        for (int i = 0; i < subclassNames.size(); i++) {
            for (int j = i + 1; j < subclassNames.size(); j++) {
                List<String> commonSubstrings = commonSubstrings(subclassNames.get(i), subclassNames.get(j));
                for (String s : commonSubstrings) {
                    if (rankMap.containsKey(s)) {
                        rankMap.put(s, rankMap.get(s) + 1);
                    } else {
                        rankMap.put(s, 1);
                    }
                }
            }
        }
        int maxRank = 0;
        String mostCommonSubstring = null;
        for (String s : rankMap.keySet()) {
            int rank = rankMap.get(s);
            if (rank > maxRank) {
                maxRank = rank;
                mostCommonSubstring = s;
            }
        }
        return mostCommonSubstring;
    }

    private List<String> commonSubstrings(String s1, String s2) {
        List<String> commonSubstrings = new ArrayList<String>();
        int m = s1.length();
        int n = s2.length();
        int[][] num = new int[m][n];
        int maxlen = 0;
        int lastSubsBegin = 0;
        StringBuilder sequence = new StringBuilder();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (s1.charAt(i) != s2.charAt(j))
                    num[i][j] = 0;
                else {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];
                    if (num[i][j] > maxlen) {
                        maxlen = num[i][j];
                        int thisSubsBegin = i - num[i][j] + 1;
                        if (lastSubsBegin == thisSubsBegin)
                            sequence.append(s1.charAt(i));
                        else {
                            lastSubsBegin = thisSubsBegin;
                            commonSubstrings.add(sequence.toString());
                            sequence = new StringBuilder();
                            sequence.append(s1, lastSubsBegin, i + 1);
                        }
                    }
                }
            }
        }
        commonSubstrings.add(sequence.toString());
        return commonSubstrings;
    }

//	public CompilationUnit getSourceCompilationUnit() {
//		return sourceCompilationUnit;
//	}
//
	public String getAbstractClassName() {
		return abstractClassName;
	}

    public String getTypeVariableSimpleName() {
		return typeCheckElimination.getTypeVariableSimpleName();
	}

	public Set<Map.Entry<PsiField, String>> getStaticFieldMapEntrySet() {
		return staticFieldMap.entrySet();
	}

	public Set<Map.Entry<PsiField, String>> getAdditionalStaticFieldMapEntrySet() {
		return additionalStaticFieldMap.entrySet();
	}

	public void setTypeNameForNamedConstant(PsiField namedConstant, String typeName) {
		if(staticFieldMap.containsKey(namedConstant)) {
			staticFieldMap.put(namedConstant, typeName);
		}
		else if(additionalStaticFieldMap.containsKey(namedConstant)) {
			additionalStaticFieldMap.put(namedConstant, typeName);
		}
		else {
			abstractClassName = typeName;
		}
	}

    public String getTypeNameForNamedConstant(PsiField namedConstant) {
        if (staticFieldMap.containsKey(namedConstant)) {
            return staticFieldMap.get(namedConstant);
        } else if (additionalStaticFieldMap.containsKey(namedConstant)) {
            return additionalStaticFieldMap.get(namedConstant);
        }
        return null;
    }

//	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
//			throws CoreException, OperationCanceledException {
//		RefactoringStatus status= new RefactoringStatus();
//		try {
//			pm.beginTask("Checking preconditions...", 1);
//			if(typeCheckElimination.getTypeField() != null) {
//				modifyTypeFieldAssignmentsInContextClass(false);
//				modifyTypeFieldAccessesInContextClass(false);
//			}
//			else if(typeCheckElimination.getTypeLocalVariable() != null) {
//				identifyTypeLocalVariableAssignmentsInTypeCheckMethod();
//				identifyTypeLocalVariableAccessesInTypeCheckMethod();
//			}
//		} finally {
//			pm.done();
//		}
//		return status;
//	}
}
