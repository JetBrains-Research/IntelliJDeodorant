package refactoring;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtil;
import core.ast.decomposition.CompositeStatementObject;
import core.ast.inheritance.InheritanceTree;
import core.ast.util.ExpressionExtractor;
import core.ast.util.StatementExtractor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class TypeCheckElimination implements Comparable<TypeCheckElimination> {
	private Map<PsiExpression, ArrayList<PsiStatement>> typeCheckMap;
	private ArrayList<PsiStatement> defaultCaseStatements;
	private Map<PsiExpression, List<PsiField>> staticFieldMap;
	private Map<PsiExpression, List<PsiType>> subclassTypeMap;
	private PsiField typeField;
	private PsiMethod typeFieldGetterMethod;
	private PsiMethod typeFieldSetterMethod;
	private PsiStatement typeCheckCodeFragment;
	private CompositeStatementObject typeCheckCompositeStatement;
	private PsiMethod typeCheckMethod;
	private PsiClass typeCheckClass;
//	private IFile typeCheckIFile;
	private LinkedHashSet<PsiField> additionalStaticFields;
	private LinkedHashSet<PsiField> accessedFields;
	private LinkedHashSet<PsiField> assignedFields;
	private LinkedHashMap<PsiField, PsiMethod> superAccessedFieldMap;
	private LinkedHashMap<PsiField, PsiMethod> superAccessedFieldBindingMap;
	private LinkedHashMap<PsiField, PsiMethod> superAssignedFieldMap;
	private LinkedHashMap<PsiField, PsiMethod> superAssignedFieldBindingMap;
	private LinkedHashSet<PsiParameter> accessedParameters;
	private LinkedHashSet<PsiParameter> assignedParameters;
	private LinkedHashSet<PsiVariable> accessedLocalVariables;
	private LinkedHashSet<PsiVariable> assignedLocalVariables;
	private LinkedHashSet<PsiMethod> accessedMethods;
	private LinkedHashSet<PsiMethod> superAccessedMethods;
	private PsiVariable typeLocalVariable;
	private PsiMethodCallExpression typeMethodInvocation;
	private PsiField foreignTypeField;
	private InheritanceTree existingInheritanceTree;
	private InheritanceTree inheritanceTreeMatchingWithStaticTypes;
	private Map<PsiField, String> staticFieldSubclassTypeMap;
	private Map<PsiExpression, DefaultMutableTreeNode> remainingIfStatementExpressionMap;
	private String abstractMethodName;
	private volatile int hashCode = 0;
	private int groupSizeAtClassLevel;
	private double averageNumberOfStatements;
	private Integer userRate;
	
	public TypeCheckElimination() {
		this.typeCheckMap = new LinkedHashMap<>();
		this.defaultCaseStatements = new ArrayList<>();
		this.staticFieldMap = new LinkedHashMap<>();
		this.subclassTypeMap = new LinkedHashMap<>();
		this.typeField = null;
		this.typeFieldGetterMethod = null;
		this.typeFieldSetterMethod = null;
		this.typeCheckCodeFragment = null;
		this.typeCheckMethod = null;
		this.typeCheckClass = null;
		this.additionalStaticFields = new LinkedHashSet<>();
		this.accessedFields = new LinkedHashSet<>();
		this.assignedFields = new LinkedHashSet<>();
		this.superAccessedFieldMap = new LinkedHashMap<>();
		this.superAccessedFieldBindingMap = new LinkedHashMap<>();
		this.superAssignedFieldMap = new LinkedHashMap<>();
		this.superAssignedFieldBindingMap = new LinkedHashMap<>();
		this.accessedParameters = new LinkedHashSet<>();
		this.assignedParameters = new LinkedHashSet<>();
		this.accessedLocalVariables = new LinkedHashSet<>();
		this.assignedLocalVariables = new LinkedHashSet<>();
		this.accessedMethods = new LinkedHashSet<>();
		this.superAccessedMethods = new LinkedHashSet<>();
		this.typeLocalVariable = null;
		this.typeMethodInvocation = null;
		this.foreignTypeField = null;
		this.existingInheritanceTree = null;
		this.inheritanceTreeMatchingWithStaticTypes = null;
		this.staticFieldSubclassTypeMap = new LinkedHashMap<>();
		this.remainingIfStatementExpressionMap = new LinkedHashMap<>();
		this.abstractMethodName = null;
	}
	
	public void addTypeCheck(PsiExpression expression, PsiStatement statement) {
		if(typeCheckMap.containsKey(expression)) {
			ArrayList<PsiStatement> statements = typeCheckMap.get(expression);
			statements.add(statement);
		} else {
			ArrayList<PsiStatement> statements = new ArrayList<>();
			statements.add(statement);
			typeCheckMap.put(expression, statements);
		}
	}
	
	public void addEmptyTypeCheck(PsiExpression expression) {
		if(!typeCheckMap.containsKey(expression)) {
			ArrayList<PsiStatement> statements = new ArrayList<>();
			typeCheckMap.put(expression, statements);
		}
	}
	
	public boolean containsTypeCheckExpression(PsiExpression expression) {
		return typeCheckMap.containsKey(expression);
	}
	
	public void addDefaultCaseStatement(PsiStatement statement) {
		defaultCaseStatements.add(statement);
	}
	
	public void addStaticType(PsiExpression expression, List<PsiField> simpleNameGroup) {
		staticFieldMap.put(expression, simpleNameGroup);
	}
	
	public void addSubclassType(PsiExpression expression, List<PsiType> subclassTypeGroup) {
		subclassTypeMap.put(expression, subclassTypeGroup);
	}
	
	public void addRemainingIfStatementExpression(PsiExpression expression, DefaultMutableTreeNode root) {
		remainingIfStatementExpressionMap.put(expression, root);
	}
	
	public void addAdditionalStaticField(PsiField staticField) {
		additionalStaticFields.add(staticField);
	}
	
	public void addAccessedField(PsiField fragment) {
		accessedFields.add(fragment);
	}
	
	public void addAssignedField(PsiField fragment) {
		assignedFields.add(fragment);
	}
	
	public void addSuperAccessedField(PsiField fragment, PsiMethod method) {
		superAccessedFieldMap.put(fragment, method);
	}
	
	public void addSuperAccessedFieldBinding(PsiField variableBinding, PsiMethod methodBinding) {
		superAccessedFieldBindingMap.put(variableBinding, methodBinding);
	}
	
	public PsiMethod getGetterMethodBindingOfSuperAccessedField(PsiField variableBinding) {
		return superAccessedFieldBindingMap.get(variableBinding);
	}

	public Set<PsiField> getSuperAccessedFields() {
		return superAccessedFieldMap.keySet();
	}
	
	public Set<PsiField> getSuperAccessedFieldBindings() {
		return superAccessedFieldBindingMap.keySet();
	}
	
	public void addSuperAssignedField(PsiField fragment, PsiMethod method) {
		superAssignedFieldMap.put(fragment, method);
	}
	
	public void addSuperAssignedFieldBinding(PsiField variableBinding, PsiMethod methodBinding) {
		superAssignedFieldBindingMap.put(variableBinding, methodBinding);
	}

	public PsiMethod getSetterMethodBindingOfSuperAssignedField(PsiField variableBinding) {
		return superAssignedFieldBindingMap.get(variableBinding);
	}

	public Set<PsiField> getSuperAssignedFields() {
		return superAssignedFieldMap.keySet();
	}
	
	public Set<PsiField> getSuperAssignedFieldBindings() {
		return superAssignedFieldBindingMap.keySet();
	}
	
	public void addAccessedLocalVariable(PsiVariable fragment) {
		accessedLocalVariables.add(fragment);
	}

	public void addAssignedLocalVariable(PsiVariable fragment) {
		assignedLocalVariables.add(fragment);
	}
	
	public void addAccessedParameter(PsiParameter parameter) {
		accessedParameters.add(parameter);
	}
	
	public void addAssignedParameter(PsiParameter parameter) {
		assignedParameters.add(parameter);
	}
	
	public void addAccessedMethod(PsiMethod method) {
		accessedMethods.add(method);
	}

	public void addSuperAccessedMethod(PsiMethod method) {
		superAccessedMethods.add(method);
	}
	
	public LinkedHashSet<PsiVariable> getAccessedLocalVariables() {
		return accessedLocalVariables;
	}

	public Set<PsiField> getAccessedFields() {
		return accessedFields;
	}
	
	public Set<PsiField> getAssignedFields() {
		return assignedFields;
	}
	
	public Set<PsiParameter> getAccessedParameters() {
		return accessedParameters;
	}
	
	public Set<PsiMethod> getAccessedMethods() {
		return accessedMethods;
	}

	public Set<PsiMethod> getSuperAccessedMethods() {
		return superAccessedMethods;
	}
	
	public Set<PsiExpression> getTypeCheckExpressions() {
		return typeCheckMap.keySet();
	}
	
	public ArrayList<PsiStatement> getTypeCheckStatements(PsiExpression expression) {
		return typeCheckMap.get(expression);
	}
	
	public List<ArrayList<PsiStatement>> getTypeCheckStatements() {
		return new ArrayList<>(typeCheckMap.values());
	}
	
	public ArrayList<PsiStatement> getDefaultCaseStatements() {
		return defaultCaseStatements;
	}
	
	public List<PsiField> getStaticFields(PsiExpression expression) {
		return staticFieldMap.get(expression);
	}
	
	public List<PsiField> getStaticFields() {
		ArrayList<PsiField> staticFields = new ArrayList<>();
		for(PsiExpression expression : typeCheckMap.keySet()) {
			List<PsiField> simpleNameGroup = staticFieldMap.get(expression);
			if(simpleNameGroup != null) {
				for(PsiField simpleName : simpleNameGroup)
					staticFields.add(simpleName);
			}
		}
		return staticFields;
	}
	
	public Set<PsiField> getAdditionalStaticFields() {
		return additionalStaticFields;
	}
	
	public DefaultMutableTreeNode getRemainingIfStatementExpression(PsiExpression expression) {
		return remainingIfStatementExpressionMap.get(expression);
	}
	
	public PsiExpression getExpressionCorrespondingToTypeCheckStatementList(ArrayList<PsiStatement> statements) {
		for(PsiExpression expression : typeCheckMap.keySet()) {
			if(statements.equals(typeCheckMap.get(expression)))
				return expression;
		}
		return null;
	}
	
	public PsiField getTypeField() {
		return typeField;
	}
	
	public void setTypeField(PsiField typeField) {
		this.typeField = typeField;
	}
	
	public PsiMethod getTypeFieldGetterMethod() {
		return typeFieldGetterMethod;
	}

	public void setTypeFieldGetterMethod(PsiMethod typeFieldGetterMethod) {
		this.typeFieldGetterMethod = typeFieldGetterMethod;
	}

	public PsiMethod getTypeFieldSetterMethod() {
		return typeFieldSetterMethod;
	}

	public void setTypeFieldSetterMethod(PsiMethod typeFieldSetterMethod) {
		this.typeFieldSetterMethod = typeFieldSetterMethod;
	}

	public PsiStatement getTypeCheckCodeFragment() {
		return typeCheckCodeFragment;
	}

	public void setTypeCheckCodeFragment(PsiStatement typeCheckCodeFragment) {
		this.typeCheckCodeFragment = typeCheckCodeFragment;
	}

	public CompositeStatementObject getTypeCheckCompositeStatement() {
		return typeCheckCompositeStatement;
	}

	public void setTypeCheckCompositeStatement(CompositeStatementObject typeCheckCompositeStatement) {
		this.typeCheckCompositeStatement = typeCheckCompositeStatement;
	}

	public PsiMethod getTypeCheckMethod() {
		return typeCheckMethod;
	}

	public void setTypeCheckMethod(PsiMethod typeCheckMethod) {
		this.typeCheckMethod = typeCheckMethod;
		this.abstractMethodName = PsiUtil.getMemberQualifiedName(typeCheckMethod);
	}

	public PsiClass getTypeCheckClass() {
		return typeCheckClass;
	}

	public void setTypeCheckClass(PsiClass typeCheckClass) {
		this.typeCheckClass = typeCheckClass;
	}

//	public IFile getTypeCheckIFile() {
//		return typeCheckIFile;
//	}

//	public void setTypeCheckIFile(IFile typeCheckIFile) {
//		this.typeCheckIFile = typeCheckIFile;
//	}

	public PsiVariable getTypeLocalVariable() {
		return typeLocalVariable;
	}

	public void setTypeLocalVariable(PsiVariable typeLocalVariable) {
		this.typeLocalVariable = typeLocalVariable;
	}

	public PsiMethodCallExpression getTypeMethodInvocation() {
		return typeMethodInvocation;
	}

	public void setTypeMethodInvocation(PsiMethodCallExpression typeMethodInvocation) {
		this.typeMethodInvocation = typeMethodInvocation;
	}

	public PsiField getForeignTypeField() {
		return foreignTypeField;
	}

	public void setForeignTypeField(PsiField foreignTypeField) {
		this.foreignTypeField = foreignTypeField;
	}

	public InheritanceTree getExistingInheritanceTree() {
		return existingInheritanceTree;
	}

	public void setExistingInheritanceTree(InheritanceTree existingInheritanceTree) {
		this.existingInheritanceTree = existingInheritanceTree;
	}

	public InheritanceTree getInheritanceTreeMatchingWithStaticTypes() {
		return inheritanceTreeMatchingWithStaticTypes;
	}

	public void setInheritanceTreeMatchingWithStaticTypes(InheritanceTree inheritanceTree) {
		this.inheritanceTreeMatchingWithStaticTypes = inheritanceTree;
	}

	public void putStaticFieldSubclassTypeMapping(PsiField staticField, String subclassType) {
		staticFieldSubclassTypeMap.put(staticField, subclassType);
	}

	public boolean allTypeCheckingsContainStaticFieldOrSubclassType() {
		return (typeCheckMap.keySet().size() > 1 || (typeCheckMap.keySet().size() == 1 && !defaultCaseStatements.isEmpty())) && 
			(typeCheckMap.keySet().size() == (staticFieldMap.keySet().size() + subclassTypeMap.keySet().size()));
	}
	
	public boolean isApplicable() {
		if(!containsLocalVariableAssignment() && !containsBranchingStatement() && !containsSuperMethodInvocation() && !containsSuperFieldAccess() &&
				!isSubclassTypeAnInterface() && !returnStatementAfterTypeCheckCodeFragment())
			return true;
		else
			return false;
	}
	
	private boolean isSubclassTypeAnInterface() {
		for(List<PsiType> subTypes : subclassTypeMap.values()) {
			for(PsiType subType : subTypes) {
				// TODO: ad me
//				if(subType.resolveBinding().isInterface())
//					return true;
			}
		}
		return false;
	}
	
	private boolean returnStatementAfterTypeCheckCodeFragment() {
		//check if the type-check code fragment contains return statements having an expression
		StatementExtractor statementExtractor = new StatementExtractor();
		List<PsiStatement> allReturnStatementsWithinTypeCheckCodeFragment = statementExtractor.getReturnStatements(typeCheckCodeFragment);
		List<PsiReturnStatement> returnStatementsHavingExpressionWithinTypeCheckCodeFragment = new ArrayList<>();
		for(PsiStatement statement : allReturnStatementsWithinTypeCheckCodeFragment) {
			PsiReturnStatement returnStatement = (PsiReturnStatement)statement;
			if(returnStatement.getReturnValue() != null)
				returnStatementsHavingExpressionWithinTypeCheckCodeFragment.add(returnStatement);
		}
		if(returnStatementsHavingExpressionWithinTypeCheckCodeFragment.isEmpty())
			return false;
		//get all return statements having an expression within method body
		List<PsiStatement> allReturnStatementsWithinTypeCheckMethod = new ArrayList<>();
		for (PsiStatement statementInBlock : typeCheckMethod.getBody().getStatements()) {
			allReturnStatementsWithinTypeCheckMethod.addAll(statementExtractor.getReturnStatements(statementInBlock));
		}
		List<PsiReturnStatement> returnStatementsHavingExpressionWithinTypeCheckMethod = new ArrayList<PsiReturnStatement>();
		for(PsiStatement statement : allReturnStatementsWithinTypeCheckMethod) {
			PsiReturnStatement returnStatement = (PsiReturnStatement)statement;
			if(returnStatement.getReturnValue() != null)
				returnStatementsHavingExpressionWithinTypeCheckMethod.add(returnStatement);
		}
		List<PsiReturnStatement> returnStatementsHavingExpressionOutsideTypeCheckMethod = new ArrayList<>();
		returnStatementsHavingExpressionOutsideTypeCheckMethod.addAll(returnStatementsHavingExpressionWithinTypeCheckMethod);
		returnStatementsHavingExpressionOutsideTypeCheckMethod.removeAll(returnStatementsHavingExpressionWithinTypeCheckCodeFragment);
		for(PsiReturnStatement returnStatement : returnStatementsHavingExpressionOutsideTypeCheckMethod) {
			if(returnStatement.getTextOffset() > typeCheckCodeFragment.getTextOffset()+typeCheckCodeFragment.getTextLength())
				return true;
		}
		return false;
	}

	private boolean containsLocalVariableAssignment() {
		PsiVariable returnedVariableDeclaration = getTypeCheckMethodReturnedVariable();
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<ArrayList<PsiStatement>> allTypeCheckStatements = getTypeCheckStatements();
		if(!getDefaultCaseStatements().isEmpty()) {
			allTypeCheckStatements.add(getDefaultCaseStatements());
		}
		for(ArrayList<PsiStatement> typeCheckStatementList : allTypeCheckStatements) {
			List<PsiVariable> variableDeclarationFragmentsInsideBranch = new ArrayList<>();
			for(PsiStatement statement : typeCheckStatementList) {
				List<PsiStatement> variableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(statement);
				for(PsiStatement statement2 : variableDeclarationStatements) {
					PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement)statement2;
					PsiVariable[] fragments = (PsiVariable[]) variableDeclarationStatement.getDeclaredElements();
					variableDeclarationFragmentsInsideBranch.addAll(Arrays.asList(fragments));
				}
			}
			for(PsiStatement statement : typeCheckStatementList) {
				List<PsiExpression> assignments = expressionExtractor.getAssignments(statement);
				for(PsiExpression expression : assignments) {
					PsiAssignmentExpression assignment = (PsiAssignmentExpression)expression;
					PsiExpression leftHandSide = assignment.getLExpression();
					PsiExpression leftHandSideName = null;
					if (leftHandSide instanceof PsiReferenceExpression) {
						leftHandSideName = leftHandSide;
					}
					if(leftHandSideName != null) {
						PsiElement resolvedElement = ((PsiReferenceExpression) leftHandSideName).resolve();
						if(resolvedElement instanceof PsiVariable) {
							PsiVariable leftHandSideVariableBinding = (PsiVariable) resolvedElement;
							if(!(leftHandSideVariableBinding instanceof PsiField)) {
								boolean variableIsDeclaredInsideBranch = false;
								for(PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
									if(fragment.equals(leftHandSideVariableBinding)) {
										variableIsDeclaredInsideBranch = true;
										break;
									}
								}
								if(!variableIsDeclaredInsideBranch) {
									if(returnedVariableDeclaration == null) {
										return true;
									}
									else if(!returnedVariableDeclaration.equals(leftHandSideVariableBinding)) {
										return true;
									}
								}
							}
						}
					}
				}
				List<PsiExpression> postfixExpressions = expressionExtractor.getPostfixExpressions(statement);
				for(PsiExpression expression : postfixExpressions) {
					PsiPostfixExpression postfix = (PsiPostfixExpression)expression;
					PsiExpression operand = postfix.getOperand();
					PsiReferenceExpression operandName = null;
					if (operand instanceof PsiReferenceExpression) {
						operandName = (PsiReferenceExpression) operand;
					}
					if(operandName != null) {
						PsiElement operandBinding = operandName.resolve();
						if(operandBinding instanceof PsiVariable) {
							PsiVariable operandVariableBinding = (PsiVariable) operandBinding;
							if(!(operandVariableBinding instanceof PsiField)) {
								boolean variableIsDeclaredInsideBranch = false;
								for(PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
									if(fragment.equals(operandVariableBinding)) {
										variableIsDeclaredInsideBranch = true;
										break;
									}
								}
								if(!variableIsDeclaredInsideBranch) {
									if(returnedVariableDeclaration == null) {
										return true;
									}
									else if(!returnedVariableDeclaration.equals(operandVariableBinding)) {
										return true;
									}
								}
							}
						}
					}
				}
				List<PsiExpression> prefixExpressions = expressionExtractor.getPrefixExpressions(statement);
				for(PsiExpression expression : prefixExpressions) {
					PsiPrefixExpression prefix = (PsiPrefixExpression)expression;
					IElementType operator = prefix.getOperationTokenType();
					if(operator.equals(JavaTokenType.PLUSPLUS) || operator.equals(JavaTokenType.MINUSMINUS)) {
						PsiExpression operand = prefix.getOperand();
						PsiReferenceExpression operandName = null;
						if (operand instanceof PsiReferenceExpression) {
							operandName = (PsiReferenceExpression) operand;
						}

						if(operandName != null) {
							PsiElement operandBinding = operandName.resolve();
							if(operandBinding instanceof PsiVariable) {
								PsiVariable operandVariableBinding = (PsiVariable) operandBinding;
								if(!(operandVariableBinding instanceof PsiField)) {
									boolean variableIsDeclaredInsideBranch = false;
									for(PsiVariable fragment : variableDeclarationFragmentsInsideBranch) {
										if(fragment.equals(operandVariableBinding)) {
											variableIsDeclaredInsideBranch = true;
											break;
										}
									}
									if(!variableIsDeclaredInsideBranch) {
										if(returnedVariableDeclaration == null) {
											return true;
										}
										else if(!returnedVariableDeclaration.equals(operandVariableBinding)) {
											return true;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean containsBranchingStatement() {
		List<PsiStatement> statementList = new ArrayList<>();
		StatementExtractor statementExtractor = new StatementExtractor();
		List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		for(ArrayList<PsiStatement> statements : typeCheckStatements) {
			for(PsiStatement statement : statements) {
				statementList.addAll(statementExtractor.getBreakStatements(statement));
				statementList.addAll(statementExtractor.getContinueStatements(statement));
				List<PsiStatement> returnStatements = statementExtractor.getReturnStatements(statement);
				for(PsiStatement statement2 : returnStatements) {
					PsiReturnStatement returnStatement = (PsiReturnStatement)statement2;
					if(returnStatement.getReturnValue() == null)
						statementList.add(returnStatement);
				}
				
				List<PsiStatement> forStatements = statementExtractor.getForStatements(statement);
				for(PsiStatement forStatement : forStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(forStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(forStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(forStatement));
				}
				List<PsiStatement> whileStatements = statementExtractor.getWhileStatements(statement);
				for(PsiStatement whileStatement : whileStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(whileStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(whileStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(whileStatement));
				}
				List<PsiStatement> doStatements = statementExtractor.getDoStatements(statement);
				for(PsiStatement doStatement : doStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(doStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(doStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(doStatement));
				}
				List<PsiStatement> enchancedForStatements = statementExtractor.getEnhancedForStatements(statement);
				for(PsiStatement enchancedForStatement : enchancedForStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(enchancedForStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(enchancedForStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(enchancedForStatement));
				}
				List<PsiStatement> switchStatements = statementExtractor.getSwitchStatements(statement);
				for(PsiStatement switchStatement : switchStatements) {
					statementList.removeAll(statementExtractor.getBreakStatements(switchStatement));
		    		statementList.removeAll(statementExtractor.getContinueStatements(switchStatement));
		    		statementList.removeAll(statementExtractor.getReturnStatements(switchStatement));
				}
			}
		}
		if(!statementList.isEmpty())
			return true;
		else
			return false;
	}
	
	private boolean containsSuperMethodInvocation() {
		List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(ArrayList<PsiStatement> statements : typeCheckStatements) {
			for(PsiStatement statement : statements) {
				List<PsiExpression> superMethodInvocations = expressionExtractor.getSuperMethodInvocations(statement);
				if(!superMethodInvocations.isEmpty())
					return true;
			}
		}
		return false;
	}

	private boolean containsSuperFieldAccess() {
		List<ArrayList<PsiStatement>> typeCheckStatements = getTypeCheckStatements();
		if(!defaultCaseStatements.isEmpty())
			typeCheckStatements.add(defaultCaseStatements);
		
		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
		for(ArrayList<PsiStatement> statements : typeCheckStatements) {
			for(PsiStatement statement : statements) {
				List<PsiExpression> superFieldAccesses = expressionExtractor.getSuperFieldAccesses(statement);
				if(!superFieldAccesses.isEmpty())
					return true;
			}
		}
		return false;
	}

	// TODO: add me
//	public Type getTypeCheckMethodReturnType() {
//		return typeCheckMethod.getReturnType2();
//	}
//
	public PsiParameter[] getTypeCheckMethodParameters() {
		return typeCheckMethod.getParameterList().getParameters();
	}
	
	public PsiVariable getTypeCheckMethodReturnedVariable() {
		StatementExtractor statementExtractor = new StatementExtractor();
		List<PsiStatement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(typeCheckCodeFragment);
		if(!typeCheckCodeFragmentReturnStatements.isEmpty()) {
			PsiReturnStatement firstReturnStatement = (PsiReturnStatement)typeCheckCodeFragmentReturnStatements.get(0);
			if(firstReturnStatement.getReturnValue() instanceof PsiReferenceExpression) {
				PsiElement resolvedReference = ((PsiReferenceExpression) firstReturnStatement.getReturnValue()).resolve();
				PsiParameter[] parameters = typeCheckMethod.getParameterList().getParameters();
				for(PsiParameter parameter : parameters) {
					if(parameter.equals(resolvedReference))
						return parameter;
				}
				List<PsiStatement> variableDeclarationStatements = new ArrayList<>();
				for (PsiStatement statementInBlock : typeCheckMethod.getBody().getStatements()) {
					variableDeclarationStatements.addAll(statementExtractor.getVariableDeclarationStatements(statementInBlock));
				}
				for(PsiStatement statement : variableDeclarationStatements) {
					PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement) statement;
					PsiElement[] fragments = variableDeclarationStatement.getDeclaredElements();
					for(PsiElement fragment : fragments) {
						if(fragment.equals(resolvedReference))
							return (PsiVariable) fragment;
					}
				}
			}
		}
		else {
			List<PsiStatement> allReturnStatements = new ArrayList<>();
			for (PsiStatement statementInBlock : typeCheckMethod.getBody().getStatements()) {
				allReturnStatements.addAll(statementExtractor.getReturnStatements(statementInBlock));
			}
			if(!allReturnStatements.isEmpty()) {
				PsiReturnStatement lastReturnStatement = (PsiReturnStatement) allReturnStatements.get(allReturnStatements.size()-1);
				if(lastReturnStatement.getReturnValue() instanceof PsiReferenceExpression) {
					PsiReferenceExpression referenceExpression = (PsiReferenceExpression) lastReturnStatement.getReturnValue();
					for(PsiParameter assignedParameter : assignedParameters) {
						if(assignedParameter.equals(referenceExpression.resolve()))
							return assignedParameter;
					}
					for(PsiVariable assignedLocalVariable : assignedLocalVariables) {
						if(assignedLocalVariable.equals(referenceExpression.resolve()))
							return assignedLocalVariable;
					}
				}
			}
		}
		return null;
	}

	//TODO: add me
//	public SimpleName getTypeVariableSimpleName() {
//		if(typeField != null) {
//			return typeField.getName();
//		}
//		else if(typeLocalVariable != null) {
//			return typeLocalVariable.getName();
//		}
//		else if(foreignTypeField != null) {
//			return foreignTypeField.getName();
//		}
//		else if(typeMethodInvocation != null) {
//			Expression typeMethodInvocationExpression = typeMethodInvocation.getExpression();
//			SimpleName invoker = null;
//			if(typeMethodInvocationExpression instanceof SimpleName) {
//				invoker = (SimpleName)typeMethodInvocationExpression;
//			}
//			else if(typeMethodInvocationExpression instanceof FieldAccess) {
//				FieldAccess fieldAccess = (FieldAccess)typeMethodInvocationExpression;
//				invoker = fieldAccess.getName();
//			}
//			if(invoker != null)
//				return invoker;
//			else
//				return typeMethodInvocation.getName();
//		}
//		return null;
//	}

	// TODO: add me
//	public String getAbstractClassName() {
//		if(typeField != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
//			String typeFieldName = typeField.getName().getIdentifier().replaceAll("_", "");
//			return typeFieldName.substring(0, 1).toUpperCase() + typeFieldName.substring(1, typeFieldName.length());
//		}
//		else if(typeLocalVariable != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
//			String typeLocalVariableName = typeLocalVariable.getName().getIdentifier().replaceAll("_", "");
//			return typeLocalVariableName.substring(0, 1).toUpperCase() + typeLocalVariableName.substring(1, typeLocalVariableName.length());
//		}
//		else if(foreignTypeField != null && existingInheritanceTree == null && inheritanceTreeMatchingWithStaticTypes == null) {
//			String foreignTypeFieldName = foreignTypeField.getName().getIdentifier().replaceAll("_", "");
//			return foreignTypeFieldName.substring(0, 1).toUpperCase() + foreignTypeFieldName.substring(1, foreignTypeFieldName.length());
//		}
//		else if(existingInheritanceTree != null) {
//			DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
//			return (String)root.getUserObject();
//		}
//		else if(inheritanceTreeMatchingWithStaticTypes != null) {
//			DefaultMutableTreeNode root = inheritanceTreeMatchingWithStaticTypes.getRootNode();
//			String rootClassName = (String)root.getUserObject();
//			if(rootClassName.contains("."))
//				return rootClassName.substring(rootClassName.lastIndexOf(".")+1,rootClassName.length());
//			else
//				return rootClassName;
//		}
//		return null;
//	}
	
	public String getAbstractClassType() {
		String abstractClassType = null;
		if(typeField != null) {
			PsiType fieldType = typeField.getType();
			if(fieldType instanceof PsiClassType) {
				abstractClassType = fieldType.getCanonicalText();
			}
		}
		else if(typeLocalVariable != null) {
			PsiType fieldType = typeLocalVariable.getType();
			if(fieldType instanceof PsiClassType) {
				abstractClassType = fieldType.getCanonicalText();
			}
		}
		else if(foreignTypeField != null) {
			PsiType fieldType = foreignTypeField.getType();
			if(fieldType instanceof PsiClassType) {
				abstractClassType = fieldType.getCanonicalText();
			}
		}
		else if(typeMethodInvocation != null) {
			PsiExpression typeMethodInvocationExpression = typeMethodInvocation.getMethodExpression().getQualifierExpression();
			PsiReferenceExpression invoker = null;
			if(typeMethodInvocationExpression instanceof PsiReferenceExpression) {
				invoker = (PsiReferenceExpression) typeMethodInvocationExpression;
			}
			if(invoker != null) {
				abstractClassType = invoker.getType().getCanonicalText();
			}
		}

		if(abstractClassType == null) {
			PsiCodeBlock typeCheckMethodBody = typeCheckMethod.getBody();
			PsiStatement[] statements = typeCheckMethodBody.getStatements();
			if(statements.length > 0 && statements[0] instanceof PsiSwitchStatement) {
				PsiSwitchStatement switchStatement = (PsiSwitchStatement)statements[0];
				PsiStatement[] statements2 = switchStatement.getBody().getStatements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				List<PsiClass> superclassTypeBindings = new ArrayList<>();
				for(PsiStatement statement2 : statements2) {
					if(!(statement2 instanceof PsiSwitchLabelStatement) && !(statement2 instanceof PsiBreakStatement)) {
						List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement2);
						if(classInstanceCreations.size() == 1) {
							PsiNewExpression classInstanceCreation = (PsiNewExpression)classInstanceCreations.get(0);
							PsiJavaCodeReferenceElement classInstanceCreationType = classInstanceCreation.getClassReference();
							PsiElement resolvedReference = classInstanceCreationType.resolve();
							if (resolvedReference instanceof PsiClass) {
								superclassTypeBindings.add(((PsiClass) resolvedReference).getSuperClass());
							}
						}
					}
				}
				if(superclassTypeBindings.size() > 1) {
					for(PsiClass superclassTypeBinding : superclassTypeBindings) {
						if("java.lang.Object".equals(superclassTypeBinding.getQualifiedName()))
							return null;
					}
					if(equalTypeBindings(superclassTypeBindings)) {
						abstractClassType = superclassTypeBindings.get(0).getQualifiedName();
					}
					else {
						List<PsiClass> superclassTypeBindings2 = new ArrayList<>();
						for(PsiClass classTypeBinding : superclassTypeBindings) {
							PsiClass superclassTypeBinding = classTypeBinding.getSuperClass();
							if(superclassTypeBinding.getQualifiedName().equals("java.lang.Object"))
								superclassTypeBindings2.add(classTypeBinding);
							else
								superclassTypeBindings2.add(superclassTypeBinding);
						}
						if(equalTypeBindings(superclassTypeBindings2)) {
							abstractClassType = superclassTypeBindings.get(0).getQualifiedName();
						}
					}
				}
			}
		}
		return abstractClassType;
	}

	private boolean equalTypeBindings(List<PsiClass> typeBindings) {
		PsiClass firstTypeBinding = typeBindings.get(0);
		for(int i=1; i<typeBindings.size(); i++) {
			PsiClass currentTypeBinding = typeBindings.get(i);
			if(!firstTypeBinding.equals(currentTypeBinding)) {
				return false;
			}
		}
		return true;
	}
//
//	public List<String> getSubclassNames() {
//		List<String> subclassNames = new ArrayList<String>();
//		for(Expression expression : typeCheckMap.keySet()) {
//			List<SimpleName> simpleNameGroup = staticFieldMap.get(expression);
//			if(simpleNameGroup != null) {
//				for(SimpleName simpleName : simpleNameGroup) {
//					String staticFieldName = simpleName.getIdentifier();
//					Type castingType = getCastingType(typeCheckMap.get(expression));
//					String subclassName = null;
//					if(!staticFieldName.contains("_")) {
//						subclassName = staticFieldName.substring(0, 1).toUpperCase() +
//						staticFieldName.substring(1, staticFieldName.length()).toLowerCase();
//					}
//					else {
//						subclassName = "";
//						StringTokenizer tokenizer = new StringTokenizer(staticFieldName,"_");
//						while(tokenizer.hasMoreTokens()) {
//							String tempName = tokenizer.nextToken().toLowerCase().toString();
//							subclassName += tempName.subSequence(0, 1).toString().toUpperCase() +
//							tempName.subSequence(1, tempName.length()).toString();
//						}
//					}
//					if(inheritanceTreeMatchingWithStaticTypes != null) {
//						subclassNames.add(staticFieldSubclassTypeMap.get(simpleName));
//					}
//					else if(existingInheritanceTree != null) {
//						DefaultMutableTreeNode root = existingInheritanceTree.getRootNode();
//						DefaultMutableTreeNode leaf = root.getFirstLeaf();
//						while(leaf != null) {
//							String childClassName = (String)leaf.getUserObject();
//							if(childClassName.endsWith(subclassName)) {
//								subclassNames.add(childClassName);
//								break;
//							}
//							else if(castingType != null && castingType.resolveBinding().getQualifiedName().equals(childClassName)) {
//								subclassNames.add(childClassName);
//								break;
//							}
//							leaf = leaf.getNextLeaf();
//						}
//					}
//					else if(castingType != null) {
//						subclassNames.add(castingType.resolveBinding().getQualifiedName());
//					}
//					else {
//						subclassNames.add(subclassName);
//					}
//				}
//			}
//			List<Type> typeGroup = subclassTypeMap.get(expression);
//			if(typeGroup != null) {
//				for(Type type : typeGroup)
//					subclassNames.add(type.resolveBinding().getQualifiedName());
//			}
//		}
//		return subclassNames;
//	}

	// TODO: add me
//	private Type getCastingType(ArrayList<Statement> typeCheckCodeFragment) {
//		List<Expression> castExpressions = new ArrayList<Expression>();
//		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
//		for(Statement statement : typeCheckCodeFragment) {
//			castExpressions.addAll(expressionExtractor.getCastExpressions(statement));
//		}
//		for(Expression expression : castExpressions) {
//			CastExpression castExpression = (CastExpression)expression;
//			Expression expressionOfCastExpression = castExpression.getExpression();
//			SimpleName superTypeSimpleName = null;
//			if(expressionOfCastExpression instanceof SimpleName) {
//				superTypeSimpleName = (SimpleName)expressionOfCastExpression;
//			}
//			else if(expressionOfCastExpression instanceof FieldAccess) {
//				FieldAccess fieldAccess = (FieldAccess)expressionOfCastExpression;
//				superTypeSimpleName = fieldAccess.getName();
//			}
//			else if(expressionOfCastExpression instanceof MethodInvocation) {
//				MethodInvocation methodInvocation = (MethodInvocation)expressionOfCastExpression;
//				if(typeFieldGetterMethod != null && typeFieldGetterMethod.resolveBinding().isEqualTo(methodInvocation.resolveMethodBinding())) {
//					superTypeSimpleName = MethodDeclarationUtility.isGetter(typeFieldGetterMethod);
//				}
//			}
//			if(superTypeSimpleName != null) {
//				if(typeField != null) {
//					if(typeField.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
//						return castExpression.getType();
//				}
//				else if(typeLocalVariable != null) {
//					if(typeLocalVariable.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
//						return castExpression.getType();
//				}
//				else if(typeMethodInvocation != null) {
//					Expression typeMethodInvocationExpression = typeMethodInvocation.getExpression();
//					SimpleName invoker = null;
//					if(typeMethodInvocationExpression instanceof SimpleName) {
//						invoker = (SimpleName)typeMethodInvocationExpression;
//					}
//					else if(typeMethodInvocationExpression instanceof FieldAccess) {
//						FieldAccess fieldAccess = (FieldAccess)typeMethodInvocationExpression;
//						invoker = fieldAccess.getName();
//					}
//					if(invoker != null && invoker.resolveBinding().isEqualTo(superTypeSimpleName.resolveBinding()))
//						return castExpression.getType();
//				}
//			}
//		}
//		return null;
//	}
//
//	public Set<ITypeBinding> getThrownExceptions() {
//		ExpressionExtractor expressionExtractor = new ExpressionExtractor();
//		StatementExtractor statementExtractor = new StatementExtractor();
//		Set<ITypeBinding> thrownExceptions = new LinkedHashSet<ITypeBinding>();
//		for(Expression key : typeCheckMap.keySet()) {
//			ArrayList<Statement> statements = typeCheckMap.get(key);
//			for(Statement typeCheckStatement : statements) {
//				List<Expression> methodInvocations = expressionExtractor.getMethodInvocations(typeCheckStatement);
//				List<Expression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(typeCheckStatement);
//				List<Statement> tryStatements = statementExtractor.getTryStatements(typeCheckStatement);
//				Set<ITypeBinding> catchClauseExceptions = new LinkedHashSet<ITypeBinding>();
//				for(Statement statement : tryStatements) {
//					TryStatement tryStatement = (TryStatement)statement;
//					List<CatchClause> catchClauses = tryStatement.catchClauses();
//					for(CatchClause catchClause : catchClauses) {
//						SingleVariableDeclaration exception = catchClause.getException();
//						Type exceptionType = exception.getType();
//						if(exceptionType instanceof UnionType) {
//							UnionType unionType = (UnionType)exceptionType;
//							List<Type> types = unionType.types();
//							for(Type type : types) {
//								catchClauseExceptions.add(type.resolveBinding());
//							}
//						}
//						else {
//							catchClauseExceptions.add(exceptionType.resolveBinding());
//						}
//					}
//				}
//				for(Expression expression : methodInvocations) {
//					if(expression instanceof MethodInvocation) {
//						MethodInvocation methodInvocation = (MethodInvocation)expression;
//						IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
//						ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
//						for(ITypeBinding typeBinding : typeBindings) {
//							if(!catchClauseExceptions.contains(typeBinding))
//								thrownExceptions.add(typeBinding);
//						}
//					}
//				}
//				for(Expression expression : classInstanceCreations) {
//					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
//					IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
//					ITypeBinding[] typeBindings = methodBinding.getExceptionTypes();
//					for(ITypeBinding typeBinding : typeBindings) {
//						if(!catchClauseExceptions.contains(typeBinding))
//							thrownExceptions.add(typeBinding);
//					}
//				}
//			}
//		}
//		return thrownExceptions;
//	}
	
	public boolean allTypeCheckBranchesAreEmpty() {
		for(PsiExpression key : typeCheckMap.keySet()) {
			ArrayList<PsiStatement> statements = typeCheckMap.get(key);
			if(!statements.isEmpty())
				return false;
		}
		return true;
	}
	
	public boolean isTypeCheckMethodStateSetter() {
		InheritanceTree tree = null;
		if(existingInheritanceTree != null)
			tree = existingInheritanceTree;
		else if(inheritanceTreeMatchingWithStaticTypes != null)
			tree = inheritanceTreeMatchingWithStaticTypes;
		if(tree != null) {
			DefaultMutableTreeNode root = tree.getRootNode();
			DefaultMutableTreeNode leaf = root.getFirstLeaf();
			List<String> subclassNames = new ArrayList<>();
			while(leaf != null) {
				subclassNames.add((String)leaf.getUserObject());
				leaf = leaf.getNextLeaf();
			}
			PsiCodeBlock typeCheckMethodBody = typeCheckMethod.getBody();
			PsiStatement[] statements = typeCheckMethodBody.getStatements();
			if(statements.length > 0 && statements[0] instanceof PsiSwitchStatement) {
				PsiSwitchStatement switchStatement = (PsiSwitchStatement)statements[0];
				PsiStatement[] statements2 = switchStatement.getBody().getStatements();
				ExpressionExtractor expressionExtractor = new ExpressionExtractor();
				int matchCounter = 0;
				for(PsiStatement statement2 : statements2) {
					if(!(statement2 instanceof PsiSwitchLabelStatement) && !(statement2 instanceof PsiBreakStatement)) {
						List<PsiExpression> classInstanceCreations = expressionExtractor.getClassInstanceCreations(statement2);
						if(classInstanceCreations.size() == 1) {
							PsiNewExpression classInstanceCreation = (PsiNewExpression) classInstanceCreations.get(0);
							String classInstanceCreationType = classInstanceCreation.getClassReference().getQualifiedName();
							if(subclassNames.contains(classInstanceCreationType)) {
								matchCounter++;
							}
						}
					}
				}
				if(matchCounter == subclassNames.size())
					return true;
			}
		}
		return false;
	}

	// TODO: add me
//	public boolean typeCheckCodeFragmentContainsReturnStatement() {
//		StatementExtractor statementExtractor = new StatementExtractor();
//		List<Statement> typeCheckCodeFragmentReturnStatements = statementExtractor.getReturnStatements(typeCheckCodeFragment);
//		if(typeCheckCodeFragmentReturnStatements.isEmpty())
//			return false;
//		else
//			return true;
//	}
	
	public String getAbstractMethodName() {
		return abstractMethodName;
	}

	public void setAbstractMethodName(String methodName) {
		abstractMethodName = methodName;
	}
	
	public double getAverageNumberOfStatements() {
		if(averageNumberOfStatements == 0) {
			List<ArrayList<PsiStatement>> typeCheckStatements = new ArrayList<>(getTypeCheckStatements());
			ArrayList<PsiStatement> defaultCaseStatements = getDefaultCaseStatements();
			if(!defaultCaseStatements.isEmpty())
				typeCheckStatements.add(defaultCaseStatements);
			StatementExtractor statementExtractor = new StatementExtractor();
			int numberOfCases = typeCheckStatements.size();
			int totalNumberOfStatements = 0;
			for(ArrayList<PsiStatement> statements : typeCheckStatements) {
				for(PsiStatement statement : statements) {
					totalNumberOfStatements += statementExtractor.getTotalNumberOfStatements(statement);
				}
			}
			averageNumberOfStatements = (double)totalNumberOfStatements/(double)numberOfCases;
		}
		return averageNumberOfStatements;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof TypeCheckElimination) {
			TypeCheckElimination typeCheckElimination = (TypeCheckElimination)o;
			return this.typeCheckClass.equals(typeCheckElimination.typeCheckClass) &&
				this.typeCheckMethod.equals(typeCheckElimination.typeCheckMethod) &&
				this.typeCheckCodeFragment.equals(typeCheckElimination.typeCheckCodeFragment);
		}
		return false;
	}
	
	public int hashCode() {
		if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + typeCheckClass.hashCode();
    		result = 37*result + typeCheckMethod.hashCode();
    		result = 37*result + typeCheckCodeFragment.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
	}
	
	public String toString() {
		return typeCheckClass.getQualifiedName() + "::" +
			typeCheckMethod.toString();
	}

	public int getGroupSizeAtClassLevel() {
		return groupSizeAtClassLevel;
	}

	public void setGroupSizeAtClassLevel(int groupSizeAtClassLevel) {
		this.groupSizeAtClassLevel = groupSizeAtClassLevel;
	}

	// TODO: add me
//	public boolean matchingStatesOrSubTypes(TypeCheckElimination other) {
//		if(!this.staticFieldMap.isEmpty() && !other.staticFieldMap.isEmpty()) {
//			Set<String> originalStaticFields = new LinkedHashSet<String>();
//			for(List<SimpleName> staticFields : this.staticFieldMap.values()) {
//				for(SimpleName staticField : staticFields)
//					originalStaticFields.add(staticField.getIdentifier());
//			}
//			for(SimpleName staticField : this.additionalStaticFields) {
//				originalStaticFields.add(staticField.getIdentifier());
//			}
//			for(List<SimpleName> staticFields : other.staticFieldMap.values()) {
//				for(SimpleName staticField : staticFields) {
//					if(originalStaticFields.contains(staticField.getIdentifier()))
//						return true;
//				}
//			}
//		}
//		else if(!this.subclassTypeMap.isEmpty() && !other.subclassTypeMap.isEmpty()) {
//			InheritanceTree tree = null;
//			if(this.existingInheritanceTree != null)
//				tree = this.existingInheritanceTree;
//			if(this.inheritanceTreeMatchingWithStaticTypes != null && tree == null)
//				tree = this.inheritanceTreeMatchingWithStaticTypes;
//			for(List<Type> subTypes : other.subclassTypeMap.values()) {
//				for(Type subType : subTypes) {
//					if(tree.contains(subType.resolveBinding().getQualifiedName()))
//						return true;
//				}
//			}
//		}
//		return false;
//	}

	public Integer getUserRate() {
		return userRate;
	}

	public void setUserRate(Integer userRate) {
		this.userRate = userRate;
	}

	public int compareTo(TypeCheckElimination other) {
		int groupSizeAtClassLevel1 = this.getGroupSizeAtClassLevel();
		int groupSizeAtClassLevel2 = other.getGroupSizeAtClassLevel();
		double averageNumberOfStatements1 = this.getAverageNumberOfStatements();
		double averageNumberOfStatements2 = other.getAverageNumberOfStatements();
		String refactoringName1 = this.toString();
		String refactoringName2 = other.toString();
		
		if(groupSizeAtClassLevel1 > groupSizeAtClassLevel2)
			return -1;
		else if(groupSizeAtClassLevel1 < groupSizeAtClassLevel2)
			return 1;
		
		if(averageNumberOfStatements1 > averageNumberOfStatements2)
			return -1;
		else if(averageNumberOfStatements1 < averageNumberOfStatements2)
			return 1;
		
		return refactoringName1.compareTo(refactoringName2);
	}
}