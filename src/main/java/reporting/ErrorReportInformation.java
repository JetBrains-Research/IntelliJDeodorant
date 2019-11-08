package reporting;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.util.SystemProperties;

import java.util.EnumMap;

import static reporting.InformationType.*;

/**
 * Collects information about the running IDEA and the error.
 */
class ErrorReportInformation {

    private final EnumMap<InformationType, String> information = new EnumMap<>(InformationType.class);

    private ErrorReportInformation(GitHubErrorBean error,
                                   ApplicationInfoEx appInfo,
                                   ApplicationNamesInfo namesInfo) {
        information.put(ERROR_DESCRIPTION, error.getDescription());
        information.put(PLUGIN_NAME, error.getPluginName());
        information.put(PLUGIN_VERSION, error.getPluginVersion());
        information.put(OS_NAME, SystemProperties.getOsName());
        information.put(JAVA_VERSION, SystemProperties.getJavaVersion());
        information.put(JAVA_VM_VENDOR, SystemProperties.getJavaVmVendor());
        information.put(APP_NAME, namesInfo.getProductName());
        information.put(APP_FULL_NAME, namesInfo.getFullProductName());
        information.put(APP_VERSION_NAME, appInfo.getVersionName());
        information.put(IS_EAP, Boolean.toString(appInfo.isEAP()));
        information.put(APP_BUILD, appInfo.getBuild().asString());
        information.put(APP_VERSION, appInfo.getFullVersion());
        information.put(PERMANENT_INSTALLATION_ID, PermanentInstallationID.get());
        information.put(LAST_ACTION, error.getLastAction());

        information.put(ERROR_MESSAGE, error.getMessage());
        information.put(ERROR_STACKTRACE, error.getStackTrace());
        information.put(ERROR_HASH, error.getExceptionHash());

        for (Attachment attachment : error.getAttachments()) {
            information.put(ATTACHMENT_NAME, attachment.getName());
            information.put(ATTACHMENT_VALUE, attachment.getEncodedBytes());
        }

    }

    static ErrorReportInformation getUsersInformation(GitHubErrorBean error,
                                                      ApplicationInfoEx appInfo,
                                                      ApplicationNamesInfo namesInfo) {
        return new ErrorReportInformation(error, appInfo, namesInfo);
    }

    public String get(InformationType informationType) {
        return information.get(informationType);
    }
}
