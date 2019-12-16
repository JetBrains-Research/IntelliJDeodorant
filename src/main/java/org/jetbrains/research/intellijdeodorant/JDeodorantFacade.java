package org.jetbrains.research.intellijdeodorant;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiVariable;
<<<<<<< HEAD:src/main/java/core/ast/Standalone.java
import core.ast.decomposition.cfg.*;
import core.distance.DistanceMatrix;
import core.distance.MoveMethodCandidateRefactoring;
import core.distance.MySystem;
import core.distance.ProjectInfo;
import ui.SettingsPanel;
=======
import org.jetbrains.research.intellijdeodorant.core.ast.ASTReader;
import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.*;
import org.jetbrains.research.intellijdeodorant.core.distance.DistanceMatrix;
import org.jetbrains.research.intellijdeodorant.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.MySystem;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
>>>>>>> master:src/main/java/org/jetbrains/research/intellijdeodorant/JDeodorantFacade.java

import java.util.*;

public class JDeodorantFacade {

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);
        Set<ClassObject> classObjectsToBeExamined = SettingsPanel.getClassesToFindRefactorings(ASTReader.getSystemObject());

        Set<String> classNamesToBeExamined = new LinkedHashSet<>();
        for (ClassObject classObject : classObjectsToBeExamined) {
            if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenerator())
                classNamesToBeExamined.add(classObject.getName());
        }
        MySystem system = new MySystem(ASTReader.getSystemObject(), false);
        DistanceMatrix distanceMatrix = new DistanceMatrix(system);

        List<MoveMethodCandidateRefactoring> candidateRefactoring =
                distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined, indicator);
        List<MoveMethodCandidateRefactoring> moveMethodCandidateList = new ArrayList<>(candidateRefactoring);
        Collections.sort(moveMethodCandidateList);
        return moveMethodCandidateList;
    }

    public static Set<ASTSliceGroup> getExtractMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);

        SystemObject systemObject = ASTReader.getSystemObject();
        Set<ASTSliceGroup> extractedSliceGroups = new TreeSet<>();
        if (systemObject != null) {
            Set<ClassObject> classObjectsToBeExamined = SettingsPanel.getClassesToFindRefactorings(systemObject);
            for (ClassObject classObject : classObjectsToBeExamined) {
                if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenenator()) {
                    ListIterator<MethodObject> methodIterator = classObject.getMethodIterator();
                    while (methodIterator.hasNext()) {
                        MethodObject methodObject = methodIterator.next();
                        processMethod(extractedSliceGroups, classObject, methodObject);
                    }
                }
            }
        }
        return extractedSliceGroups;
    }

    private static void processMethod(final Set<ASTSliceGroup> extractedSliceGroups, ClassObject classObject, MethodObject methodObject) {
        if (methodObject.getMethodBody() != null) {
            CFG cfg = new CFG(methodObject);
            PDG pdg = new PDG(cfg, classObject.getPsiFile(), classObject.getFieldsAccessedInsideMethod(methodObject));
            for (PsiVariable declaration : pdg.getVariableDeclarationsInMethod()) {
                PlainVariable variable = new PlainVariable(declaration);
                PDGSliceUnionCollection sliceUnionCollection = new PDGSliceUnionCollection(pdg, variable);
                double sumOfExtractedStatementsInGroup = 0.0;
                double sumOfDuplicatedStatementsInGroup = 0.0;
                double sumOfDuplicationRatioInGroup = 0.0;
                int maximumNumberOfExtractedStatementsInGroup = 0;
                int groupSize = sliceUnionCollection.getSliceUnions().size();
                ASTSliceGroup sliceGroup = new ASTSliceGroup();
                for (PDGSliceUnion sliceUnion : sliceUnionCollection.getSliceUnions()) {
                    ASTSlice slice = new ASTSlice(sliceUnion);
                    if (!slice.isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement()) {
                        int numberOfExtractedStatements = slice.getNumberOfSliceStatements();
                        int numberOfDuplicatedStatements = slice.getNumberOfDuplicatedStatements();
                        double duplicationRatio = (double) numberOfDuplicatedStatements / (double) numberOfExtractedStatements;
                        sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
                        sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
                        sumOfDuplicationRatioInGroup += duplicationRatio;
                        if (numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
                            maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
                        sliceGroup.addCandidate(slice);
                    }
                }
                if (!sliceGroup.getCandidates().isEmpty()) {
                    sliceGroup.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup / (double) groupSize);
                    sliceGroup.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup / (double) groupSize);
                    sliceGroup.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup / (double) groupSize);
                    sliceGroup.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
                    extractedSliceGroups.add(sliceGroup);
                }
            }
            for (PsiVariable declaration : pdg.getVariableDeclarationsAndAccessedFieldsInMethod()) {
                PlainVariable variable = new PlainVariable(declaration);
                PDGObjectSliceUnionCollection objectSliceUnionCollection = new PDGObjectSliceUnionCollection(pdg, variable);
                double sumOfExtractedStatementsInGroup = 0.0;
                double sumOfDuplicatedStatementsInGroup = 0.0;
                double sumOfDuplicationRatioInGroup = 0.0;
                int maximumNumberOfExtractedStatementsInGroup = 0;
                int groupSize = objectSliceUnionCollection.getSliceUnions().size();
                ASTSliceGroup sliceGroup = new ASTSliceGroup();
                for (PDGObjectSliceUnion objectSliceUnion : objectSliceUnionCollection.getSliceUnions()) {
                    ASTSlice slice = new ASTSlice(objectSliceUnion);
                    if (!slice.isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement()) {
                        int numberOfExtractedStatements = slice.getNumberOfSliceStatements();
                        int numberOfDuplicatedStatements = slice.getNumberOfDuplicatedStatements();
                        double duplicationRatio = (double) numberOfDuplicatedStatements / (double) numberOfExtractedStatements;
                        sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
                        sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
                        sumOfDuplicationRatioInGroup += duplicationRatio;
                        if (numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
                            maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
                        sliceGroup.addCandidate(slice);
                    }
                }
                if (!sliceGroup.getCandidates().isEmpty()) {
                    sliceGroup.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup / (double) groupSize);
                    sliceGroup.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup / (double) groupSize);
                    sliceGroup.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup / (double) groupSize);
                    sliceGroup.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
                    extractedSliceGroups.add(sliceGroup);
                }
            }
        }
    }
}
