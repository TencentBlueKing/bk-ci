package com.tencent.devops.common.util;

import java.util.HashMap;
import java.util.Map;

public class HttpPathUrlUtil {

    private static Map<String, String> urlRouterMap = new HashMap<String, String>()
    {{
        put("COVERITY", "compile/");
        put("KLOCWORK", "compile/");
        put("CCN", "");
        put("DUPC", "");
    }};


    // https://iwiki.oa.tencent.com/x/yJQTDQ
    public static String getTargetUrl(String devopsHost, String projectId, long taskId, String toolName)
    {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append(devopsHost)
            .append("/console/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list");
        return sb.toString();
    }

    public static String getLintTargetUrl(String devopsHost, String projectId, long taskId, String toolName, String entityId)
    {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append(devopsHost)
            .append("/console/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list")
            .append(String.format("?entityId=%s", entityId));
        return sb.toString();
    }

    public static String getCcnTargetUrl(String devopsHost, String projectId, long taskId, String toolName, String entityId, String filePath)
    {
        StringBuilder sb = new StringBuilder();
        String route = urlRouterMap.get(toolName);
        if (route == null) route = "lint/";

        sb.append(devopsHost)
            .append("/console/codecc/")
            .append(projectId)
            .append("/task/")
            .append(taskId)
            .append("/defect/")
            .append(route)
            .append(toolName)
            .append("/list")
            .append(String.format("?entityId=%s&filePath=%s", entityId, filePath));
        return sb.toString();
    }

    public static String getCodeccTargetUrl(String codeccHost, String projectId, long taskId) {
        StringBuilder sb = new StringBuilder();
        sb.append(codeccHost)
                .append("/codecc/")
                .append(projectId)
                .append("/task/")
                .append(taskId)
                .append("/settings/code");
        return sb.toString();
    }
}
