package org.jetbrains.research.intellijdeodorant.ide.fus.collectors;

import com.intellij.concurrency.JobScheduler;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.intellijdeodorant.ide.fus.IntelliJDeodorantLogger;

import java.util.concurrent.TimeUnit;

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

    public void extractMethodRefactoringApplied(Project project, Integer extractedStatementsCount) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("extracted.statements.count", extractedStatementsCount);
        IntelliJDeodorantLogger.log(group, "extract.method.applied", data);
    }

    public void moveMethodRefactoringApplied(Project project, Integer sourceAccessedMembers, Integer targetAccessedMembers) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("source.accessed.members", sourceAccessedMembers)
                .addData("target.accessed.members", targetAccessedMembers);
        IntelliJDeodorantLogger.log(group, "move.method.applied", data);
    }

    public void extractClassRefactoringApplied(Project project, Integer extractedFieldsCount, Integer extractedMethodsCount) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("extracted.fields.count", extractedFieldsCount)
                .addData("extracted.methods.count", extractedMethodsCount);
        IntelliJDeodorantLogger.log(group, "extract.class.applied", data);
    }

    public void typeStateCheckingRefactoringApplied(Project project, Double averageNumberOfStatementsPerCase) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("average.number.of.statements.per.case", averageNumberOfStatementsPerCase);
        IntelliJDeodorantLogger.log(group, "type.state.checking.applied", data);
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
