package org.jetbrains.research.intellijdeodorant.reporting;

import com.intellij.diagnostic.*;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.idea.IdeaLogger;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import java.awt.*;

public class GitHubErrorReporter extends ErrorReportSubmitter {
    @Override
    public boolean submit(@NotNull IdeaLoggingEvent[] events, String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<SubmittedReportInfo> consumer) {
        GitHubErrorBean errorBean = new GitHubErrorBean(events[0].getThrowable(), IdeaLogger.ourLastActionId);
        return doSubmit(events[0], parentComponent, consumer, errorBean, additionalInfo);
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private static boolean doSubmit(final IdeaLoggingEvent event,
                                    final Component parentComponent,
                                    final Consumer<SubmittedReportInfo> callback,
                                    final GitHubErrorBean bean,
                                    final String description) {
        final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);

        bean.setDescription(description);
        bean.setMessage(event.getMessage());

        if (event instanceof IdeaReportingEvent) {
            IdeaReportingEvent reportingEvent = (IdeaReportingEvent) event;
            IdeaPluginDescriptor descriptor = reportingEvent.getPlugin();
            if (descriptor != null) {
                bean.setPluginName(descriptor.getName());
                bean.setPluginVersion(descriptor.getVersion());
            }
        }

        Object data = event.getData();

        if (data instanceof LogMessage) {
            bean.setAttachments(((LogMessage) data).getIncludedAttachments());
        }

        ErrorReportInformation errorReportInformation = ErrorReportInformation
                .getUsersInformation(bean,
                        (ApplicationInfoEx) ApplicationInfo.getInstance(),
                        ApplicationNamesInfo.getInstance());

        final Project project = CommonDataKeys.PROJECT.getData(dataContext);

        final CallbackWithNotification notifyingCallback = new CallbackWithNotification(callback, project);
        AnonymousFeedbackTask task =
                new AnonymousFeedbackTask(project,
                        IntelliJDeodorantBundle.message("report.error.progress.dialog.text"),
                        true,
                        errorReportInformation,
                        notifyingCallback);
        if (project == null) {
            task.run(new EmptyProgressIndicator());
        } else {
            ProgressManager.getInstance().run(task);
        }
        return true;
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return IntelliJDeodorantBundle.message("report.error.to.plugin.vendor");
    }

    /**
     * Provides functionality to show an error report message to the user that gives a click-able link to the created issue.
     */
    static class CallbackWithNotification implements Consumer<SubmittedReportInfo> {

        private final Consumer<SubmittedReportInfo> myOriginalConsumer;
        private final Project myProject;

        CallbackWithNotification(Consumer<SubmittedReportInfo> originalConsumer, Project project) {
            this.myOriginalConsumer = originalConsumer;
            this.myProject = project;
        }

        @Override
        public void consume(SubmittedReportInfo reportInfo) {
            myOriginalConsumer.consume(reportInfo);

            if (reportInfo.getStatus().equals(SubmittedReportInfo.SubmissionStatus.FAILED)) {
                ReportMessages.GROUP.createNotification(
                        ReportMessages.ERROR_REPORT,
                        reportInfo.getLinkText(),
                        NotificationType.ERROR,
                        NotificationListener.URL_OPENING_LISTENER).setImportant(false).notify(myProject);
            } else {
                ReportMessages.GROUP.createNotification(
                        ReportMessages.ERROR_REPORT,
                        reportInfo.getLinkText(),
                        NotificationType.INFORMATION,
                        NotificationListener.URL_OPENING_LISTENER).setImportant(false).notify(myProject);
            }
        }
    }
}