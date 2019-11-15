package com.tencent.bk.devops.atom.api;

import com.google.common.collect.Maps;
import com.tencent.bk.devops.atom.common.BuildType;
import com.tencent.bk.devops.atom.common.Constants;
import com.tencent.bk.devops.atom.utils.http.SdkUtils;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @version 1.0
 */
@Setter
@Getter
public class SdkEnv {

    private final static Logger logger = LoggerFactory.getLogger(SdkEnv.class);

    private BuildType buildType;
    private String projectId;
    private String agentId;
    private String secretKey;
    private String gateway;
    private String buildId;
    private String vmSeqId;

    private static SdkEnv instance;

    static Map<String, String> getSdkHeader() {
        Map<String, String> map = Maps.newHashMap();
        map.put(Header.AUTH_HEADER_DEVOPS_BUILD_TYPE, instance.buildType.name());

        map.put(Header.AUTH_HEADER_DEVOPS_PROJECT_ID, instance.projectId);

        map.put(Header.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY, instance.secretKey);

        map.put(Header.AUTH_HEADER_DEVOPS_AGENT_ID, instance.agentId);

        map.put(Header.AUTH_HEADER_DEVOPS_VM_SEQ_ID, instance.vmSeqId);

        map.put(Header.AUTH_HEADER_DEVOPS_BUILD_ID, instance.buildId);
        return map;
    }

    public static String projectId() {
        return instance.projectId;
    }

    public static String agentId() {
        return instance.agentId;
    }

    public static String buildId() {
        return instance.buildId;
    }

    public static String vmSeqId() {
        return instance.vmSeqId;
    }

    public static void init() throws IOException {
        String dataDir = System.getenv(Constants.DATA_DIR_ENV);
        if (dataDir == null || dataDir.trim().length() == 0 || !(new File(dataDir)).isDirectory()) {
            dataDir = System.getProperty("user.dir");
        }
        String sdkFile = ".sdk.json";
        File file = new File(dataDir + "/" + sdkFile);
        String json = FileUtils.readFileToString(file, Charset.defaultCharset());
        boolean flag = file.delete(); //读取完后删除文件
        logger.info("delete file result is:{}", flag);
        instance = JsonUtil.fromJson(json, SdkEnv.class);
    }

    public static String genUrl(String path) {
        if (path.startsWith("/")) {
            return getGatewayHost() + "/" + path.substring(1).trim();
        } else {
            return getGatewayHost() + "/" + path.trim();
        }
    }

    public static String getGatewayHost() {
        if (SdkUtils.hasProtocol(instance.gateway)) {
            return instance.gateway;
        } else {
            return "http://" + instance.gateway;
        }
    }

}
