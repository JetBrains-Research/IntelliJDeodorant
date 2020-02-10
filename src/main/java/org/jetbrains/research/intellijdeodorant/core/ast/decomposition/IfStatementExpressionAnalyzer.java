package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class IfStatementExpressionAnalyzer {
    //parent nodes are CONDITIONAL_AND (&&), CONDITIONAL_OR (||) infix operators, while leaf nodes are expressions
    private DefaultMutableTreeNode root;
    private PsiExpression completeExpression;
    //contains the expressions corresponding to each candidate type variable
    private Map<PsiReferenceExpression, PsiExpression> typeVariableExpressionMap;
    //contains the static fields corresponding to each candidate type variable
    private Map<PsiReferenceExpression, ArrayList<PsiReferenceExpression>> typeVariableStaticFieldMap;
    //contains the subclass types corresponding to each candidate type variable
    private Map<PsiReferenceExpression, ArrayList<PsiType>> typeVariableSubclassMap;

    //contains the expressions corresponding to each candidate type method invocation
    private Map<PsiMethodCallExpression, PsiExpression> typeMethodInvocationExpressionMap;
    //contains the static fields corresponding to each candidate type method invocation
    private Map<PsiMethodCallExpression, ArrayList<PsiReferenceExpression>> typeMethodInvocationStaticFieldMap;
    //contains the subclass types corresponding to each candidate type method invocation
    private Map<PsiMethodCallExpression, ArrayList<PsiType>> typeMethodInvocationSubclassMap;

    public IfStatementExpressionAnalyzer(PsiExpression completeExpression) {
        this.root = new DefaultMutableTreeNode();
        this.completeExpression = completeExpression;
        this.typeVariableExpressionMap = new LinkedHashMap<>();
        this.typeVariableStaticFieldMap = new LinkedHashMap<>();
        this.typeVariableSubclassMap = new LinkedHashMap<>();
        this.typeMethodInvocationExpressionMap = new LinkedHashMap<>();
        this.typeMethodInvocationStaticFieldMap = new LinkedHashMap<>();
        this.typeMethodInvocationSubclassMap = new LinkedHashMap<>();
        processExpression(root, completeExpression);
    }

    public void putTypeVariableExpression(PsiReferenceExpression typeVariable, PsiExpression expression) {
        typeVariableExpressionMap.put(typeVariable, expression);
    }

    public Set<PsiReferenceExpression> getTargetVariables() {
        Set<PsiReferenceExpression> targetVariables = new LinkedHashSet<>();
        for (PsiReferenceExpression targetVariable : typeVariableExpressionMap.keySet()) {
            if (typeVariableStaticFieldMap.containsKey(targetVariable) ||
                    typeVariableSubclassMap.containsKey(targetVariable))
                targetVariables.add(targetVariable);
        }
        return targetVariables;
    }

    public PsiExpression getTypeVariableExpression(PsiReferenceExpression typeVariable) {
        return typeVariableExpressionMap.get(typeVariable);
    }

    public void putTypeVariableStaticField(PsiReferenceExpression typeVariable, PsiReferenceExpression staticField) {
        for (PsiReferenceExpression keySimpleName : typeVariableStaticFieldMap.keySet()) {
            if (keySimpleName.resolve().equals(typeVariable.resolve())) {
                ArrayList<PsiReferenceExpression> staticFields = typeVariableStaticFieldMap.get(keySimpleName);
                staticFields.add(staticField);
                return;
            }
        }
        ArrayList<PsiReferenceExpression> staticFields = new ArrayList<>();
        staticFields.add(staticField);
        typeVariableStaticFieldMap.put(typeVariable, staticFields);
    }

    public List<PsiReferenceExpression> getTypeVariableStaticField(PsiReferenceExpression typeVariable) {
        return typeVariableStaticFieldMap.get(typeVariable);
    }

    public void putTypeVariableSubclass(PsiReferenceExpression typeVariable, PsiType subclass) {
        for (PsiReferenceExpression keySimpleName : typeVariableSubclassMap.keySet()) {
            if (keySimpleName.resolve().equals(typeVariable.resolve())) {
                ArrayList<PsiType> subclasses = typeVariableSubclassMap.get(keySimpleName);
                subclasses.add(subclass);
                return;
            }
        }
        ArrayList<PsiType> subclasses = new ArrayList<>();
        subclasses.add(subclass);
        typeVariableSubclassMap.put(typeVariable, subclasses);
    }

    public List<PsiType> getTypeVariableSubclass(PsiReferenceExpression typeVariable) {
        return typeVariableSubclassMap.get(typeVariable);
    }

    public void putTypeMethodInvocationExpression(PsiMethodCallExpression typeMethodInvocation, PsiExpression expression) {
        typeMethodInvocationExpressionMap.put(typeMethodInvocation, expression);
    }

    public Set<PsiMethodCallExpression> getTargetMethodInvocations() {
        Set<PsiMethodCallExpression> targetMethodInvocations = new LinkedHashSet<PsiMethodCallExpression>();
        for (PsiMethodCallExpression targetMethodInvocation : typeMethodInvocationExpressionMap.keySet()) {
            if (typeMethodInvocationStaticFieldMap.containsKey(targetMethodInvocation) ||
                    typeMethodInvocationSubclassMap.containsKey(targetMethodInvocation))
                targetMethodInvocations.add(targetMethodInvocation);
        }
        return targetMethodInvocations;
    }

    public PsiExpression getTypeMethodInvocationExpression(PsiMethodCallExpression typeMethodInvocation) {
        return typeMethodInvocationExpressionMap.get(typeMethodInvocation);
    }

    public void putTypeMethodInvocationStaticField(PsiMethodCallExpression typeMethodInvocation, PsiReferenceExpression staticField) {
        for (PsiMethodCallExpression keyMethodInvocation : typeMethodInvocationStaticFieldMap.keySet()) {
            if (keyMethodInvocation.resolveMethod().equals(typeMethodInvocation.resolveMethod())) {
                ArrayList<PsiReferenceExpression> staticFields = typeMethodInvocationStaticFieldMap.get(keyMethodInvocation);
                staticFields.add(staticField);
                return;
            }
        }
        ArrayList<PsiReferenceExpression> staticFields = new ArrayList<>();
        staticFields.add(staticField);
        typeMethodInvocationStaticFieldMap.put(typeMethodInvocation, staticFields);
    }

    public List<PsiReferenceExpression> getTypeMethodInvocationStaticField(PsiMethodCallExpression typeMethodInvocation) {
        return typeMethodInvocationStaticFieldMap.get(typeMethodInvocation);
    }

    public void putTypeMethodInvocationSubclass(PsiMethodCallExpression typeMethodInvocation, PsiType subclass) {
        for (PsiMethodCallExpression keyMethodInvocation : typeMethodInvocationSubclassMap.keySet()) {
            if (keyMethodInvocation.resolveMethod().equals(typeMethodInvocation.resolveMethod())) {
                ArrayList<PsiType> subclasses = typeMethodInvocationSubclassMap.get(keyMethodInvocation);
                subclasses.add(subclass);
                return;
            }
        }
        ArrayList<PsiType> subclasses = new ArrayList<>();
        subclasses.add(subclass);
        typeMethodInvocationSubclassMap.put(typeMethodInvocation, subclasses);
    }

    public List<PsiType> getTypeMethodInvocationSubclass(PsiMethodCallExpression typeMethodInvocation) {
        return typeMethodInvocationSubclassMap.get(typeMethodInvocation);
    }

    private void processExpression(DefaultMutableTreeNode parent, PsiExpression expression) { // TODO: not sure if works
        if (expression instanceof PsiBinaryExpression) {
            PsiBinaryExpression infixExpression = (PsiBinaryExpression) expression;
            IElementType operator = infixExpression.getOperationTokenType();
            if (operator.equals(JavaTokenType.ANDAND) || operator.equals(JavaTokenType.OROR)) {
                parent.setUserObject(operator);
                DefaultMutableTreeNode leftOperandNode = new DefaultMutableTreeNode();
                DefaultMutableTreeNode rightOperandNode = new DefaultMutableTreeNode();
                parent.add(leftOperandNode);
                parent.add(rightOperandNode);
                processExpression(leftOperandNode, infixExpression.getLOperand());
                processExpression(rightOperandNode, infixExpression.getROperand());
            } else {
                parent.setUserObject(infixExpression);
            }
        } else {
            parent.setUserObject(expression);
        }
    }

    public List<PsiInstanceOfExpression> getInstanceofExpressions() {
        List<PsiInstanceOfExpression> expressionList = new ArrayList<>();
        DefaultMutableTreeNode leaf = root.getFirstLeaf();
        while (leaf != null) {
            PsiExpression expression = (PsiExpression) leaf.getUserObject();
            if (expression instanceof PsiInstanceOfExpression) {
                PsiInstanceOfExpression instanceofExpression = (PsiInstanceOfExpression) expression;
                expressionList.add(instanceofExpression);
            }
            leaf = leaf.getNextLeaf();
        }
        return expressionList;
    }

    public List<PsiBinaryExpression> getInfixExpressionsWithEqualsOperator() {
        List<PsiBinaryExpression> expressionList = new ArrayList<>();
        DefaultMutableTreeNode leaf = root.getFirstLeaf();
        while (leaf != null) {
            PsiExpression expression = (PsiExpression) leaf.getUserObject();
            if (expression instanceof PsiBinaryExpression) {
                PsiBinaryExpression infixExpression = (PsiBinaryExpression) expression;
                IElementType operator = infixExpression.getOperationTokenType();
                if (operator.equals(JavaTokenType.EQEQ))
                    expressionList.add(infixExpression);
            }
            leaf = leaf.getNextLeaf();
        }
        return expressionList;
    }

    public DefaultMutableTreeNode getRemainingExpression(PsiExpression expressionToBeRemoved) {
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode();
        processExpression(newRoot, completeExpression);
        DefaultMutableTreeNode leaf = newRoot.getFirstLeaf();
        while (leaf != null) {
            PsiExpression expression = (PsiExpression) leaf.getUserObject();
            if (expression.equals(expressionToBeRemoved)) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) leaf.getParent();
                if (parent != null) {
                    DefaultMutableTreeNode grandParent = (DefaultMutableTreeNode) parent.getParent();
                    DefaultMutableTreeNode sibling = null;
                    if (leaf.getNextSibling() != null) {
                        sibling = leaf.getNextSibling();
                    } else if (leaf.getPreviousSibling() != null) {
                        sibling = leaf.getPreviousSibling();
                    }
                    if (grandParent != null) {
                        int parentIndex = grandParent.getIndex(parent);
                        grandParent.remove(parent);
                        grandParent.insert(sibling, parentIndex);
                    } else {
                        newRoot = sibling;
                    }
                    break;
                } else {
                    newRoot = null;
                    break;
                }
            }
            leaf = leaf.getNextLeaf();
        }
        return newRoot;
    }

    public boolean allParentNodesAreConditionalAndOperators() {
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (!node.isLeaf()) {
                IElementType operator = (IElementType) node.getUserObject();
                if (!operator.equals(JavaTokenType.ANDAND))
                    return false;
            }
        }
        return true;
    }

    public boolean allParentNodesAreConditionalOrOperators() {
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (!node.isLeaf()) {
                IElementType operator = (IElementType) node.getUserObject();
                if (!operator.equals(JavaTokenType.OROR))
                    return false;
            }
        }
        return true;
    }

    public int getNumberOfConditionalOperatorNodes() {
        int counter = 0;
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (!node.isLeaf()) {
                counter++;
            }
        }
        return counter;
    }

    public PsiExpression getCompleteExpression() {
        return completeExpression;
    }

    public String toString() {
        return completeExpression.toString();
    }
}
