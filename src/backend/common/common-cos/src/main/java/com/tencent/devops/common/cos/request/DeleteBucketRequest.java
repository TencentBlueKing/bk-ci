package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by schellingma on 2017/05/08.
 * Powered By Tencent
 */
public class DeleteBucketRequest extends AbstractRequest {
    //TODO "必须在删除Bucket之下所有Object之后才能执行Delete Bucket。否则，返回403无权限" , 目前还没有实现，待实现

    public DeleteBucketRequest(final String bucketName) throws COSException {
        super(bucketName);
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.DELETE, null);
    }
}
