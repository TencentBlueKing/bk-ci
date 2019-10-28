package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.util.InputStreamRequestBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liangyuzhou on 2017/2/13.
 * Powered By Tencent
 */
public class PutObjectRequest extends AbstractRequest {

    private final String objectName;
    private final Map<String, String> objectMetaMap;
    private final InputStream inputStream;
    private final String objectInputType;
    private final byte[] inputBytes;
    private final MediaType mediaType;

    public PutObjectRequest(final String bucketName, final String objectName, final Map<String, String> objectMetaMap, final InputStream inputStream, final String contentType) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        if (objectMetaMap == null) {
            this.objectMetaMap = new HashMap<>();
        } else {
            this.objectMetaMap = objectMetaMap;
        }
        if (inputStream == null) {
            throw new COSException("Invalid input stream");
        }
        this.inputStream = inputStream;
        this.objectInputType = "stream";
        this.inputBytes = null;
        if (StringUtils.isEmpty(contentType)) {
            this.mediaType = DEFAULT_MEDIA_TYPE;
        } else {
            this.mediaType = MediaType.parse(contentType);
        }
    }

    public PutObjectRequest(final String bucketName, final String objectName, final Map<String, String> objectMetaMap, final byte[] bytes, final String contentType) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        if (objectMetaMap == null) {
            this.objectMetaMap = new HashMap<>();
        } else {
            this.objectMetaMap = objectMetaMap;
        }
        if (bytes == null) {
            throw new COSException("Invalid input bytes");
        }
        this.inputStream = null;
        this.objectInputType = "bytes";
        this.inputBytes = bytes;
        if (StringUtils.isEmpty(contentType)) {
            this.mediaType = DEFAULT_MEDIA_TYPE;
        } else {
            this.mediaType = MediaType.parse(contentType);
        }
    }

    @Override
    public Map<String, String> getHeaderParams() {
        Map<String, String> headers = new HashMap<>();
        if (!objectMetaMap.isEmpty()) {
            objectMetaMap.forEach((k, v) -> headers.put(String.format("X-COS-META-%s", k.toUpperCase()), v));
        }
        return headers;
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        switch (objectInputType) {
            case "stream":
                return Pair.of(HttpMethodEnum.PUT, InputStreamRequestBody.create(mediaType, inputStream));
            case "bytes":
                return Pair.of(HttpMethodEnum.PUT, RequestBody.create(mediaType, inputBytes));
            default:
                return null;
        }
    }

    @Override
    public String getPath() {
        return String.format("/%s", StringUtils.strip(objectName, " /"));
    }
}
