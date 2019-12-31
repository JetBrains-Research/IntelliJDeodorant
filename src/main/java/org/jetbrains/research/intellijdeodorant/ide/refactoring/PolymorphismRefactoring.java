package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;
import org.jetbrains.research.intellijdeodorant.core.ast.util.MethodDeclarationUtility;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class PolymorphismRefactoring {
    protected PsiFile sourceFile;
    protected PsiClass sourceTypeDeclaration;
    protected Project project;
    protected TypeCheckElimination typeCheckElimination;
    protected PsiElementFactory elementFactory;
    protected CodeStyleManager codeStyleManager;

    final protected PsiJavaToken semicolon;

    public PolymorphismRefactoring(PsiFile sourceFile,
                                   Project project,
                                   PsiClass sourceTypeDeclaration,
                                   TypeCheckElimination typeCheckElimination) {
        this.sourceFile = sourceFile;
        this.sourceTypeDeclaration = sourceTypeDeclaration;
        this.typeCheckElimination = typeCheckElimination;
        elementFactory = PsiElementFactory.SERVICE.getInstance(project);
        codeStyleManager = CodeStyleManager.getInstance(project);
        this.project = project;
        semicolon = (PsiJavaToken) elementFactory.createStatementFromText(";", null).getFirstChild();
    }

    public abstract void apply();

    protected void modifySourceMethodInvocationsInSubclass(List<PsiExpression> oldMethodInvocations,
                                                           List<PsiExpression> newMethodInvocations,
                                                           Set<PsiMethod> accessedMethods,
                                                           Set<PsiMethod> superAccessedMethods) {
        int j = 0;
        for (PsiExpression expression : newMethodInvocations) {
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) expression;
                PsiMethodCallExpression oldMethodInvocation = (PsiMethodCallExpression) oldMethodInvocations.get(j);
                for (PsiMethod methodDeclaration : accessedMethods) {
                    if (methodDeclaration.equals(oldMethodInvocation.resolveMethod())) {
						addQualifierToSourceTypeMethodCalls(newMethodInvocation, methodDeclaration);
						break;
					}
                }
                for (PsiMethod superMethodBinding : superAccessedMethods) {
                    if (superMethodBinding.equals(oldMethodInvocation.resolveMethod())) {
                        PsiExpression qualifierExpression = oldMethodInvocation.getMethodExpression().getQualifierExpression();
                        if (qualifierExpression == null || qualifierExpression instanceof PsiThisExpression) {
							addQualifierToSourceTypeMethodCalls(newMethodInvocation, superMethodBinding);
							break;
						}
                    }
                }
            }
            j++;
        }
    }

	private void addQualifierToSourceTypeMethodCalls(PsiMethodCallExpression newMethodInvocation, PsiMethod methodDeclaration) {
		String invokerName = sourceTypeDeclaration.getName();
		if (!methodDeclaration.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
			invokerName = invokerName.substring(0, 1).toLowerCase() + invokerName.substring(1);
		}
		newMethodInvocation.getMethodExpression().setQualifierExpression(elementFactory.createExpressionFromText(invokerName, null));
	}

	protected void replaceThisExpressionWithContextParameterInMethodInvocationArguments(List<PsiExpression> oldMethodInvocations,
                                                                                        List<PsiExpression> newMethodInvocations) {
        for (int expressionIndex = 0; expressionIndex < oldMethodInvocations.size(); expressionIndex++) {
            PsiExpression newExpression = newMethodInvocations.get(expressionIndex);
            PsiExpression oldExpression = oldMethodInvocations.get(expressionIndex);
            if (newExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression newMethodInvocation = (PsiMethodCallExpression) newExpression;
                PsiMethodCallExpression oldMethodInvocation = (PsiMethodCallExpression) oldExpression;
                PsiExpression[] newArguments = newMethodInvocation.getArgumentList().getExpressions();
                PsiExpression[] oldArguments = oldMethodInvocation.getArgumentList().getExpressions();
                for (int argumentIndex = 0; argumentIndex < oldArguments.length; argumentIndex++) {
                    PsiExpression oldArgument = oldArguments[argumentIndex];
                    PsiExpression newArgument = newArguments[argumentIndex];
                    if (oldArgument instanceof PsiThisExpression) {
                        String parameterName = sourceTypeDeclaration.getName();
                        parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
                        newArgument.replace(elementFactory.createExpressionFromText(parameterName, null));
                    }
                }
            }
        }
    }

    protected void replaceThisExpressionWithContextParameterInClassInstanceCreationArguments(PsiStatement newStatement) {
        ExpressionExtractor expressionExtractor = new ExpressionExtractor();
        List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(newStatement);
        for (PsiExpression creation : classInstanceCreations) {
            PsiNewExpression classInstanceCreation = (PsiNewExpression) creation;
            PsiExpressionList argumentList = classInstanceCreation.getArgumentList();
            PsiExpression[] arguments = argumentList != null ? argumentList.getExpressions() : PsiExpression.EMPTY_ARRAY;
            for (PsiExpression argument : arguments) {
                if (argument instanceof PsiThisExpression) {
                    String parameterName = sourceTypeDeclaration.getName();
                    parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
                    argument.replace(elementFactory.createExpressionFromText(
                            parameterName,
                            null
                    ));
                }
            }
        }
    }

    protected void modifySourceVariableInstructionsInSubclass(List<PsiExpression> oldVariableInstructions,
                                                              List<PsiExpression> newVariableInstructions,
                                                              Set<PsiField> accessedFields,
                                                              Set<PsiField> assignedFields,
                                                              Set<PsiField> superAccessedFields,
                                                              Set<PsiField> superAssignedFields) {

        Set<PsiField> accessedFieldBindings = new LinkedHashSet<>();
        accessedFieldBindings.addAll(accessedFields);
        accessedFieldBindings.addAll(superAccessedFields);
        Set<PsiField> assignedFieldBindings = new LinkedHashSet<>();
        assignedFieldBindings.addAll(assignedFields);
        assignedFieldBindings.addAll(superAssignedFields);

        for (int i = 0; i < newVariableInstructions.size(); i++) {
            PsiReferenceExpression newSimpleName = (PsiReferenceExpression) newVariableInstructions.get(i);
            PsiReferenceExpression oldSimpleName = (PsiReferenceExpression) oldVariableInstructions.get(i);
            PsiExpression newParentExpression = newSimpleName;
            PsiExpression oldParentExpression = oldSimpleName;

            if (newParentExpression.getParent() instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression newAssignment = (PsiAssignmentExpression) newParentExpression.getParent();
                PsiAssignmentExpression oldAssignment = (PsiAssignmentExpression) oldParentExpression.getParent();
                PsiExpression newLeftHandSide = newAssignment.getLExpression();
                PsiExpression oldLeftHandSide = oldAssignment.getLExpression();
                PsiReferenceExpression newLeftHandSideName = null;
                PsiReferenceExpression oldLeftHandSideName = null;
                if (newLeftHandSide instanceof PsiReferenceExpression) {
                    newLeftHandSideName = (PsiReferenceExpression) newLeftHandSide;
                    oldLeftHandSideName = (PsiReferenceExpression) oldLeftHandSide;
                }
                PsiExpression newRightHandSide = newAssignment.getRExpression();
                PsiExpression oldRightHandSide = oldAssignment.getRExpression();
                PsiReferenceExpression newRightHandSideName = null;
                PsiReferenceExpression oldRightHandSideName = null;
                if (newRightHandSide instanceof PsiReferenceExpression) {
                    newRightHandSideName = (PsiReferenceExpression) newRightHandSide;
                    oldRightHandSideName = (PsiReferenceExpression) oldRightHandSide;
                }
                String invokerName = sourceTypeDeclaration.getName();
                invokerName = invokerName.substring(0, 1).toLowerCase() + invokerName.substring(1);

                if (newLeftHandSideName != null && newLeftHandSideName.equals(newSimpleName)) {
                    for (PsiField assignedFieldBinding : assignedFieldBindings) {
                        if (assignedFieldBinding.equals(oldLeftHandSideName.resolve())) {
                            PsiMethodCallExpression setterMethodInvocation = generateSetterInvocation(superAssignedFields, invokerName, assignedFieldBinding);

                            PsiBinaryExpression infixArgument = null;
                            if (!newAssignment.getOperationTokenType().equals(JavaTokenType.EQ)) {
                                String getterMethodName = getGetterName(superAccessedFields, assignedFieldBinding);
                                infixArgument = (PsiBinaryExpression) elementFactory.createExpressionFromText(
                                        "a" + newAssignment.getOperationSign().getText() + "b",
                                        null
                                );
                                infixArgument.getLOperand().replace(
                                        elementFactory.createExpressionFromText(invokerName + "." + getterMethodName + "()", null)
                                );
                            }
                            PsiExpressionList methodInvocationArgumentsRewrite = setterMethodInvocation.getArgumentList();
                            if (oldRightHandSideName != null) {
                                boolean accessedFieldFound = false;
                                for (PsiField accessedFieldBinding : accessedFieldBindings) {
                                    if (accessedFieldBinding.equals(oldRightHandSideName.resolve())) {
                                        if (accessedFieldBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                            String qualifier = accessedFieldBinding.getContainingClass().getName();
                                            PsiReferenceExpression reference = (PsiReferenceExpression) elementFactory.createExpressionFromText(
                                                    qualifier + "." + newRightHandSideName.getReferenceName(),
                                                    null
                                            );
                                            if (infixArgument != null) {
                                                infixArgument.getROperand().replace(reference);
                                                methodInvocationArgumentsRewrite.add(infixArgument);
                                            } else {
                                                methodInvocationArgumentsRewrite.add(reference);
                                            }
                                            if (accessedFieldBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                                                setPublicModifierToSourceField(accessedFieldBinding);
                                            }
                                        } else {
                                            String rightHandMethodName = getGetterName(superAccessedFields, accessedFieldBinding);
                                            PsiMethodCallExpression getterCallExpression = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                                                    invokerName + "." + rightHandMethodName + "()",
                                                    null
                                            );
                                            if (infixArgument != null) {
                                                infixArgument.getROperand().replace(getterCallExpression);
                                                methodInvocationArgumentsRewrite.add(infixArgument);
                                            } else {
                                                methodInvocationArgumentsRewrite.add(getterCallExpression);
                                            }
                                        }
                                        accessedFieldFound = true;
                                        break;
                                    }
                                }
                                if (!accessedFieldFound) {
                                    if (infixArgument != null) {
                                        infixArgument.getROperand().replace(newAssignment.getRExpression());
                                        methodInvocationArgumentsRewrite.add(infixArgument);
                                    } else {
                                        methodInvocationArgumentsRewrite.add(newAssignment.getRExpression());
                                    }
                                }
                            } else {
                                if (infixArgument != null) {
                                    infixArgument.getROperand().replace(newAssignment.getRExpression());
                                    methodInvocationArgumentsRewrite.add(infixArgument);
                                } else {
                                    methodInvocationArgumentsRewrite.add(newAssignment.getRExpression());
                                }
                            }
                            newAssignment.replace(setterMethodInvocation);
                            break;
                        }
                    }
                }

                if (newRightHandSideName != null && newRightHandSideName.equals(newSimpleName)) {
                    for (PsiField accessedFieldBinding : accessedFieldBindings) {
                        if (accessedFieldBinding.equals(oldRightHandSideName.resolve())) {
                            if (accessedFieldBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                String qualifier = accessedFieldBinding.getContainingClass().getName();
                                newSimpleName.replace(elementFactory.createExpressionFromText(
                                        qualifier + "." + newSimpleName.getReferenceName(),
                                        null
                                ));

                                if (accessedFieldBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                                    setPublicModifierToSourceField(accessedFieldBinding);
                                }
                            } else {
                                String rightHandMethodName = getGetterName(superAccessedFields, accessedFieldBinding);
                                newAssignment.getRExpression().replace(elementFactory.createExpressionFromText(
                                        invokerName + '.' + rightHandMethodName + "()",
                                        null
                                ));
                            }
                            break;
                        }
                    }
                }
            } else if (newParentExpression.getParent() instanceof PsiPostfixExpression) { // TODO: does not work for {a = b++} (even in the original plugin)
                PsiUnaryExpression newPostfixExpression = (PsiPostfixExpression) newParentExpression.getParent();
                PsiPostfixExpression oldPostfixExpression = (PsiPostfixExpression) oldParentExpression.getParent();
                PsiReferenceExpression newOperandReference = null;
                PsiReferenceExpression oldOperandReference = null;
                if (newPostfixExpression.getOperand() instanceof PsiReferenceExpression) {
                    newOperandReference = (PsiReferenceExpression) newPostfixExpression.getOperand();
                    oldOperandReference = (PsiReferenceExpression) oldPostfixExpression.getOperand();
                }
                String invokerName = sourceTypeDeclaration.getName();
                invokerName = invokerName.substring(0, 1).toLowerCase() + invokerName.substring(1);
                if (newOperandReference != null && newOperandReference.equals(newSimpleName)) {
                    handleBasicUnaryExpression(
                            superAccessedFields,
                            superAssignedFields,
                            assignedFieldBindings,
                            newPostfixExpression,
                            oldOperandReference,
                            invokerName
                    );
                }
            } else if (newParentExpression.getParent() instanceof PsiPrefixExpression) {
                PsiUnaryExpression newPrefixExpression = (PsiPrefixExpression) newParentExpression.getParent();
                PsiPrefixExpression oldPrefixExpression = (PsiPrefixExpression) oldParentExpression.getParent();
                PsiReferenceExpression newOperandSimpleName = null;
                PsiReferenceExpression oldOperandSimpleName = null;
                if (newPrefixExpression.getOperand() instanceof PsiReferenceExpression) {
                    newOperandSimpleName = (PsiReferenceExpression) newPrefixExpression.getOperand();
                    oldOperandSimpleName = (PsiReferenceExpression) oldPrefixExpression.getOperand();
                }
                String invokerName = sourceTypeDeclaration.getName();
                invokerName = invokerName.substring(0, 1).toLowerCase() + invokerName.substring(1);
                if (newPrefixExpression.getOperationTokenType().equals(JavaTokenType.PLUSPLUS) ||
                        newPrefixExpression.getOperationTokenType().equals(JavaTokenType.MINUSMINUS)) {
                    if (newOperandSimpleName != null && newOperandSimpleName.equals(newSimpleName)) {
                        handleBasicUnaryExpression(
                                superAccessedFields,
                                superAssignedFields,
                                assignedFieldBindings,
                                newPrefixExpression,
                                oldOperandSimpleName,
                                invokerName
                        );
                    }
                } else {
                    if (newOperandSimpleName != null && newOperandSimpleName.equals(newSimpleName)) {
                        for (PsiField accessedFieldBinding : accessedFieldBindings) {
                            if (accessedFieldBinding.equals(oldOperandSimpleName.resolve())) {
                                if (accessedFieldBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                                    String qualifier = accessedFieldBinding.getContainingClass().getName();
                                    newOperandSimpleName.replace(elementFactory.createExpressionFromText(
                                            qualifier + "." + newOperandSimpleName.getReferenceName(),
                                            null
                                    ));
                                    if (accessedFieldBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                                        setPublicModifierToSourceField(accessedFieldBinding);
                                    }
                                } else {
                                    String methodName = getGetterName(superAccessedFields, accessedFieldBinding);
                                    PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                                            invokerName + "." + methodName + "()",
                                            null
                                    );
                                    newOperandSimpleName.replace(methodInvocation);

                                }
                            }
                        }
                    }
                }
            } else {
                for (PsiField accessedFieldBinding : accessedFieldBindings) {
                    if (accessedFieldBinding.equals(oldSimpleName.resolve())) {
                        if (accessedFieldBinding.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                            String qualifier = accessedFieldBinding.getName();
                            newSimpleName.replace(elementFactory.createExpressionFromText(
                                    qualifier + "." + newSimpleName.getReferenceName(),
                                    null
                            ));
                            if (accessedFieldBinding.getContainingClass().equals(sourceTypeDeclaration)) {
                                setPublicModifierToSourceField(accessedFieldBinding);
                            }
                        } else {
                            String methodName = getGetterName(superAccessedFields, accessedFieldBinding);
                            String invokerName = sourceTypeDeclaration.getName();
                            invokerName = invokerName.substring(0, 1).toLowerCase() + invokerName.substring(1);
                            PsiMethodCallExpression getterInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
                                    invokerName + "." + methodName + "()",
                                    null
                            );
                            newSimpleName.replace(getterInvocation);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void handleBasicUnaryExpression(Set<PsiField> superAccessedFields, Set<PsiField> superAssignedFields, Set<PsiField> assignedFieldBindings, PsiUnaryExpression newUnaryExpression, PsiReferenceExpression oldOperandReference, String invokerName) {
        for (PsiField assignedFieldBinding : assignedFieldBindings) {
            if (assignedFieldBinding.equals(oldOperandReference.resolve())) {
                PsiMethodCallExpression setterMethodInvocation = generateSetterInvocation(superAssignedFields, invokerName, assignedFieldBinding);

                String getterMethodName = getGetterName(superAccessedFields, assignedFieldBinding);

                String infixOperator = null;
                if (newUnaryExpression.getOperationTokenType().equals(JavaTokenType.PLUSPLUS)) {
                    infixOperator = "+";
                } else if (newUnaryExpression.getOperationTokenType().equals(JavaTokenType.MINUSMINUS)) {
                    infixOperator = "-";
                }
                PsiBinaryExpression infixArgument = (PsiBinaryExpression) elementFactory.createExpressionFromText(
                        invokerName + "." + getterMethodName + "()" + infixOperator + "1",
                        null
                );
                PsiExpressionList setterMethodInvocationArgumentsRewrite = setterMethodInvocation.getArgumentList();
                setterMethodInvocationArgumentsRewrite.add(infixArgument);
                newUnaryExpression.replace(setterMethodInvocation);
            }
        }
    }

    private String getGetterName(Set<PsiField> superAccessedFields, PsiField assignedFieldBinding) {
		PsiMethod getterMethodBinding = null;
		if (superAccessedFields.contains(assignedFieldBinding)) {
			getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(assignedFieldBinding);
		} else {
			getterMethodBinding = findGetterMethodInContext(assignedFieldBinding);
		}
		String getterMethodName;
		if (getterMethodBinding != null) {
			getterMethodName = getterMethodBinding.getName();
		} else {
			getterMethodName = assignedFieldBinding.getName();
			getterMethodName = "get" + getterMethodName.substring(0, 1).toUpperCase() + getterMethodName.substring(1);
		}
		return getterMethodName;
	}

	private PsiMethodCallExpression generateSetterInvocation(Set<PsiField> superAssignedFields, String invokerName, PsiField assignedFieldBinding) {
		PsiMethod setterMethodBinding = null;
		if (superAssignedFields.contains(assignedFieldBinding)) {
			setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(assignedFieldBinding);
		} else {
			setterMethodBinding = findSetterMethodInContext(assignedFieldBinding);
		}
		String leftHandMethodName;
		if (setterMethodBinding != null) {
			leftHandMethodName = setterMethodBinding.getName();
		} else {
			leftHandMethodName = assignedFieldBinding.getName();
			leftHandMethodName = "set" + leftHandMethodName.substring(0, 1).toUpperCase() + leftHandMethodName.substring(1);
		}

		PsiMethodCallExpression setterInvocation = (PsiMethodCallExpression) elementFactory.createExpressionFromText(
				invokerName + "." + leftHandMethodName + "()",
				null
		);
		return setterInvocation;
	}

	private void setPublicModifierToSourceField(PsiField variableBinding) {
        PsiUtil.setModifierProperty(variableBinding, PsiModifier.PUBLIC, true);
    }

    protected PsiMethod findSetterMethodInContext(PsiField fieldBinding) {
        PsiMethod[] contextMethods = sourceTypeDeclaration.getMethods();
        for (PsiMethod methodDeclaration : contextMethods) {
            PsiElement simpleName = MethodDeclarationUtility.isSetter(methodDeclaration);
            if (fieldBinding.equals(simpleName)) {
                return methodDeclaration;
            }
        }
        return null;
    }

    protected PsiMethod findGetterMethodInContext(PsiField fieldBinding) {
        PsiMethod[] contextMethods = sourceTypeDeclaration.getMethods();
        for (PsiMethod methodDeclaration : contextMethods) {
            PsiExpression simpleName = MethodDeclarationUtility.isGetter(methodDeclaration);
            if (simpleName instanceof PsiReferenceExpression && fieldBinding.equals(((PsiReferenceExpression) simpleName).resolve())) {
                return methodDeclaration;
            }
        }
        return null;
    }


    protected void generateGettersForAccessedFields() {
        Set<PsiField> accessedFields = new LinkedHashSet<>();
        accessedFields.addAll(typeCheckElimination.getAccessedFields());
        accessedFields.addAll(typeCheckElimination.getSuperAccessedFields());
        for (PsiField fragment : accessedFields) {
            if (!fragment.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                PsiMethod getterMethodBinding = null;
                if (typeCheckElimination.getSuperAccessedFields().contains(fragment)) {
                    for (PsiField fieldBinding : typeCheckElimination.getSuperAccessedFieldBindings()) {
                        if (fieldBinding.equals(fragment)) {
                            getterMethodBinding = typeCheckElimination.getGetterMethodBindingOfSuperAccessedField(fieldBinding);
                            break;
                        }
                    }
                } else {
                    getterMethodBinding = findGetterMethodInContext(fragment);
                }
                if (getterMethodBinding == null) {
                    PsiMethod getter = PropertyUtil.generateGetterPrototype(fragment);
                    fragment.getContainingClass().add(getter);
                }
            }
        }
    }

    protected void generateSettersForAssignedFields() {
        Set<PsiField> assignedFields = new LinkedHashSet<>();
        assignedFields.addAll(typeCheckElimination.getAssignedFields());
        assignedFields.addAll(typeCheckElimination.getSuperAssignedFields());
        for (PsiField fragment : assignedFields) {
            PsiMethod setterMethodBinding = null;
            if (typeCheckElimination.getSuperAssignedFields().contains(fragment)) {
                for (PsiField fieldBinding : typeCheckElimination.getSuperAssignedFieldBindings()) {
                    if (fieldBinding.equals(fragment)) {
                        setterMethodBinding = typeCheckElimination.getSetterMethodBindingOfSuperAssignedField(fieldBinding);
                        break;
                    }
                }
            } else {
                setterMethodBinding = findSetterMethodInContext(fragment);
            }
            if (setterMethodBinding == null) {
                PsiMethod setter = PropertyUtil.generateSetterPrototype(fragment);
                fragment.getContainingClass().add(setter);
            }
        }
    }

    protected void setPublicModifierToAccessedMethods() {
        for (PsiMethod methodDeclaration : typeCheckElimination.getAccessedMethods()) {
            PsiUtil.setModifierProperty(methodDeclaration, PsiModifier.PUBLIC, true);
        }
    }

    protected PsiExpression constructExpression(DefaultMutableTreeNode node) {
        Object object = node.getUserObject();
        if (object instanceof PsiJavaToken) {
            PsiJavaToken operator = (PsiJavaToken) object;
            DefaultMutableTreeNode leftChild = (DefaultMutableTreeNode) node.getChildAt(0);
            DefaultMutableTreeNode rightChild = (DefaultMutableTreeNode) node.getChildAt(1);
            PsiExpression leftExpression = constructExpression(leftChild);
            PsiExpression rightExpression = constructExpression(rightChild);
            PsiBinaryExpression binaryExpression = (PsiBinaryExpression)
                    elementFactory.createExpressionFromText("a" + operator.getText() + "b", null);
            binaryExpression.getLOperand().replace(leftExpression);
            binaryExpression.getROperand().replace(rightExpression);
            return binaryExpression;
        } else if (object instanceof PsiExpression) {
            return (PsiExpression) object;
        }
        return null;
    }

    protected boolean sourceTypeRequiredForExtraction() {
        return Stream.of(
                typeCheckElimination.getAccessedFields(),
                typeCheckElimination.getAssignedFields(),
                typeCheckElimination.getAccessedMethods(),
                typeCheckElimination.getAccessedMethods(),
                typeCheckElimination.getSuperAccessedFieldBindings(),
                typeCheckElimination.getSuperAssignedFieldBindings()
        ).flatMap(Collection::stream).anyMatch(element ->
                element.getModifierList() == null || !element.getModifierList().hasModifierProperty(PsiModifier.STATIC)
        );
    }

    protected void addImports(PsiImportList importList, Set<PsiType> requiredImportDeclarations) {
        for (PsiType typeBinding : requiredImportDeclarations) {
            PsiClass resolvedClass = PsiUtil.resolveClassInType(typeBinding);
            if (resolvedClass != null && resolvedClass.getContainingClass() == null && !PsiUtil.getPackageName(resolvedClass).isEmpty()) {
                importList.add(elementFactory.createImportStatement(resolvedClass));
            }
        }
    }

    protected static PsiImportList getPsiImportList(PsiFile classFile) {
        PsiElement[] children = classFile.getChildren();
        for (PsiElement child : children) {
            if (child instanceof PsiImportList) {
                return (PsiImportList) child;
            }
        }
        // will not happen
        return null;
    }

    public Project getProject() {
        return project;
    }

    public PsiClass getSourceTypeDeclaration() {
        return sourceTypeDeclaration;
    }

    public PsiFile getSourceFile() {
        return sourceFile;
    }
}
