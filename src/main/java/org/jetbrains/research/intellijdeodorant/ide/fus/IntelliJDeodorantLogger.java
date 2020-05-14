package org.jetbrains.research.intellijdeodorant.ide.fus;

import com.intellij.internal.statistic.eventLog.*;

public class IntelliJDeodorantLogger {
    static final StatisticsEventLoggerProvider loggerProvider = StatisticsEventLoggerKt.getEventLogProvider("DBP");
    static public final Integer version = loggerProvider.getVersion();

    static public void log(EventLogGroup group, String action) {
        loggerProvider.getLogger().log(group, action, false);
    }

    static public void log(EventLogGroup group, String action, FeatureUsageData data) {
        loggerProvider.getLogger().log(group, action, data.build(), false);
    }
}
