package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.BucketACLEnum;
import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liangyuzhou on 2017/2/10.
 * Powered By Tencent
 */
public class PutBucketRequest extends AbstractRequest {
    private final BucketACLEnum bucketACL;

    public PutBucketRequest(final String bucketName, final BucketACLEnum bucketACL) throws COSException {
        super(bucketName);
        if (bucketACL == null) {
            this.bucketACL = BucketACLEnum.ACL_PRIVATE;
        } else {
            this.bucketACL = bucketACL;
        }
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.PUT, RequestBody.create(DEFAULT_MEDIA_TYPE, ""));
    }

    @Override
    public Map<String, String> getHeaderParams() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-cos-acl", this.bucketACL.getACL());
        return headers;
    }
}
