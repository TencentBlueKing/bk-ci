package com.tencent.devops.common.storage.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.devops.common.storage.vo.BkRepoResult;
import com.tencent.devops.common.storage.vo.BkRepoStartChunkVo;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 封装BkRepo的接口调用
 */
@AllArgsConstructor
public class BkRepoApi {

    private String username;

    private String password;

    private String project;

    private String repo;

    private String bkrepoHost;

    protected String bkrepoSchema;


    public String genericSimpleUpload(String filepath, File file) {
        String url = String.format("%s://%s/generic/%s/%s/%s", bkrepoSchema, bkrepoHost, project, repo, filepath);
        OkhttpUtils.INSTANCE.doFileStreamPut(url, file, getAuthHeaders());
        return url + "?download=true";
    }

    public String startChunk(String filepath) throws Exception {
        String url = String.format("%s://%s/generic/block/%s/%s/%s", bkrepoSchema, bkrepoHost, project, repo, filepath);
        String resp = OkhttpUtils.INSTANCE.doHttpPost(url, "{}",getAuthHeaders());
        BkRepoResult<BkRepoStartChunkVo> result =
                JsonUtil.INSTANCE.to(resp, new TypeReference<BkRepoResult<BkRepoStartChunkVo>>(){});
        if(result == null || !result.isOk() || result.getData() == null){
            throw new Exception("startChunk : " + filepath + " return " +
                    JsonUtil.INSTANCE.toJson(result) + " cause error.");
        }
        return result.getData().getUploadId();
    }


    public Boolean genericChunkUpload(String filepath, File file,Integer chunkNo,String uploadId) {
        String url = String.format("%s://%s/generic/%s/%s/%s", bkrepoSchema, bkrepoHost, project, repo, filepath);
        Map<String,String> headers = getAuthHeaders();
        if (chunkNo != null) {
            headers.put("X-BKREPO-SEQUENCE", chunkNo.toString());
        }
        if (StringUtils.hasLength(uploadId)) {
            headers.put("X-BKREPO-UPLOAD-ID", uploadId);
        }
        OkhttpUtils.INSTANCE.doFileStreamPut(url, file, getAuthHeaders());
        return true;
    }

    public String genericFinishChunk(String filepath, String uploadId) {
        String url = String.format("%s://%s/generic/block/%s/%s/%s", bkrepoSchema, bkrepoHost, project, repo, filepath);
        Map<String,String> headers = getAuthHeaders();
        if (StringUtils.hasLength(uploadId)) {
            headers.put("X-BKREPO-UPLOAD-ID", uploadId);
        }
        OkhttpUtils.INSTANCE.doHttpPut(url, "{}", getAuthHeaders());
        return url + "?download=true";
    }

    /**
     * 认证头信息
     * @return
     */
    public Map<String, String> getAuthHeaders() {
        String base64Src = username + ":" + password;
        String base64 = Base64.getEncoder().encodeToString(base64Src.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + base64);
        return headers;
    }

    /**
     * 设置上传文件的超时信息
     * @param headers
     * @param expires
     * @return
     */
    public Map<String, String> setExpires(Map<String, String> headers, Long expires) {
        Map<String, String> newHeaders = CollectionUtils.isEmpty(headers) ? new HashMap<>() : headers;
        if (expires == null || expires < 0) {
            expires = 0L;
        }
        newHeaders.put("X-BKREPO-EXPIRES", expires.toString());
        return headers;
    }



}
