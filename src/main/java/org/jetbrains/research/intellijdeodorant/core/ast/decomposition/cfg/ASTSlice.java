package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.*;
import com.sun.istack.NotNull;

import java.util.*;

public class ASTSlice {
    @NotNull
    private final PsiClass sourceTypeDeclaration;
    @NotNull
    private final PsiMethod sourceMethodDeclaration;
    @NotNull
    private final PsiFile psiFile;
    @NotNull
    private PsiStatement variableCriterionDeclarationStatement;
    @NotNull
    private PsiStatement extractedMethodInvocationInsertionStatement;
    private PsiVariable localVariableCriterion;
    private Set<PDGNode> sliceNodes;
    private Set<PsiStatement> sliceStatements;
    private Set<PsiStatement> removableStatements;
    private Set<PsiStatement> duplicatedStatements;
    private Set<PsiVariable> passedParameters;

    private String extractedMethodName;
    private boolean declarationOfVariableCriterionBelongsToSliceNodes;
    private boolean declarationOfVariableCriterionBelongsToRemovableNodes;
    private BasicBlock boundaryBlock;
    private boolean isObjectSlice;
    private int methodSize;

    public ASTSlice(PDGSlice pdgSlice) {
        this.sourceMethodDeclaration = pdgSlice.getMethod().getMethodDeclaration();
        this.sourceTypeDeclaration = (PsiClass) sourceMethodDeclaration.getParent();
        this.sliceNodes = pdgSlice.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(node.getASTStatement());
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgSlice.getRemovableNodes()) {
            removableStatements.add(node.getASTStatement());
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgSlice.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgSlice.getLocalVariableCriterion();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = variableDeclaration;
                this.extractedMethodName = Objects.requireNonNull(localVariableCriterion.getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgSlice.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(variableDeclaration);
                    break;
                }
            }
        }
        PDGNode declarationOfVariableCriterionNode = pdgSlice.getDeclarationOfVariableCriterion();
        if (declarationOfVariableCriterionNode != null)
            this.variableCriterionDeclarationStatement = declarationOfVariableCriterionNode.getASTStatement();
        this.extractedMethodInvocationInsertionStatement = pdgSlice.getExtractedMethodInvocationInsertionNode().getASTStatement();
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSlice.declarationOfVariableCriterionBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSlice.declarationOfVariableCriterionBelongsToRemovableNodes();
        this.psiFile = pdgSlice.getIFile();
        this.boundaryBlock = pdgSlice.getBoundaryBlock();
        this.isObjectSlice = false;
        this.methodSize = pdgSlice.getMethodSize();
    }

    public ASTSlice(PDGSliceUnion pdgSliceUnion) {
        this.sourceMethodDeclaration = pdgSliceUnion.getMethod().getMethodDeclaration();
        this.sourceTypeDeclaration = (PsiClass) sourceMethodDeclaration.getParent();
        this.sliceNodes = pdgSliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(node.getASTStatement());
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgSliceUnion.getRemovableNodes()) {
            removableStatements.add(node.getASTStatement());
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgSliceUnion.getLocalVariableCriterion();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = variableDeclaration;
                this.extractedMethodName = Objects.requireNonNull(localVariableCriterion.getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgSliceUnion.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(variableDeclaration);
                    break;
                }
            }
        }
        PDGNode declarationOfVariableCriterionNode = pdgSliceUnion.getDeclarationOfVariableCriterion();
        if (declarationOfVariableCriterionNode != null)
            this.variableCriterionDeclarationStatement = declarationOfVariableCriterionNode.getASTStatement();
        this.extractedMethodInvocationInsertionStatement = pdgSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement();
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToRemovableNodes();
        this.psiFile = pdgSliceUnion.getIFile();
        this.boundaryBlock = pdgSliceUnion.getBoundaryBlock();
        this.isObjectSlice = false;
        this.methodSize = pdgSliceUnion.getMethodSize();
    }

    public ASTSlice(PDGObjectSliceUnion pdgObjectSliceUnion) {
        this.sourceMethodDeclaration = pdgObjectSliceUnion.getMethod().getMethodDeclaration();
        this.sourceTypeDeclaration = (PsiClass) sourceMethodDeclaration.getParent();
        this.sliceNodes = pdgObjectSliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(node.getASTStatement());
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgObjectSliceUnion.getRemovableNodes()) {
            removableStatements.add(node.getASTStatement());
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgObjectSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgObjectSliceUnion.getObjectReference();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = variableDeclaration;
                this.extractedMethodName = Objects.requireNonNull(localVariableCriterion.getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgObjectSliceUnion.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(variableDeclaration);
                    break;
                }
            }
        }
        PDGNode declarationOfObjectReferenceNode = pdgObjectSliceUnion.getDeclarationOfObjectReference();
        if (declarationOfObjectReferenceNode != null)
            this.variableCriterionDeclarationStatement = declarationOfObjectReferenceNode.getASTStatement();
        this.extractedMethodInvocationInsertionStatement = pdgObjectSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement();
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToRemovableNodes();
        this.psiFile = pdgObjectSliceUnion.getIFile();
        this.boundaryBlock = pdgObjectSliceUnion.getBoundaryBlock();
        this.isObjectSlice = true;
        this.methodSize = pdgObjectSliceUnion.getMethodSize();
    }

    public boolean isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement() {
        PsiStatement variableCriterionDeclarationStatement = getVariableCriterionDeclarationStatement();
        if (variableCriterionDeclarationStatement != null) {
            int depthOfNestingForVariableCriterionDeclarationStatement = depthOfNesting(variableCriterionDeclarationStatement);
            PsiStatement extractedMethodInvocationInsertionStatement = getExtractedMethodInvocationInsertionStatement();
            int depthOfNestingForExtractedMethodInvocationInsertionStatement = depthOfNesting(extractedMethodInvocationInsertionStatement);
            if (depthOfNestingForVariableCriterionDeclarationStatement > depthOfNestingForExtractedMethodInvocationInsertionStatement)
                return true;
            return depthOfNestingForVariableCriterionDeclarationStatement == depthOfNestingForExtractedMethodInvocationInsertionStatement
                    && variableCriterionDeclarationStatement instanceof PsiTryStatement;
        }
        return false;
    }

    private int depthOfNesting(PsiStatement statement) {
        int depthOfNesting = 0;
        PsiElement parent = statement;
        while (!(parent instanceof PsiMethod)) {
            depthOfNesting++;
            parent = parent.getParent();
        }
        return depthOfNesting;
    }

    public PsiClass getSourceTypeDeclaration() {
        return sourceTypeDeclaration;
    }

    public PsiMethod getSourceMethodDeclaration() {
        return sourceMethodDeclaration;
    }

    public PsiVariable getLocalVariableCriterion() {
        return localVariableCriterion;
    }

    public Set<PsiVariable> getPassedParameters() {
        return passedParameters;
    }

    public Set<PDGNode> getSliceNodes() {
        return sliceNodes;
    }

    public Set<PsiStatement> getSliceStatements() {
        return sliceStatements;
    }

    private Set<PsiStatement> getRemovableStatements() {
        return removableStatements;
    }

    private PsiStatement getVariableCriterionDeclarationStatement() {
        return variableCriterionDeclarationStatement;
    }

    private PsiStatement getExtractedMethodInvocationInsertionStatement() {
        return extractedMethodInvocationInsertionStatement;
    }

    public String getExtractedMethodName() {
        return extractedMethodName;
    }

    public void setExtractedMethodName(String extractedMethodName) {
        this.extractedMethodName = extractedMethodName;
    }

    public boolean declarationOfVariableCriterionBelongsToSliceNodes() {
        return declarationOfVariableCriterionBelongsToSliceNodes;
    }

    public boolean declarationOfVariableCriterionBelongsToRemovableNodes() {
        return declarationOfVariableCriterionBelongsToRemovableNodes;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public BasicBlock getBoundaryBlock() {
        return boundaryBlock;
    }

    public boolean isObjectSlice() {
        return isObjectSlice;
    }

    public int getMethodSize() {
        return methodSize;
    }

    public String sliceToString() {
        StringBuilder sb = new StringBuilder();
        for (PDGNode sliceNode : sliceNodes) {
            sb.append(sliceNode.getStatement().toString());
        }
        return sb.toString();
    }

    public String toString() {
        return getSourceTypeDeclaration().getQualifiedName() + "::" +
                getSourceMethodDeclaration().getName() + "." +
                getLocalVariableCriterion().getName();
    }

    public int getNumberOfSliceStatements() {
        return getSliceStatements().size();
    }

    public int getNumberOfDuplicatedStatements() {
        int numberOfSliceStatements = getNumberOfSliceStatements();
        int numberOfRemovableStatements = getRemovableStatements().size();
        return numberOfSliceStatements - numberOfRemovableStatements;
    }

    /**
     * Checks all {@link PsiStatement} from slice for availability.
     *
     * @return true if all {@link PsiStatement} are valid, false otherwise.
     */
    public boolean areSliceStatementsValid() {
        Set<PsiStatement> setOfSliceStatements = this.getSliceStatements();
        if (setOfSliceStatements.isEmpty()) {
            return false;
        }
        Iterator<PsiStatement> iterator = setOfSliceStatements.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().isValid()) {
                return false;
            }
        }
        return true;
    }

}
