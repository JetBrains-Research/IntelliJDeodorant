package core.ast;

import com.intellij.openapi.progress.ProgressIndicator;
import core.distance.DistanceMatrix;
import core.distance.MoveMethodCandidateRefactoring;
import core.distance.MySystem;
import core.distance.ProjectInfo;
import refactoring.TypeCheckEliminationGroup;

import java.util.*;

public class Standalone {

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);
        Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>(ASTReader.getSystemObject().getClassObjects());

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

    public static Set<TypeCheckEliminationGroup> getTypeCheckEliminationRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);
        SystemObject systemObject = ASTReader.getSystemObject();

        Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>();
        for (ClassObject classObject : systemObject.getClassObjects()) {
            if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenerator()) {
                classObjectsToBeExamined.add(classObject);
            }
        }
        return new TreeSet<>(systemObject.generateTypeCheckEliminations(classObjectsToBeExamined, indicator));
    }
}
