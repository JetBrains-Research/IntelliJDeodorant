package org.jetbrains.research.intellijdeodorant.reporting;

import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import javax.annotation.Nullable;
import java.util.*;

import static org.jetbrains.research.intellijdeodorant.reporting.InformationType.*;

/**
 * Provides functionality to create and send GitHub issues when an exception is thrown by a plugin.
 */
class AnonymousFeedback {
    private final static String TOKEN_FILE = "errorReporterToken";
    private final static String GIT_REPO_USER = "JetBrains-Research";
    private final static String GIT_REPO = "IntelliJDeodorant";
    private final static String ISSUE_LABEL_BUG = "bug";
    private final static String ISSUE_LABEL_AUTO_GENERATED = "auto-generated";
    private final static String GIT_ISSUE_TITLE = "[auto-generated:%s] %s";
    private final static String HTML_URL_TO_CREATE_NEW_ISSUE = "https://github.com/JetBrains-Research/IntelliJDeodorant/issues/new";
    private final static EnumMap<InformationType, String> usersInformationToPresentableForm
            = new EnumMap<>(InformationType.class);

    static {
        usersInformationToPresentableForm.put(PLUGIN_NAME, "Plugin Name");
        usersInformationToPresentableForm.put(PLUGIN_VERSION, "Plugin Version");
        usersInformationToPresentableForm.put(OS_NAME, "OS Name");
        usersInformationToPresentableForm.put(JAVA_VERSION, "Java Version");
        usersInformationToPresentableForm.put(JAVA_VM_VENDOR, "Java VM Vendor");
        usersInformationToPresentableForm.put(APP_NAME, "App Name");
        usersInformationToPresentableForm.put(APP_FULL_NAME, "App Full Name");
        usersInformationToPresentableForm.put(APP_VERSION_NAME, "App Version Name");
        usersInformationToPresentableForm.put(IS_EAP, "Is EAP");
        usersInformationToPresentableForm.put(APP_BUILD, "App Build");
        usersInformationToPresentableForm.put(APP_VERSION, "App Version");
        usersInformationToPresentableForm.put(LAST_ACTION, "Last Action");
        usersInformationToPresentableForm.put(PERMANENT_INSTALLATION_ID, "User's Permanent Installation ID");
    }

    private AnonymousFeedback() {
    }

    /**
     * Makes a connection to GitHub. Checks if there is an issue that is a duplicate and based on this, creates either a
     * new issue or comments on the duplicate (if the user provided additional information).
     *
     * @param errorReportInformation information collected by {@link ErrorReportInformation}
     * @return the report info that is then used in {@link GitHubErrorReporter} to show the user a balloon with the link
     * of the created issue.
     */
    static SubmittedReportInfo sendFeedback(ErrorReportInformation errorReportInformation) {

        final SubmittedReportInfo result;
        try {
            final String gitAccessToken = GitHubAccessTokenScrambler.decrypt(AnonymousFeedback.class.getResourceAsStream(TOKEN_FILE));

            GitHubClient client = new GitHubClient();
            client.setOAuth2Token(gitAccessToken);
            RepositoryId repoID = new RepositoryId(GIT_REPO_USER, GIT_REPO);
            IssueService issueService = new IssueService(client);

            Issue newGibHubIssue = createNewGibHubIssue(errorReportInformation);
            Issue duplicate = findFirstDuplicate(newGibHubIssue.getTitle(), issueService, repoID);
            boolean isNewIssue = true;
            if (duplicate != null) {
                String newErrorComment = generateGitHubIssueBody(errorReportInformation);
                issueService.createComment(repoID, duplicate.getNumber(), newErrorComment);
                newGibHubIssue = duplicate;
                isNewIssue = false;
            } else {
                newGibHubIssue = issueService.createIssue(repoID, newGibHubIssue);
            }

            final long id = newGibHubIssue.getNumber();
            final String htmlUrl = newGibHubIssue.getHtmlUrl();
            final String message = IntelliJDeodorantBundle.message(isNewIssue ? "git.issue.text" : "git.issue.duplicate.text", htmlUrl, id);
            result = new SubmittedReportInfo(htmlUrl, message, isNewIssue ? SubmissionStatus.NEW_ISSUE : SubmissionStatus.DUPLICATE);
            return result;
        } catch (Exception e) {
            return new SubmittedReportInfo(HTML_URL_TO_CREATE_NEW_ISSUE,
                    IntelliJDeodorantBundle.message("report.error.connection.failure",
                            HTML_URL_TO_CREATE_NEW_ISSUE),
                    SubmissionStatus.FAILED);
        }
    }

