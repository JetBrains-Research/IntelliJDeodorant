package org.jetbrains.research.intellijdeodorant;

import com.intellij.compiler.ProblemsView;
import com.intellij.compiler.impl.ProblemsViewImpl;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiVariable;
import com.intellij.ui.content.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.core.ast.ASTReader;
import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.*;
import org.jetbrains.research.intellijdeodorant.core.distance.*;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckEliminationGroup;
import org.jetbrains.research.intellijdeodorant.ide.ui.AbstractRefactoringPanel;

import java.util.*;

public class JDeodorantFacade {
    public static void runAfterCompilationCheck(ProjectInfo projectInfo, Task task) {
        ApplicationManager.getApplication().invokeLater(() -> {
            List<PsiClass> classes = projectInfo.getClasses();

            if (!classes.isEmpty()) {
                VirtualFile[] virtualFiles = classes.stream().map(classObject -> classObject.getContainingFile().getVirtualFile()).toArray(VirtualFile[]::new);
                Project project = classes.iterator().next().getContainingFile().getProject();

                ErrorTreeViewConfiguration configuration = ErrorTreeViewConfiguration.getInstance(project);
                boolean wasHideWarnings = configuration.isHideWarnings();
                boolean wasHideInfoMessages = configuration.isHideInfoMessages();
                configuration.setHideWarnings(true);
                configuration.setHideInfoMessages(true);

                CompilerManager compilerManager = CompilerManager.getInstance(project);
                myCompileStatusNotification callback = new myCompileStatusNotification(task, project, wasHideWarnings, wasHideInfoMessages);
                CompileScope compileScope = compilerManager.createFilesCompileScope(virtualFiles);
                compilerManager.make(compileScope, callback);
            } else {
                ProgressManager.getInstance().run(task);
            }
        });
    }

    private static class myCompileStatusNotification implements CompileStatusNotification {
        private Task task;
        private Project project;
        boolean wasHideWarnings;
        boolean wasHideInfoMessages;

        private myCompileStatusNotification(Task task, Project project, boolean wasHideWarnings, boolean wasHideInfoMessages) {
            this.task = task;
            this.project = project;
            this.wasHideWarnings = wasHideWarnings;
            this.wasHideInfoMessages = wasHideInfoMessages;
        }

        @Override
        public void finished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ErrorTreeViewConfiguration configuration = ErrorTreeViewConfiguration.getInstance(project);
                configuration.setHideWarnings(wasHideWarnings);
                configuration.setHideInfoMessages(wasHideInfoMessages);
            });

            if (errors == 0 && !aborted) {
                ProgressManager.getInstance().run(task);
            } else {
                AbstractRefactoringPanel.showCompilationErrorNotification(task.getProject());
            }
        }
    }

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo
                                                                                                     project, ProgressIndicator indicator) {
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

    public static TreeSet<ExtractClassCandidateGroup> getExtractClassRefactoringOpportunities(ProjectInfo
                                                                                                      project, ProgressIndicator indicator) {
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

    public static Set<ASTSliceGroup> getExtractMethodRefactoringOpportunities(ProjectInfo
                                                                                      project, ProgressIndicator indicator) {
        new ASTReader(project, indicator);

        SystemObject systemObject = ASTReader.getSystemObject();
        Set<ASTSliceGroup> extractedSliceGroups = new TreeSet<>();
        if (systemObject != null) {
            Set<ClassObject> classObjectsToBeExamined = new LinkedHashSet<>(systemObject.getClassObjects());

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

    private static void processMethod(final Set<ASTSliceGroup> extractedSliceGroups, ClassObject
            classObject, MethodObject methodObject) {
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

    public static Set<TypeCheckEliminationGroup> getTypeCheckEliminationRefactoringOpportunities(ProjectInfo
                                                                                                         project, ProgressIndicator indicator) {
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
