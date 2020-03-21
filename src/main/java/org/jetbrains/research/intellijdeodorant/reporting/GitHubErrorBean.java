package org.jetbrains.research.intellijdeodorant.reporting;

import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.util.ExceptionUtil;

import java.util.Collections;
import java.util.List;

/**
 * Collects information about thrown exception.
 */
class GitHubErrorBean {
    private String myExceptionHash;
    private final String stackTrace;
    private final String lastAction;
    private String message;
    private String description;
    private String pluginName;
    private String pluginVersion;
    private List<Attachment> attachments = Collections.emptyList();

    GitHubErrorBean(Throwable throwable, String lastAction) {
        this.stackTrace = throwable != null ? ExceptionUtil.getThrowableText(throwable) : null;
        this.lastAction = lastAction;
        if (throwable != null) {
            setMessage(throwable.getMessage());
            myExceptionHash = Integer.toHexString(stackTrace.hashCode());
        }
    }

    String getExceptionHash() {
        return myExceptionHash;
    }

    String getStackTrace() {
        return stackTrace;
    }

    String getLastAction() {
        return lastAction;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getPluginName() {
        return pluginName;
    }

    void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    String getPluginVersion() {
        return pluginVersion;
    }

    void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    List<Attachment> getAttachments() {
        return attachments;
    }

    void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

}