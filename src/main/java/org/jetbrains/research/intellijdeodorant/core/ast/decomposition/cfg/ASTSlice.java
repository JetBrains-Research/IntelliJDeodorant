package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.*;
import com.sun.istack.NotNull;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.*;

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
    private final SmartPsiElementPointer<PsiElement> extractedMethodInvocationInsertionStatement;
    private SmartPsiElementPointer<PsiElement> localVariableCriterion;
    private final Set<PDGNode> sliceNodes;
    private final Set<SmartPsiElementPointer<PsiElement>> sliceStatements;
    private final Set<SmartPsiElementPointer<PsiElement>> removableStatements;
    private final Set<SmartPsiElementPointer<PsiElement>> duplicatedStatements;
    private final BasicBlock boundaryBlock;
    private final int originalMethodLinesCount;
    private final int originalMethodStatementsCount;
    private final int originalMethodParametersCount;
    private final String qualifiedMethodName;

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
                break;
            }
        }
        PDGNode declarationOfVariableCriterionNode = pdgSliceUnion.getDeclarationOfVariableCriterion();
        if (declarationOfVariableCriterionNode != null)
            this.variableCriterionDeclarationStatement = toPointer(declarationOfVariableCriterionNode.getASTStatement());
        this.extractedMethodInvocationInsertionStatement = toPointer(pdgSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement());
        this.psiFile = toPointer(pdgSliceUnion.getIFile());
        this.boundaryBlock = pdgSliceUnion.getBoundaryBlock();
        this.originalMethodLinesCount = getNumberOfLinesInMethod(pdgSliceUnion.getMethod().getMethodDeclaration());
        this.originalMethodStatementsCount = getMethodStatementCount(pdgSliceUnion.getMethod().getMethodDeclaration());
        this.originalMethodParametersCount = pdgSliceUnion.getMethod().getMethodDeclaration().getParameterList().getParametersCount();
        this.qualifiedMethodName = getHumanReadableName(pdgSliceUnion.getMethod().getMethodDeclaration());
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
                break;
            }
        }
        PDGNode declarationOfObjectReferenceNode = pdgObjectSliceUnion.getDeclarationOfObjectReference();
        if (declarationOfObjectReferenceNode != null)
            this.variableCriterionDeclarationStatement = toPointer(declarationOfObjectReferenceNode.getASTStatement());
        this.extractedMethodInvocationInsertionStatement = toPointer(pdgObjectSliceUnion.getExtractedMethodInvocationInsertionNode().getASTStatement());
        this.psiFile = toPointer(pdgObjectSliceUnion.getIFile());
        this.boundaryBlock = pdgObjectSliceUnion.getBoundaryBlock();
        this.originalMethodLinesCount = getNumberOfLinesInMethod(pdgObjectSliceUnion.getMethod().getMethodDeclaration());
        this.originalMethodStatementsCount = getMethodStatementCount(pdgObjectSliceUnion.getMethod().getMethodDeclaration());
        this.originalMethodParametersCount = pdgObjectSliceUnion.getMethod().getMethodDeclaration().getParameterList().getParametersCount();
        this.qualifiedMethodName = getHumanReadableName(pdgObjectSliceUnion.getMethod().getMethodDeclaration());
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

    public BasicBlock getBoundaryBlock() {
        return boundaryBlock;
    }

    public int getOriginalMethodLinesCount() {
        return originalMethodLinesCount;
    }

    public int getOriginalMethodStatementsCount() {
        return originalMethodStatementsCount;
    }

    public int getOriginalMethodParametersCount() {
        return originalMethodParametersCount;
    }

    @Override
    public String toString() {
        return qualifiedMethodName;
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
            if (psiElementSmartPsiElementPointer.getElement() == null ||
                    psiElementSmartPsiElementPointer.getElement() instanceof PsiStatement
                            && !psiElementSmartPsiElementPointer.getElement().isValid()) {
                return false;
            }
        }
        return true;
    }

}
