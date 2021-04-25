package com.tencent.devops.common.service.utils;

import org.apache.commons.lang.StringUtils;

public class ToolParamUtils {
    public static String trimUserName(String username) {
        if (StringUtils.isNotEmpty(username)) {
            int keyIndex = username.indexOf("(");
            if (keyIndex != -1) {
                return username.substring(0, keyIndex);
            }
            return username;
        }
        return username;
    }
}
