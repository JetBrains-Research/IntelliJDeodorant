package gr.uom.java.ast;

import gr.uom.java.distance.ProjectInfo;
import gr.uom.java.distance.DistanceMatrix;
import gr.uom.java.distance.MoveMethodCandidateRefactoring;
import gr.uom.java.distance.MySystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Standalone {

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo project) {
        SystemObject systemObject = new SystemObject();
        new ASTReader(project, systemObject);

        List<MoveMethodCandidateRefactoring> moveMethodCandidateList = new ArrayList<MoveMethodCandidateRefactoring>();
        Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<ClassObject>(systemObject.getClassObjects());

        Set<String> classNamesToBeExamined = new LinkedHashSet<String>();
        for (ClassObject classObject : classObjectsToBeExamined) {
            if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenenator())
                classNamesToBeExamined.add(classObject.getName());
        }
        MySystem system = new MySystem(systemObject, false);
        DistanceMatrix distanceMatrix = new DistanceMatrix(system);

        moveMethodCandidateList.addAll(distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined));
        Collections.sort(moveMethodCandidateList);
        return moveMethodCandidateList;
    }
}
