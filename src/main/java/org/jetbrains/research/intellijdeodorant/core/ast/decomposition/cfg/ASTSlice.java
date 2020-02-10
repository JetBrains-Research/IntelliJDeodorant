package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.*;
import com.sun.istack.NotNull;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ASTSlice {
    @NotNull
    private final SmartPsiElementPointer<PsiElement> sourceTypeDeclaration;
    @NotNull
    private final SmartPsiElementPointer<PsiElement> sourceMethodDeclaration;
    @NotNull
    private final SmartPsiElementPointer<PsiElement> psiFile;
    @NotNull
    private SmartPsiElementPointer<PsiElement> variableCriterionDeclarationStatement;
    @NotNull
    private SmartPsiElementPointer<PsiElement> extractedMethodInvocationInsertionStatement;
    private SmartPsiElementPointer<PsiElement> localVariableCriterion;
    private Set<PDGNode> sliceNodes;
    private Set<SmartPsiElementPointer<PsiElement>> sliceStatements;
    private Set<SmartPsiElementPointer<PsiElement>> removableStatements;
    private Set<SmartPsiElementPointer<PsiElement>> duplicatedStatements;
    private Set<SmartPsiElementPointer<PsiElement>> passedParameters;

    private String extractedMethodName;
    private boolean declarationOfVariableCriterionBelongsToSliceNodes;
    private boolean declarationOfVariableCriterionBelongsToRemovableNodes;
    private BasicBlock boundaryBlock;
    private boolean isObjectSlice;
    private int methodSize;

    public ASTSlice(PDGSlice pdgSlice) {
        this.sourceMethodDeclaration = toPointer(pdgSlice.getMethod().getMethodDeclaration());
        this.sourceTypeDeclaration = toPointer(sourceMethodDeclaration.getElement().getParent());
        this.sliceNodes = pdgSlice.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(toPointer(node.getASTStatement()));
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgSlice.getRemovableNodes()) {
            removableStatements.add(toPointer(node.getASTStatement()));
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgSlice.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgSlice.getLocalVariableCriterion();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = toPointer(variableDeclaration);
                this.extractedMethodName = Objects.requireNonNull(((PsiVariable) localVariableCriterion.getElement()).getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgSlice.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(toPointer(variableDeclaration));
                    break;
                }
            }
        }
        PDGNode declarationOfVariableCriterionNode = pdgSlice.getDeclarationOfVariableCriterion();
        if (declarationOfVariableCriterionNode != null)
            this.variableCriterionDeclarationStatement = toPointer(declarationOfVariableCriterionNode.getASTStatement());
        this.extractedMethodInvocationInsertionStatement = toPointer(pdgSlice.getExtractedMethodInvocationInsertionNode().getASTStatement());
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSlice.declarationOfVariableCriterionBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSlice.declarationOfVariableCriterionBelongsToRemovableNodes();
        this.psiFile = toPointer(pdgSlice.getIFile());
        this.boundaryBlock = pdgSlice.getBoundaryBlock();
        this.isObjectSlice = false;
        this.methodSize = pdgSlice.getMethodSize();
    }

    public ASTSlice(PDGSliceUnion pdgSliceUnion) {
        this.sourceMethodDeclaration = toPointer(pdgSliceUnion.getMethod().getMethodDeclaration());
        this.sourceTypeDeclaration = toPointer(sourceMethodDeclaration.getElement().getParent());
        this.sliceNodes = pdgSliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(toPointer(node.getASTStatement()));
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgSliceUnion.getRemovableNodes()) {
            removableStatements.add(toPointer(node.getASTStatement()));
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgSliceUnion.getLocalVariableCriterion();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = toPointer(variableDeclaration);
                this.extractedMethodName = Objects.requireNonNull(((PsiVariable) localVariableCriterion.getElement()).getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgSliceUnion.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(toPointer(variableDeclaration));
                    break;
                }
            }
        }
        PDGNode declarationOfVariableCriterionNode = pdgSliceUnion.getDeclarationOfVariableCriterion();
        if (declarationOfVariableCriterionNode != null)
            this.variableCriterionDeclarationStatement = toPointer(declarationOfVariableCriterionNode.getASTStatement());
        this.extractedMethodInvocationInsertionStatement = toPointer(pdgSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement());
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgSliceUnion.declarationOfVariableCriterionBelongsToRemovableNodes();
        this.psiFile = toPointer(pdgSliceUnion.getIFile());
        this.boundaryBlock = pdgSliceUnion.getBoundaryBlock();
        this.isObjectSlice = false;
        this.methodSize = pdgSliceUnion.getMethodSize();
    }

    public ASTSlice(PDGObjectSliceUnion pdgObjectSliceUnion) {
        this.sourceMethodDeclaration = toPointer(pdgObjectSliceUnion.getMethod().getMethodDeclaration());
        this.sourceTypeDeclaration = toPointer(sourceMethodDeclaration.getElement().getParent());
        this.sliceNodes = pdgObjectSliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (PDGNode node : sliceNodes) {
            sliceStatements.add(toPointer(node.getASTStatement()));
        }
        this.removableStatements = new LinkedHashSet<>();
        for (PDGNode node : pdgObjectSliceUnion.getRemovableNodes()) {
            removableStatements.add(toPointer(node.getASTStatement()));
        }
        this.duplicatedStatements = new LinkedHashSet<>(sliceStatements);
        this.duplicatedStatements.removeAll(removableStatements);
        Set<PsiVariable> variableDeclarationsAndAccessedFields = pdgObjectSliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
        AbstractVariable criterion = pdgObjectSliceUnion.getObjectReference();
        for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
            if (variableDeclaration.equals(criterion.getOrigin())) {
                this.localVariableCriterion = toPointer(variableDeclaration);
                this.extractedMethodName = Objects.requireNonNull(((PsiVariable) localVariableCriterion.getElement()).getNameIdentifier()).getText();
                break;
            }
        }
        this.passedParameters = new LinkedHashSet<>();
        for (AbstractVariable variable : pdgObjectSliceUnion.getPassedParameters()) {
            for (PsiVariable variableDeclaration : variableDeclarationsAndAccessedFields) {
                if (variableDeclaration.equals(variable.getOrigin())) {
                    passedParameters.add(toPointer(variableDeclaration));
                    break;
                }
            }
        }
        PDGNode declarationOfObjectReferenceNode = pdgObjectSliceUnion.getDeclarationOfObjectReference();
        if (declarationOfObjectReferenceNode != null)
            this.variableCriterionDeclarationStatement = toPointer(declarationOfObjectReferenceNode.getASTStatement());
        this.extractedMethodInvocationInsertionStatement = toPointer(pdgObjectSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement());
        this.declarationOfVariableCriterionBelongsToSliceNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToSliceNodes();
        this.declarationOfVariableCriterionBelongsToRemovableNodes = pdgObjectSliceUnion.declarationOfObjectReferenceBelongsToRemovableNodes();
        this.psiFile = toPointer(pdgObjectSliceUnion.getIFile());
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
        return (PsiClass) sourceTypeDeclaration.getElement();
    }

    public PsiMethod getSourceMethodDeclaration() {
        return (PsiMethod) sourceMethodDeclaration.getElement();
    }

    public PsiVariable getLocalVariableCriterion() {
        return (PsiVariable) localVariableCriterion.getElement();
    }

    public Set<SmartPsiElementPointer<PsiElement>> getPassedParameters() {
        return passedParameters;
    }

    public Set<PDGNode> getSliceNodes() {
        return sliceNodes;
    }

    public Set<SmartPsiElementPointer<PsiElement>> getSliceStatements() {
        return sliceStatements;
    }

    private Set<SmartPsiElementPointer<PsiElement>> getRemovableStatements() {
        return removableStatements;
    }

    private PsiStatement getVariableCriterionDeclarationStatement() {
        return variableCriterionDeclarationStatement == null ? null : (PsiStatement) variableCriterionDeclarationStatement.getElement();
    }

    private PsiStatement getExtractedMethodInvocationInsertionStatement() {
        return (PsiStatement) extractedMethodInvocationInsertionStatement.getElement();
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
        return (PsiFile) psiFile.getElement();
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
        for (SmartPsiElementPointer<PsiElement> psiElementSmartPsiElementPointer : this.getSliceStatements()) {
            if (psiElementSmartPsiElementPointer.getElement() instanceof PsiStatement
                    && !psiElementSmartPsiElementPointer.getElement().isValid()) {
                return false;
            }
        }
        return true;
    }

}
