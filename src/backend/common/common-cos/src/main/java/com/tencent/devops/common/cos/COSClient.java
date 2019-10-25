package com.tencent.devops.common.cos;

import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.request.*;
import com.tencent.devops.common.cos.response.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by liangyuzhou on 2017/2/9.
 * Powered By Tencent
 */
public class COSClient implements ICOS {
    private final COSClientConfig cosClientConfig;
    private final OkHttpClient httpClient;
    private final Logger logger = LoggerFactory.getLogger(COSClient.class);

    public COSClient(final COSClientConfig cosClientConfig) throws COSException {
        this.cosClientConfig = checkClientConfig(cosClientConfig);
        httpClient = new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.HOURS)
                .writeTimeout(1, TimeUnit.HOURS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public COSClientConfig getCosClientConfig() {
        return this.cosClientConfig;
    }

    /**
     * 检查或初始化默认COS客户端参数
     * @param cosClientConfig COS客户端配置对象
     * @return 默认值填充后的COS客户端配置对象
     * @throws COSException 异常
     */
    private COSClientConfig checkClientConfig(final COSClientConfig cosClientConfig) throws COSException {
        //如果不是请求spm得到的sign，则必须带有secretId和secretKey
        if (!cosClientConfig.isFromSpm()) {
            if (StringUtils.isEmpty(cosClientConfig.getSecretId())) {
                throw new COSException("Invalid secret id");
            }
            if (StringUtils.isEmpty(cosClientConfig.getSecretKey())) {
                throw new COSException("Invalid secret key");
            }
        }

        if (cosClientConfig.getAppId() == null) {
            throw new COSException("Invalid app id");
        }
        if (cosClientConfig.getEnv() == null) {
            throw new COSException("Invalid env param");
        }
        if (StringUtils.isEmpty(cosClientConfig.getRegion())) {
            throw new COSException("Invalid region");
        }
        return cosClientConfig;
    }

    private <T extends BaseResponse> T call(final AbstractRequest request, final Class<T> classOfResponse) throws COSException {
        final Request httpRequest = request.getRequest(this.cosClientConfig);
        logger.info("Start the call the url " + httpRequest.url());
        try (final Response response = httpClient.newCall(httpRequest).execute()) {
            T t;
            try {
                t = classOfResponse.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.warn("Fail to call the cos client", e);
                throw new COSException("Parse COS response exception", e);
            }
            t.parseResponse(response);
            return t;
        } catch (IOException e) {
            logger.warn("Fail to call the cos client", e);
            throw new COSException("Call COS api network exception", e);
        }
    }

    @Override
    public PutBucketResponse putBucket(final PutBucketRequest request) throws COSException {
        return call(request, PutBucketResponse.class);
    }

    @Override
    public HeadBucketResponse headBucket(HeadBucketRequest request) throws COSException {
        return call(request, HeadBucketResponse.class);
    }

    @Override
    public PutObjectResponse putObject(PutObjectRequest request) throws COSException {
        return call(request, PutObjectResponse.class);
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest request) throws COSException {
        return call(request, HeadObjectResponse.class);
    }

    @Override
    public GetObjectResponse getObject(GetObjectRequest request) throws COSException {
        return call(request, GetObjectResponse.class);
    }

    @Override
    public AppendObjectResponse appendObject(final AppendObjectRequest request) throws COSException {
        return call(request, AppendObjectResponse.class);
    }

    @Override
    public DeleteObjectResponse deleteObject(final DeleteObjectRequest request) throws COSException {
        return call(request, DeleteObjectResponse.class);
    }

    @Override
    public ListBucketResponse getBucket(final ListBucketRequest request) throws COSException {
        return call(request, ListBucketResponse.class);
    }

    @Override
    public DeleteBucketResponse deleteBucket(final DeleteBucketRequest request) throws COSException {
        return call(request, DeleteBucketResponse.class);
    }

    @Override
    public ClientGetObjectResponse clientGetObject(ClientGetObjectRequest request) throws COSException {
        final Request httpRequest = request.getRequest(this.cosClientConfig);
        ClientGetObjectResponse clientGetObjectResponse = new ClientGetObjectResponse();
        clientGetObjectResponse.setErrorMessage(null);
        clientGetObjectResponse.setSuccess(true);
        clientGetObjectResponse.setUrl(httpRequest.url().toString());
        return clientGetObjectResponse;
    }
}
