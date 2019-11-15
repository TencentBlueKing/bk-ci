package com.tencent.bk.devops.atom.utils.http;

import com.tencent.bk.devops.atom.common.Constants;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SDK工具类
 */
public class SdkUtils {

    /**
     * 获取插件文件路径前缀
     *
     * @return 插件文件路径前缀
     */
    public static String getDataDir() {
        String dataDir = System.getenv(Constants.DATA_DIR_ENV);
        if (dataDir == null || dataDir.trim().length() == 0 || !(new File(dataDir)).isDirectory()) {
            dataDir = System.getProperty("user.dir");
        }
        return dataDir;
    }

    public static String trimProtocol(String url) {
        String host = url;
        if (url.startsWith("http")) {
            Matcher matcher = Pattern.compile("(http[s]?://)([-.a-z0-9A-Z]+)([/]?.*)").matcher(host);
            if (matcher.matches()) {
                host = matcher.group(2);
            }
        }
        return host;
    }

    public static Boolean hasProtocol(String url) {
        Matcher matcher = Pattern.compile("(http[s]?://)([-.a-z0-9A-Z]+)([/]?.*)").matcher(url);
        return matcher.matches();
    }
}
