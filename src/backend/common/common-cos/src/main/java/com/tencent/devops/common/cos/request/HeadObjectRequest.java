package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class HeadObjectRequest extends AbstractRequest {
    private final String objectName;

    public HeadObjectRequest(final String bucketName, final String objectName) throws COSException {
        super(bucketName);
        if (StringUtils.isEmpty(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.HEAD, null);
    }

    @Override
    public String getPath() {
        return String.format("/%s", StringUtils.strip(objectName, " /"));
    }

    public String getObjectName() {
        return objectName;
    }
}
