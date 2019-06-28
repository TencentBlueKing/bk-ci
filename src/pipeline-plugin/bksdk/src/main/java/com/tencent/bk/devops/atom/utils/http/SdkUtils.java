package com.tencent.bk.devops.atom.utils.http;

import com.tencent.bk.devops.atom.common.Constants;

import java.io.File;

/**
 * SDK工具类
 */
public class SdkUtils {

    /**
     * 获取插件文件路径前缀
     * @return 插件文件路径前缀
     */
    public static String getDataDir() {
        String dataDir = System.getenv(Constants.DATA_DIR_ENV);
        if (dataDir == null || dataDir.trim().length() == 0 || !(new File(dataDir)).isDirectory()) {
            dataDir = System.getProperty("user.dir");
        }
        return dataDir;
    }
}
