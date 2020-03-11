package org.jetbrains.research.intellijdeodorant.ide.fus.collectors;

import com.intellij.concurrency.JobScheduler;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.intellijdeodorant.ide.fus.IntelliJDeodorantLogger;

import java.util.concurrent.TimeUnit;

public class IntelliJDeodorantCounterCollector {
    private IntelliJDeodorantCounterCollector() {}

    private static IntelliJDeodorantCounterCollector instance;

    private static final Integer LOG_DELAY_MIN = 24 * 60;
    private static final Integer LOG_INITIAL_DELAY_MIN = 11;

    private static final EventLogGroup group = new EventLogGroup("dbp.ijd.count", IntelliJDeodorantLogger.version);

    public void refactoringFound(Project project, String name, Integer total) {
        if (total <= 0) return;

        FeatureUsageData data = new FeatureUsageData().addProject(project).addData("name", name).addData("total", total);
        IntelliJDeodorantLogger.log(group, "refactoring.found", data);
    }

    public void refactoringApplied(Project project, String name) {
        FeatureUsageData data = new FeatureUsageData().addProject(project).addData("name", name);
        IntelliJDeodorantLogger.log(group, "refactoring.applied", data);
    }

    public static IntelliJDeodorantCounterCollector getInstance() {
        if (instance == null) {
            instance = new IntelliJDeodorantCounterCollector();
            JobScheduler.getScheduler().scheduleWithFixedDelay(
                    IntelliJDeodorantCounterCollector::trackRegistered,
                    LOG_INITIAL_DELAY_MIN.longValue(),
                    LOG_DELAY_MIN.longValue(),
                    TimeUnit.MINUTES
            );
        }
        return instance;
    }

    private static void trackRegistered() {
        IntelliJDeodorantLogger.log(group, "registered");
    }
}
