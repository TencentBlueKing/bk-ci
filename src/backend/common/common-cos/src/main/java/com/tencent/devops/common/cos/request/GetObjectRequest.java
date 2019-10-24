package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.exception.COSException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liangyuzhou on 2017/2/13.
 * Powered By Tencent
 */
public class GetObjectRequest extends AbstractRequest {
    private final String objectName;
    private final Long startByte;
    private final Long endByte;
    private final long expireSeconds;

    public GetObjectRequest(final String bucketName, final String objectName) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        this.startByte = null;
        this.endByte = null;
        this.expireSeconds = 24 * 60 * 60;
    }

    public GetObjectRequest(final String bucketName, final String objectName, final long expireSeconds) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        this.startByte = null;
        this.endByte = null;
        this.expireSeconds = expireSeconds;
    }

    public GetObjectRequest(final String bucketName, final String objectName, final long rangeStart, final long rangeEnd) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        this.startByte = rangeStart;
        this.endByte = rangeEnd;
        this.expireSeconds = 24 * 60 * 60;
    }

    public GetObjectRequest(final String bucketName, final String objectName, final long rangeStart, final long rangeEnd, final long expireSeconds) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        this.startByte = rangeStart;
        this.endByte = rangeEnd;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public Map<String, String> getHeaderParams() {
        Map<String, String> headerParams = new HashMap<>();
        if (startByte != null && endByte != null) {
            headerParams.put("Range", String.format("bytes=%d-%d", startByte, endByte));
        }
        return headerParams;
    }

    @Override
    public String getPath() {
        return String.format("/%s", StringUtils.strip(objectName, " /"));
    }

    @Override
    public long getSignExpireSeconds() {
        return expireSeconds;
    }

//    @Override
//    public SignTypeEnum getSignType() {
//        return SignTypeEnum.QUERY;
//    }
}
