package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

public class HeadBucketRequest extends AbstractRequest {

    public HeadBucketRequest(final String bucketName) throws COSException {
        super(bucketName);
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.HEAD, null);
    }
}
