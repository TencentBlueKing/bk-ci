package com.tencent.devops.common.storage.service;

import com.tencent.devops.common.storage.StorageService;
import com.tencent.devops.common.storage.constant.StorageType;
import com.tencent.devops.common.util.OkhttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

@Slf4j
public abstract class AbstractStorageService implements StorageService {
    /**
     * 保存时间（默认永久，单位小时）
     * 0 表示永久
     */
    @Value("${storage.expires:0}")
    private Long expires;

    @Value("${storage.type:nfs}")
    private String storageType;

    @Override
    public String upload(String localFilePath, String subPath, String filename) throws FileNotFoundException {
        File file = new File(Paths.get(localFilePath).toString());
        if (!file.exists()) {
            log.error(localFilePath + " file no exists, upload fail!.");
            throw new FileNotFoundException(localFilePath + " file no exists");
        }
        String url = upload(subPath, filename, file);
        log.info("upload {} to {} success! url: {}", filename, storageType, url);
        return url;
    }

    public abstract String upload(String subPath,String filename, File file);

    @Override
    public Boolean chunkUpload(String localFilePath, String subPath, String filename, Integer chunkNo, String uploadId)
            throws FileNotFoundException {
        File file = new File(Paths.get(localFilePath).toString());
        if (!file.exists()) {
            log.error(localFilePath + " file no exists, upload fail!.");
            throw new FileNotFoundException(localFilePath + " file no exists");
        }
        String uploadFilePath = subPath + "/" + filename;
        return chunkUpload(uploadFilePath, filename, file, chunkNo, uploadId);
    }

    public abstract Boolean chunkUpload(String uploadFilePath, String filename, File file,
                                       Integer chunkNo, String uploadId);

    @Override
    public void download(String localFilePath, String storageType, String urlOrPath)  {
        //如果存储类型不同，则无法获取
        if (storageType == null || storageType.equals(StorageType.NFS.code())) {
            return;
        }
        if (!this.storageType.equals(storageType)) {
            log.error("download {} request storage type {} not match current storage type {}",
                    urlOrPath, storageType, this.storageType);
            return;
        }
        OkhttpUtils.INSTANCE.downloadFile(urlOrPath, new File(Paths.get(localFilePath).toString()), downloadHeaders());
        log.info("download {} to {} success! storageType: {}", urlOrPath, localFilePath, storageType);
    }

    public Map<String, String> downloadHeaders() {
        return Collections.emptyMap();
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    @Override
    public String getStorageType() {
        return storageType;
    }

    @Override
    public Boolean ifNeedAndCanDownload(String storageType, String urlOrPath) {
        log.info("storageType : {}",storageType);
        //NFS不需要下载
        if(storageType == null || storageType.equals(StorageType.NFS.code())
                || !StringUtils.hasLength(urlOrPath)){
            return false;
        }
        return getStorageType().equals(storageType);
    }

    protected String getUploadFilePath(String subPath,String filename){
        return subPath + "/" + filename;
    }
}
