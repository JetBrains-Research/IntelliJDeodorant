package org.jetbrains.research.intellijdeodorant.ide.fus.collectors;

import com.intellij.concurrency.JobScheduler;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.ide.fus.IntelliJDeodorantLogger;

import java.util.concurrent.TimeUnit;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.getNumberOfLinesInMethod;

public class IntelliJDeodorantCounterCollector {
    private IntelliJDeodorantCounterCollector() {
        JobScheduler.getScheduler().scheduleWithFixedDelay(
                IntelliJDeodorantCounterCollector::trackRegistered,
                LOG_INITIAL_DELAY_MIN.longValue(),
                LOG_DELAY_MIN.longValue(),
                TimeUnit.MINUTES
        );
    }

    private static IntelliJDeodorantCounterCollector instance;

    private static final Integer LOG_DELAY_MIN = 24 * 60;
    private static final Integer LOG_INITIAL_DELAY_MIN = 11;

    private static final EventLogGroup group = new EventLogGroup("dbp.ijd.count", IntelliJDeodorantLogger.version);

    public void refactoringFound(Project project, String name, Integer total) {
        if (total <= 0) return;

        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("name", name)
                .addData("total", total);
        IntelliJDeodorantLogger.log(group, "refactoring.found", data);
    }

    public void extractMethodRefactoringApplied(Project project, ASTSlice slice, PsiMethod extractedMethod) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("extracted_statements_count", slice.getSliceStatements().size())
                .addData("slice_nodes_count", slice.getSliceNodes().size())
                .addData("new_method_length", getNumberOfLinesInMethod(extractedMethod))
                .addData("new_method_parameters_count", extractedMethod.getParameterList().getParametersCount())
                .addData("original_method_statements_count", slice.getOriginalMethodStatementsCount())
                .addData("original_method_length_before", slice.getOriginalMethodLinesCount())
                .addData("original_method_length_after", getNumberOfLinesInMethod(slice.getSourceMethodDeclaration()))
                .addData("original_method_parameters_count", slice.getOriginalMethodParametersCount());
        IntelliJDeodorantLogger.log(group, "extract.method.applied", data);
    }

    public void moveMethodRefactoringApplied(Project project, Integer sourceAccessedMembers, Integer targetAccessedMembers,
                                             Integer methodLength, Integer methodParametersCount) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("source_accessed_members", sourceAccessedMembers)
                .addData("target_accessed_members", targetAccessedMembers)
                .addData("method_length", methodLength)
                .addData("method_parameters_count", methodParametersCount);
        IntelliJDeodorantLogger.log(group, "move.method.applied", data);
    }

    public void extractClassRefactoringApplied(Project project, Integer extractedFieldsCount, Integer extractedMethodsCount,
                                               Integer totalFieldsCountInOriginalClass, Integer totalMethodsCountInOriginalClass) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("extracted_fields_count", extractedFieldsCount)
                .addData("extracted_methods_count", extractedMethodsCount)
                .addData("original_class_fields_count", totalFieldsCountInOriginalClass)
                .addData("original_class_methods_count", totalMethodsCountInOriginalClass);
        IntelliJDeodorantLogger.log(group, "extract.class.applied", data);
    }

    public void typeStateCheckingRefactoringApplied(Project project, Integer totalNumberOfCaseStatements,
                                                    Double averageNumberOfStatementsPerCase) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("case_statements_count", totalNumberOfCaseStatements)
                .addData("average_number_of_statements_per_case", averageNumberOfStatementsPerCase);
        IntelliJDeodorantLogger.log(group, "replace.conditional.type.applied", data);
    }

    public static IntelliJDeodorantCounterCollector getInstance() {
        if (instance == null) {
            instance = new IntelliJDeodorantCounterCollector();
        }
        return instance;
    }

    private static void trackRegistered() {
        IntelliJDeodorantLogger.log(group, "registered");
    }
}
