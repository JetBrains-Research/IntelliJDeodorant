package org.jetbrains.research.intellijdeodorant.reporting;

import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the sending of feedback into a background task that is run by {@link GitHubErrorReporter}.
 */
public class AnonymousFeedbackTask extends Backgroundable {
    private final Consumer<SubmittedReportInfo> myCallback;
    private final ErrorReportInformation errorReportInformation;

    AnonymousFeedbackTask(@Nullable Project project,
                          @NotNull String title,
                          boolean canBeCancelled,
                          ErrorReportInformation errorReportInformation,
                          final Consumer<SubmittedReportInfo> callback) {
        super(project, title, canBeCancelled);

        this.errorReportInformation = errorReportInformation;
        myCallback = callback;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        myCallback.consume(AnonymousFeedback.sendFeedback(errorReportInformation));
    }
}