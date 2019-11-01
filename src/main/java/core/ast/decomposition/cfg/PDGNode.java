package core.ast.decomposition.cfg;

import com.intellij.psi.*;
import core.ast.*;
import core.ast.decomposition.AbstractStatement;
import core.ast.util.ExpressionExtractor;

import java.util.*;

import static utils.PsiUtils.isPrimitive;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
    private CFGNode cfgNode;
    Set<AbstractVariable> declaredVariables;
    protected Set<AbstractVariable> definedVariables;
    protected Set<AbstractVariable> usedVariables;
    Set<CreationObject> createdTypes;
    Set<String> thrownExceptionTypes;
    private Set<VariableDeclarationObject> variableDeclarationsInMethod;
    private Set<FieldObject> fieldsAccessedInMethod;
    private Set<AbstractVariable> originalDefinedVariables;
    private Set<AbstractVariable> originalUsedVariables;

    PDGNode() {
        super();
        this.declaredVariables = new LinkedHashSet<>();
        this.definedVariables = new LinkedHashSet<>();
        this.usedVariables = new LinkedHashSet<>();
        this.createdTypes = new LinkedHashSet<>();
        this.thrownExceptionTypes = new LinkedHashSet<>();
    }

    PDGNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
            Set<FieldObject> fieldsAccessedInMethod) {
        super();
        this.cfgNode = cfgNode;
        this.variableDeclarationsInMethod = variableDeclarationsInMethod;
        this.fieldsAccessedInMethod = fieldsAccessedInMethod;
        this.id = cfgNode.id;
        cfgNode.setPDGNode(this);
        this.declaredVariables = new LinkedHashSet<>();
        this.definedVariables = new LinkedHashSet<>();
        this.usedVariables = new LinkedHashSet<>();
        this.createdTypes = new LinkedHashSet<>();
        this.thrownExceptionTypes = new LinkedHashSet<>();
    }

    public Iterator<AbstractVariable> getDeclaredVariableIterator() {
        return declaredVariables.iterator();
    }

    public Iterator<AbstractVariable> getDefinedVariableIterator() {
        return definedVariables.iterator();
    }

    public Iterator<AbstractVariable> getUsedVariableIterator() {
        return usedVariables.iterator();
    }

    public CFGNode getCFGNode() {
        return cfgNode;
    }

    public Set<String> getThrownExceptionTypes() {
        return thrownExceptionTypes;
    }

    public Iterator<GraphEdge> getDependenceIterator() {
        Set<GraphEdge> allEdges = new LinkedHashSet<>();
        allEdges.addAll(incomingEdges);
        allEdges.addAll(outgoingEdges);
        return allEdges.iterator();
    }

    public Iterator<GraphEdge> getOutgoingDependenceIterator() {
        return outgoingEdges.iterator();
    }

    public Iterator<GraphEdge> getIncomingDependenceIterator() {
        return incomingEdges.iterator();
    }

    public Set<PDGNode> getControlDependentNodes() {
        Set<PDGNode> nodes = new LinkedHashSet<>();
        for (GraphEdge edge : outgoingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                PDGControlDependence controlDependence = (PDGControlDependence) dependence;
                PDGNode dstNode = (PDGNode) controlDependence.getDst();
                nodes.add(dstNode);
            }
        }
        return nodes;
    }

    public Set<PDGNode> getTrueControlDependentNodes() {
        Set<PDGNode> nodes = new LinkedHashSet<>();
        for (GraphEdge edge : outgoingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                PDGControlDependence controlDependence = (PDGControlDependence) dependence;
                if (controlDependence.isTrueControlDependence()) {
                    PDGNode dstNode = (PDGNode) controlDependence.getDst();
                    nodes.add(dstNode);
                }
            }
        }
        return nodes;
    }

    public PDGNode getControlDependenceParent() {
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                return (PDGNode) dependence.src;
            }
        }
        return null;
    }

    private boolean isControlDependentOnNode(PDGNode node) {
        PDGNode parent = this.getControlDependenceParent();
        while (parent != null) {
            if (parent.equals(node)) {
                return true;
            }
            parent = parent.getControlDependenceParent();
        }
        return false;
    }

    public boolean isControlDependentOnOneOfTheNodes(Set<PDGNode> nodes) {
        for (PDGNode node : nodes) {
            if (this.isControlDependentOnNode(node)) {
                return true;
            }
        }
        return false;
    }

    public PDGControlDependence getIncomingControlDependence() {
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                return (PDGControlDependence) dependence;
            }
        }
        return null;
    }

    boolean hasIncomingControlDependenceFromMethodEntryNode() {
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                PDGNode srcNode = (PDGNode) dependence.src;
                if (srcNode instanceof PDGMethodEntryNode)
                    return true;
            }
        }
        return false;
    }

    public Set<AbstractVariable> incomingDataDependencesFromNodesDeclaringOrDefiningVariables() {
        Set<AbstractVariable> dataDependences = new LinkedHashSet<>();
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGDataDependence) {
                PDGDataDependence dataDependence = (PDGDataDependence) dependence;
                PDGNode srcNode = (PDGNode) dependence.src;
                if (srcNode.declaresLocalVariable(dataDependence.getData()) || srcNode.definesLocalVariable(dataDependence.getData())) {
                    dataDependences.add(dataDependence.getData());
                }
            } else if (dependence instanceof PDGOutputDependence) {
                PDGOutputDependence outputDependence = (PDGOutputDependence) dependence;
                PDGNode srcNode = (PDGNode) dependence.src;
                if (srcNode.declaresLocalVariable(outputDependence.getData()) || srcNode.definesLocalVariable(outputDependence.getData())) {
                    dataDependences.add(outputDependence.getData());
                }
            }
        }
        return dataDependences;
    }

    boolean declaresLocalVariable(AbstractVariable variable) {
        return declaredVariables.contains(variable);
    }

    boolean definesLocalVariable(AbstractVariable variable) {
        return definedVariables.contains(variable);
    }

    boolean usesLocalVariable(AbstractVariable variable) {
        return usedVariables.contains(variable);
    }

    boolean instantiatesLocalVariable(AbstractVariable variable) {
        if (variable instanceof PlainVariable && this.definesLocalVariable(variable)) {
            PlainVariable plainVariable = (PlainVariable) variable;
            String variableType = plainVariable.getType();
            for (CreationObject creation : createdTypes) {
                if (creation instanceof ClassInstanceCreationObject) {
                    PsiNewExpression classInstanceCreationExpression =
                            ((ClassInstanceCreationObject) creation).getClassInstanceCreation();
                    PsiReference psiReference = classInstanceCreationExpression.getReference();
                    if (psiReference != null) {
                        PsiClass referencedClass = (PsiClass) psiReference.getElement();
                        PsiClass superClass = referencedClass.getSuperClass();
                        Set<String> implementedInterfaces = new LinkedHashSet<>();
                        if (superClass != null && superClass.getInterfaces().length > 0) {
                            for (PsiClass implementedInterface : superClass.getInterfaces()) {
                                implementedInterfaces.add(implementedInterface.getName());
                            }
                        }
                        if (variableType.equals(referencedClass.getQualifiedName())
                                || variableType.equals(Objects.requireNonNull(superClass).getQualifiedName())
                                || implementedInterfaces.contains(variableType))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    boolean containsClassInstanceCreation() {
        return !createdTypes.isEmpty();
    }

    boolean throwsException() {
        return !thrownExceptionTypes.isEmpty();
    }

    public BasicBlock getBasicBlock() {
        return cfgNode.getBasicBlock();
    }

    public AbstractStatement getStatement() {
        return cfgNode.getStatement();
    }

    public PsiStatement getASTStatement() {
        return cfgNode.getASTStatement();
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof PDGNode) {
            PDGNode pdgNode = (PDGNode) o;
            return this.cfgNode.equals(pdgNode.cfgNode);
        }
        return false;
    }

    public int hashCode() {
        return cfgNode.hashCode();
    }

    public String toString() {
        return cfgNode.toString();
    }

    public int compareTo(PDGNode node) {
        return Integer.compare(this.getId(), node.getId());
    }

    public String getAnnotation() {
        return "Def = " + definedVariables + " , Use = " + usedVariables;
    }

    void updateReachingAliasSet(ReachingAliasSet reachingAliasSet) {
        Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<>();
        variableDeclarations.addAll(variableDeclarationsInMethod);
        variableDeclarations.addAll(fieldsAccessedInMethod);
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiVariable vDStatement = (PsiVariable) ((PsiDeclarationStatement) statement).getDeclaredElements()[0];
            if (!isPrimitive(vDStatement.getType())) {
                PsiExpression initializer = vDStatement.getInitializer();
                PsiElement initializerSimpleName = null;
                if (initializer != null) {
                    if (initializer instanceof PsiVariable) {
                        initializerSimpleName = initializer;
                    }
                    if (initializerSimpleName != null) {
                        PsiVariable initializerVariableDeclaration = null;
                        for (VariableDeclarationObject declarationObject : variableDeclarations) {
                            PsiVariable declaration = declarationObject.getVariableDeclaration();
                            if (declaration.equals((initializerSimpleName))) {
                                initializerVariableDeclaration = declaration;
                                break;
                            }
                        }
                        if (initializerVariableDeclaration != null) {
                            reachingAliasSet.insertAlias(vDStatement, initializerVariableDeclaration);
                        }
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            if (expression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                processAssignment(reachingAliasSet, variableDeclarations, assignment);
            }
        }
    }

    private void processAssignment(ReachingAliasSet reachingAliasSet,
                                   Set<VariableDeclarationObject> variableDeclarations, PsiAssignmentExpression assignment) {
        PsiExpression leftHandSideExpression = assignment.getLExpression();
        PsiExpression rightHandSideExpression = assignment.getRExpression();
        PsiElement leftHandSideElement = leftHandSideExpression.getFirstChild();
        PsiVariable leftHandSideSimpleName = null;
        if (leftHandSideElement instanceof PsiVariable) {
            leftHandSideSimpleName = (PsiVariable) leftHandSideElement;
        }
        if (leftHandSideSimpleName != null && !isPrimitive(leftHandSideSimpleName.getType())) {
            PsiVariable leftHandSideVariableDeclaration = null;
            for (VariableDeclarationObject declarationObject : variableDeclarations) {
                PsiVariable declaration = declarationObject.getVariableDeclaration();
                if (declaration.equals(leftHandSideSimpleName)) {
                    leftHandSideVariableDeclaration = declaration;
                    break;
                }
            }
            PsiElement rightHandSideSimpleName = null;
            if (rightHandSideExpression != null) {
                rightHandSideSimpleName = rightHandSideExpression;
            }
            if (rightHandSideSimpleName != null) {
                PsiVariable rightHandSideVariableDeclaration = null;
                for (VariableDeclarationObject declarationObject : variableDeclarations) {
                    PsiVariable declaration = declarationObject.getVariableDeclaration();
                    if (declaration.equals(rightHandSideSimpleName)) {
                        rightHandSideVariableDeclaration = declaration;
                        break;
                    }
                }
                if (leftHandSideVariableDeclaration != null && rightHandSideVariableDeclaration != null) {
                    reachingAliasSet.insertAlias(leftHandSideVariableDeclaration, rightHandSideVariableDeclaration);
                }
            } else {
                if (leftHandSideVariableDeclaration != null) {
                    reachingAliasSet.removeAlias(leftHandSideVariableDeclaration);
                }
            }
        }
    }

    void applyReachingAliasSet(ReachingAliasSet reachingAliasSet) {
        if (originalDefinedVariables == null)
            originalDefinedVariables = new LinkedHashSet<>(definedVariables);
        Set<AbstractVariable> defVariablesToBeAdded = new LinkedHashSet<>();
        for (AbstractVariable abstractVariable : originalDefinedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (reachingAliasSet.containsAlias(compositeVariable)) {
                    Set<PsiVariable> aliases = reachingAliasSet.getAliases(compositeVariable);
                    for (PsiVariable alias : aliases) {
                        CompositeVariable aliasCompositeVariable =
                                new CompositeVariable(alias, compositeVariable.getRightPart());
                        defVariablesToBeAdded.add(aliasCompositeVariable);
                    }
                }
            }
        }
        definedVariables.addAll(defVariablesToBeAdded);
        if (originalUsedVariables == null)
            originalUsedVariables = new LinkedHashSet<>(usedVariables);
        Set<AbstractVariable> useVariablesToBeAdded = new LinkedHashSet<>();
        for (AbstractVariable abstractVariable : originalUsedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (reachingAliasSet.containsAlias(compositeVariable)) {
                    Set<PsiVariable> aliases = reachingAliasSet.getAliases(compositeVariable);
                    for (PsiVariable alias : aliases) {
                        CompositeVariable aliasCompositeVariable = new CompositeVariable(alias, compositeVariable.getRightPart());
                        useVariablesToBeAdded.add(aliasCompositeVariable);
                    }
                }
            }
        }
        usedVariables.addAll(useVariablesToBeAdded);
    }

    Map<PsiVariable, PsiNewExpression> getClassInstantiations() {
        Map<PsiVariable, PsiNewExpression> classInstantiationMap = new LinkedHashMap<>();
        Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<>();
        variableDeclarations.addAll(variableDeclarationsInMethod);
        variableDeclarations.addAll(fieldsAccessedInMethod);
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement vDStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = vDStatement.getDeclaredElements();
            for (PsiElement psiElement : declaredElements) {
                if (psiElement instanceof PsiVariable) {
                    PsiVariable psiVariable = (PsiVariable) psiElement;
                    PsiExpression psiExpression = psiVariable.getInitializer();
                    if (psiExpression instanceof PsiNewExpression) {
                        PsiNewExpression classInstanceCreation = (PsiNewExpression) psiExpression;
                        classInstantiationMap.put(psiVariable, classInstanceCreation);
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
            for (PsiExpression assignmentExpression : assignments) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) assignmentExpression;
                PsiExpression leftHandSideExpression = assignment.getLExpression();
                PsiExpression rightHandSideExpression = assignment.getRExpression();
                if (rightHandSideExpression instanceof PsiNewExpression) {
                    PsiNewExpression classInstanceCreation = (PsiNewExpression) rightHandSideExpression;
                    PsiElement leftHandSideSimpleName = null;
                    if (leftHandSideExpression instanceof PsiVariable) {
                        leftHandSideSimpleName = leftHandSideExpression;
                    }
                    if (leftHandSideSimpleName != null) {
                        PsiVariable leftHandSideVariableDeclaration = null;
                        for (VariableDeclarationObject declarationObject : variableDeclarations) {
                            PsiVariable declaration = declarationObject.getVariableDeclaration();
                            if (declaration.equals(leftHandSideSimpleName)) {
                                leftHandSideVariableDeclaration = declaration;
                                break;
                            }
                        }
                        if (leftHandSideVariableDeclaration != null) {
                            classInstantiationMap.put(leftHandSideVariableDeclaration, classInstanceCreation);
                        }
                    }
                }
            }
        }
        return classInstantiationMap;
    }

    public boolean changesStateOfVariable(PlainVariable plainVariable) {
        for (AbstractVariable abstractVariable : definedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (compositeVariable.getInitialVariable().equals(plainVariable)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean changesStateOfReference(PsiVariable variableDeclaration) {
        for (AbstractVariable abstractVariable : definedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (variableDeclaration.equals(compositeVariable.getOrigin()))
                    return true;
            }
        }
        return false;
    }

    boolean accessesReference(PsiVariable variableDeclaration) {
        for (AbstractVariable abstractVariable : usedVariables) {
            if (abstractVariable instanceof PlainVariable) {
                PlainVariable plainVariable = (PlainVariable) abstractVariable;
                if (variableDeclaration.equals(plainVariable.getOrigin()))
                    return true;
            }
        }
        return false;
    }

    boolean assignsReference(PsiVariable variableDeclaration) {
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement vDStatement = (PsiDeclarationStatement) statement;
            PsiElement[] psiElements = vDStatement.getDeclaredElements();
            for (PsiElement psiElement : psiElements) {
                if (psiElement instanceof PsiVariable) {
                    PsiVariable psiVariable = (PsiVariable) psiElement;
                    PsiExpression initializer = psiVariable.getInitializer();
                    PsiExpression initializerSimpleName = null;
                    if (initializer != null) {
                        initializerSimpleName = initializer;
                    }
                    if (initializerSimpleName != null) {
                        if (variableDeclaration.equals(initializerSimpleName.getLastChild())) {
                            return true;
                        }
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
            for (PsiExpression assignmentExpression : assignments) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) assignmentExpression;
                PsiExpression rightHandSideExpression = assignment.getRExpression();
                PsiElement rightHandSideSimpleName;
                if (rightHandSideExpression instanceof PsiField) {
                    rightHandSideSimpleName = rightHandSideExpression;
                } else {
                    rightHandSideSimpleName = rightHandSideExpression;
                }
                if (rightHandSideSimpleName != null) {
                    if (variableDeclaration.equals(rightHandSideSimpleName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
