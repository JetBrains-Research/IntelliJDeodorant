package core.ast;

import core.distance.DistanceMatrix;
import core.distance.MoveMethodCandidateRefactoring;
import core.distance.MySystem;
import core.distance.ProjectInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Standalone {

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo project) {
        SystemObject systemObject = new SystemObject();
        new ASTReader(project, systemObject);

        Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>(systemObject.getClassObjects());

        Set<String> classNamesToBeExamined = new LinkedHashSet<>();
        for (ClassObject classObject : classObjectsToBeExamined) {
            if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenerator())
                classNamesToBeExamined.add(classObject.getName());
        }
        MySystem system = new MySystem(systemObject, false);
        DistanceMatrix distanceMatrix = new DistanceMatrix(system);

        List<MoveMethodCandidateRefactoring> candidateRefactoring = distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined);
        List<MoveMethodCandidateRefactoring> moveMethodCandidateList = new ArrayList<>(candidateRefactoring);
        Collections.sort(moveMethodCandidateList);
        return moveMethodCandidateList;
    }
}
