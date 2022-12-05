package com.tencent.devops.common.storage;

import com.tencent.devops.common.storage.sdk.BkRepoApi;
import com.tencent.devops.common.storage.service.AbstractStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.File;
import java.util.Map;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "bkrepo")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class BkRepoStorageService extends AbstractStorageService {


    @Value("${storage.bkrepo.username}")
    private String username;

    @Value("${storage.bkrepo.password}")
    private String password;

    @Value("${storage.bkrepo.project}")
    private String project;

    @Value("${storage.bkrepo.repo}")
    private String repo;

    @Value("${storage.bkrepo.host:#{null}}")
    private String bkrepoHost;

    @Value("${storage.bkrepo.schema:http}")
    protected String bkrepoSchema;

    @Autowired
    private BkRepoApi bkRepoApi;

    @Bean
    public BkRepoApi bkRepoApi() {
        return new BkRepoApi(username, password, project, repo, bkrepoHost, bkrepoSchema);
    }

    @Override
    public String upload(String subPath,String filename, File file) {
        return bkRepoApi.genericSimpleUpload(getUploadFilePath(subPath, filename), file);
    }

    @Override
    public Map<String, String> downloadHeaders() {
        Map<String, String> headers = bkRepoApi.getAuthHeaders();
        headers = bkRepoApi.setExpires(headers, getExpires());
        return headers;
    }


    @Override
    public String startChunk(String subPath, String filename) throws Exception {
        return bkRepoApi.startChunk(getUploadFilePath(subPath, filename));
    }

    @Override
    public Boolean chunkUpload(String subPath, String filename, File file, Integer chunkNo, String uploadId) {
        return bkRepoApi.genericChunkUpload(getUploadFilePath(subPath, filename), file, chunkNo, uploadId);
    }

    @Override
    public String finishChunk(String subPath, String filename, String uploadId)  {
        return bkRepoApi.genericFinishChunk(getUploadFilePath(subPath, filename), uploadId);
    }


}
