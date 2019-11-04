package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.SignTypeEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import org.apache.commons.lang3.StringUtils;

public class ClientGetObjectRequest extends AbstractRequest {
    private String objectName;
    private long expireSeconds;

    public ClientGetObjectRequest(final String bucketName, final String objectName, final long expireSeconds) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public String getPath() {
        return objectName;
    }

    @Override
    public SignTypeEnum getSignType() {
        return SignTypeEnum.QUERY;
    }

    @Override
    public long getSignExpireSeconds() {
        return expireSeconds;
    }

}
