package com.tencent.bk.codecc.defect.component;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.schedule.api.ServiceFSRestResource;
import com.tencent.bk.codecc.schedule.vo.FileIndexVO;
import com.tencent.devops.common.storage.StorageService;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.storage.constant.StorageType;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提取svn/git变更记录的工具类，主要是用来关联告警作者
 *
 * @version V3.0
 * @date 2018/5/28
 */
@Component
@Slf4j
public class ScmJsonComponent
{
    public static final String SCM_JSON = "SCM_JSON";

    public static final String AGGREGATE = "AGGREGATE";

    private static final String SCM_JSON_FILE_POSTFIX = "_scm_blame.json";

    private static final String REPO_INFO_FILE_POSTFIX = "_scm_info.json";

    private static final String SCM_URL_JSON = "_scm_url.json";

    /**
     * 压缩过的告警文件的后缀
     */
    private static final String DEFECTS_FILE_POSTFIX = "_tool_scan_data.json";

    /**
     * 原生的告警文件的后缀
     */
    private static final String RAW_DEFECTS_FILE_POSTFIX = "_tool_scan_file.json";

    private static final String FILE_MD5_POSTFIX = "_md5.json";

    @Autowired
    private Client client;

    @Autowired
    private StorageService storageService;


    /**
     * 创建文件的索引
     *
     * @param fileName
     * @param type
     * @return
     */
    public Boolean upload(String localFilePath, String fileName, String type) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            log.error("upload {} file not exist!", localFilePath);
        }
        try {
            if (storageService.getStorageType().equals(StorageType.NFS.code())) {
                return true;
            }
            //上传文件
            String url = storageService.upload(localFilePath,fileName,type);
            //更新索引
            Result<FileIndexVO> result = client.get(ServiceFSRestResource.class)
                    .updateUploadInfo(new FileIndexVO(fileName, null, type,
                            storageService.getStorageType(), url, 1));
            if (result.isNotOk() || result.getData() == null) {
                log.error("upload {} file fail!", localFilePath);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("upload " + localFilePath + " file cause error!", e);
        }
        return false;
    }

    /**
     * 创建文件的索引
     *
     * @param fileName
     * @param type
     * @return
     */
    public String index(String fileName, String type)
    {
        //获取风险系数值
        Result<FileIndexVO> result = client.get(ServiceFSRestResource.class).index(fileName, type);

        if (result.isNotOk() || null == result.getData())
        {
            log.error("get file {} index fail: {}", fileName, result);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{fileName}, null);
        }
        FileIndexVO fileIndex = result.getData();
        return getLocalFilePath(fileIndex);
    }

    /**
     * 获取scm_jsom文件的索引
     * 如果文件不在本地，下载
     * @param fileName
     * @param type
     * @return
     */
    public String getFileIndex(String fileName, String type)
    {
        //获取风险系数值
        Result<FileIndexVO> result = client.get(ServiceFSRestResource.class).getFileIndex(fileName, type);
        log.info("getFileIndex type : {} filename : {}, return {}", type, fileName,
                result != null ? JSONObject.toJSONString(result) : "null");
        if (result == null || result.isNotOk() || null == result.getData()) {
            log.error("get file {} index fail: {}", fileName, result);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{fileName}, null);
        }
        FileIndexVO fileIndex = result.getData();
        if (StringUtils.isEmpty(fileIndex.getFileName()))
        {
            log.error("file not found: {}, {}", fileName, result);
            return "";
        }
        return getLocalFilePath(fileIndex);
    }

    private String getLocalFilePath(FileIndexVO fileIndex){
        //判断文件是否在本地存在，否则下载文件
        String localFilePath = String.format("%s/%s", fileIndex.getFileFolder(), fileIndex.getFileName());
        if (!Files.exists(Paths.get(localFilePath))
                && storageService.ifNeedAndCanDownload(fileIndex.getStoreType(), fileIndex.getDownloadUrl())) {
            storageService.download(localFilePath, fileIndex.getStoreType(), fileIndex.getDownloadUrl());
        }
        return localFilePath;
    }


    /**
     * 从文件中提取代码库作者信息
     *
     * @param streamName
     * @param toolName
     * @return
     */
    public List<ScmBlameVO> loadAuthorInfo(String streamName, String toolName, String buildId)
    {
        // 初始化scm blame文件数据
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, SCM_JSON_FILE_POSTFIX);

        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, SCM_JSON_FILE_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info(fileIndex);
        String fileContent = readFileContent(fileIndex);
        JSONArray scmBlameArr = null;
        if (StringUtils.isNotEmpty(fileContent))
        {
            scmBlameArr = new JSONArray(fileContent);
        }

        log.info("load scm json successful");

        Map<String, ScmBlameVO> scmBlameMap = new HashMap<>();

        // 填充scm blame文件读取的信息
        if (scmBlameArr != null && scmBlameArr.length() > 0)
        {
            JsonUtil.INSTANCE.to(scmBlameArr.toString(), new TypeReference<List<ScmBlameVO>>() {}).forEach((it) -> {
                scmBlameMap.put(it.getFilePath(), it);
            });
        }

        return new ArrayList<>(scmBlameMap.values());
    }

    /**
     * 从文件中提取仓库信息
     *
     * @param streamName
     * @param buildId
     * @return
     */
    public JSONArray loadRepoInfo(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s%s", streamName, buildId, REPO_INFO_FILE_POSTFIX);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, REPO_INFO_FILE_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info(fileIndex);
        String fileContent = readFileContent(fileIndex);
        JSONArray resultJsonArr = null;
        if (StringUtils.isNotEmpty(fileContent))
        {
            resultJsonArr = new JSONArray(fileContent);
        }
        return resultJsonArr;
    }

    /**
     * 从文件中提取文件URL信息
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public String loadRepoFileUrl(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, SCM_URL_JSON);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, SCM_URL_JSON);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info(fileIndex);
        String fileContent = readFileContent(fileIndex);
        log.info("load scm url json successful");
        return fileContent;
    }

    /**
     * 从CFS上获取告警数据
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public String loadDefects(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, DEFECTS_FILE_POSTFIX);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, DEFECTS_FILE_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info(fileIndex);
        String fileContent = readFileContent(fileIndex);
        return fileContent;
    }

    /**
     * 从CFS上获取告警数据
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public String getFileMD5Index(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, FILE_MD5_POSTFIX);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, FILE_MD5_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info(fileIndex);
        return fileIndex;
    }

    /**
     * 从CFS上获取告警数据
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public FileMD5TotalModel loadFileMD5(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, FILE_MD5_POSTFIX);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, FILE_MD5_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }
        log.info("file index for {}, {}, {}, {}", streamName, toolName, buildId, fileIndex);
        return JsonUtil.INSTANCE.to(readFileContent(fileIndex), new TypeReference<FileMD5TotalModel>() {});
    }

    /**
     * 获取告警文件的大小
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public long getDefectFileSize(String streamName, String toolName, String buildId)
    {
        String fileIndex = getDefectFileIndex(streamName, toolName, buildId);

        if (StringUtils.isEmpty(fileIndex))
        {
            log.warn("文件[{}]不存在", fileIndex);
            return 0;
        }

        File file = new File(fileIndex);
        if (!file.exists())
        {
            log.warn("文件[{}]不存在", file.getAbsolutePath());
            return 0;
        }

        long fileLength = file.length();
        log.info("file: {}, size: {}", fileIndex, fileLength);
        return fileLength;
    }

    /**
     * 获取告警文件的索引
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public String getDefectFileIndex(String streamName, String toolName, String buildId)
    {
        String scmJsonFileName = String.format("%s_%s_%s%s", streamName, toolName, buildId, RAW_DEFECTS_FILE_POSTFIX);
        String fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        if (StringUtils.isEmpty(fileIndex))
        {
            scmJsonFileName = String.format("%s_%s%s", streamName, toolName, RAW_DEFECTS_FILE_POSTFIX);
            fileIndex = getFileIndex(scmJsonFileName, SCM_JSON);
        }

        log.info(fileIndex);
        return fileIndex;
    }

    /**
     * 获取告警文件的索引
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    public String loadRawDefects(String streamName, String toolName, String buildId)
    {
        String fileIndex = getDefectFileIndex(streamName, toolName, buildId);
        String fileContent = readFileContent(fileIndex);
        return fileContent;
    }

    /**
     * 读取文件内容
     *
     * @param filePath
     * @return
     */
    public static String readFileContent(String filePath)
    {
        if (filePath == null)
        {
            log.warn("文件路径为空");
            return "";
        }
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("文件[{}]不存在", filePath);
            return "";
        }

        //start read scm json
        String fileData = null;
        StringBuffer fileBuffer = new StringBuffer();
        try (FileInputStream fileInputStream = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, Charsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                fileBuffer.append(tempString);
            }
            fileData = fileBuffer.toString();
        } catch (IOException e) {
            log.error("ERROR!!!", e);
        }

        if (StringUtils.isEmpty(fileData))
        {
            fileData = "[]";
        }
        return fileData;
    }

}
