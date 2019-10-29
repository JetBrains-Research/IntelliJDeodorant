package core.ast;

import com.intellij.openapi.progress.ProgressIndicator;
import core.distance.*;

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

    public static Set<ExtractClassCandidateGroup> getExtractClassRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);
        SystemObject systemObject = ASTReader.getSystemObject();
        if (systemObject != null) {
            Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>(systemObject.getClassObjects());

            Set<String> classNamesToBeExamined = new LinkedHashSet<String>();
            for (ClassObject classObject : classObjectsToBeExamined) {
                if (!classObject.isEnum() && !classObject.isInterface() && !classObject.isGeneratedByParserGenerator())
                    classNamesToBeExamined.add(classObject.getName());
            }
            MySystem system = new MySystem(systemObject, true);
            DistanceMatrix distanceMatrix = new DistanceMatrix(system);

            List<ExtractClassCandidateRefactoring> extractClassCandidateList = new ArrayList<>(distanceMatrix.getExtractClassCandidateRefactorings(classNamesToBeExamined, indicator));

            HashMap<String, ExtractClassCandidateGroup> groupedBySourceClassMap = new HashMap<>();
            for (ExtractClassCandidateRefactoring candidate : extractClassCandidateList) {
                if (groupedBySourceClassMap.containsKey(candidate.getSourceEntity())) {
                    groupedBySourceClassMap.get(candidate.getSourceEntity()).addCandidate(candidate);
                } else {
                    ExtractClassCandidateGroup group = new ExtractClassCandidateGroup(candidate.getSourceEntity());
                    group.addCandidate(candidate);
                    groupedBySourceClassMap.put(candidate.getSourceEntity(), group);
                }
            }
            for (String sourceClass : groupedBySourceClassMap.keySet()) {
                groupedBySourceClassMap.get(sourceClass).groupConcepts();
            }
            return new TreeSet<>(groupedBySourceClassMap.values());
        } else {
            return new TreeSet<>();
        }
    }
}