    /**
     * Collects all issues on the repo and finds the first duplicate that has the same title. For this to work, the title
     * contains the hash of the stack trace.
     *
     * @param uniqueTitle title of the newly created issue. Since for auto-reported issues the title is always the same,
     *                    it includes the hash of the stack trace. The title is used so that I don't have to match
     *                    something in the whole body of the issue.
     * @param service     issue-service of the GitHub lib that lets you access all issues
     * @param repo        the repository that should be used
     * @return the duplicate if one is found or null
     */
    @Nullable
    private static Issue findFirstDuplicate(String uniqueTitle, final IssueService service, RepositoryId repo) {
        Map<String, String> searchParameters = new HashMap<>(2);
        searchParameters.put(IssueService.FILTER_STATE, IssueService.STATE_OPEN);
        final PageIterator<Issue> pages = service.pageIssues(repo, searchParameters);
        for (Collection<Issue> page : pages) {
            for (Issue issue : page) {
                if (issue.getTitle().equals(uniqueTitle)) {
                    return issue;
                }
            }
        }
        return null;
    }

    /**
     * Turns collected information of an error into a new (offline) GitHub issue
     *
     * @param errorReportInformation a map of the information. Note that I remove items from there when they should not go in the issue
     *                               body as well. When creating the body, all remaining items are iterated.
     * @return the new issue
     */
    private static Issue createNewGibHubIssue(ErrorReportInformation errorReportInformation) {
        String errorMessage = errorReportInformation.get(ERROR_MESSAGE);
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Unspecified error";
        }
        String errorHash = errorReportInformation.get(ERROR_HASH);
        if (errorHash == null) {
            errorHash = "";
        }

        final Issue gitHubIssue = new Issue();
        final String body = generateGitHubIssueBody(errorReportInformation);
        gitHubIssue.setTitle(String.format(GIT_ISSUE_TITLE, errorHash, errorMessage));
        gitHubIssue.setBody(body);
        Label bugLabel = new Label();
        bugLabel.setName(ISSUE_LABEL_BUG);
        Label autoGeneratedLabel = new Label();
        autoGeneratedLabel.setName(ISSUE_LABEL_AUTO_GENERATED);
        gitHubIssue.setLabels(Arrays.asList(autoGeneratedLabel, bugLabel));
        return gitHubIssue;
    }

    /**
     * Creates the body of the GitHub issue. It will contain information about the system, error report information
     * provided by the user, and the full stack trace. Everything is formatted using markdown.
     *
     * @param errorReportInformation details provided by {@link ErrorReportInformation}
     * @return a markdown string representing the GitHub issue body.
     */
    private static String generateGitHubIssueBody(ErrorReportInformation errorReportInformation) {
        String errorDescription = errorReportInformation.get(ERROR_DESCRIPTION);
        if (errorDescription == null) {
            errorDescription = "";
        }
        String stackTrace = errorReportInformation.get(ERROR_STACKTRACE);
        if (stackTrace == null || stackTrace.isEmpty()) {
            stackTrace = "invalid stacktrace";
        }

        StringBuilder result = new StringBuilder();
        if (!errorDescription.isEmpty()) {
            result.append(errorDescription);
            result.append("\n\n----------------------\n\n");
        }
        for (Map.Entry<InformationType, String> usersInformationEntry : usersInformationToPresentableForm.entrySet()) {
            result.append("- ");
            result.append(usersInformationEntry.getValue());
            result.append(": ");
            result.append(errorReportInformation.get(usersInformationEntry.getKey()));
            result.append("\n");
        }

        result.append("\n```\n");
        result.append(stackTrace);
        result.append("\n```\n");
        return result.toString();
    }
}