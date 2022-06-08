package com.tencent.devops.common.util;

import com.tencent.devops.common.constant.ComConstants;

public class NotifyUtils {
    public static String getTargetUrl(String projectId, String nameCn, long taskId, String codeccHost,
                                      String devopsHost, String createFrom) {
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom)) {
            return String.format("%s/codecc/%s/task/%s/detail", codeccHost, projectId, taskId);
        }

        if (isGitCi(projectId, nameCn)) {
            return String.format("%s/codecc/%s/task/%s/detail", codeccHost, projectId, taskId);
        }
        return String.format("%s/console/codecc/%s/task/%s/detail", devopsHost, projectId, taskId);
    }

    public static String getResolveHtmlEmail(String projectId, String nameCn, String codeccHost, String devopsHost, String htmlEmail) {
        if (isGitCi(projectId, nameCn)) {
            return htmlEmail.replaceAll(devopsHost + "/console", codeccHost);
        }
        return htmlEmail;
    }

    public static String getBotTargetUrl(String projectId, String nameCn, long taskId, String toolName, String codeccHost, String devopsHost) {
        if (isGitCi(projectId, nameCn)) {
            return codeccHost + String.format("/codecc/%s/task/%s/defect/compile/%s/list?dimension=DEFECT",
                projectId, taskId, toolName);
        }
        return devopsHost + String.format("/console/codecc/%s/task/%s/defect/compile/%s/list?dimension=DEFECT",
            projectId, taskId, toolName);
    }

    public static boolean isGitCi(String projectId, String nameCn) {
        return projectId.startsWith("git_") && nameCn.startsWith(projectId);
    }
}
