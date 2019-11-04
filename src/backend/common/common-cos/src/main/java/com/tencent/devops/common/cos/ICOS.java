package com.tencent.devops.common.cos;

import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.request.*;
import com.tencent.devops.common.cos.response.*;

public interface ICOS {
    PutBucketResponse putBucket(final PutBucketRequest request) throws COSException;
    HeadBucketResponse headBucket(final HeadBucketRequest request) throws COSException;
    PutObjectResponse putObject(final PutObjectRequest request) throws COSException;
    HeadObjectResponse headObject(final HeadObjectRequest request) throws COSException;
    ClientGetObjectResponse clientGetObject(final ClientGetObjectRequest request) throws COSException;
    GetObjectResponse getObject(final GetObjectRequest request) throws COSException;
    AppendObjectResponse appendObject(final AppendObjectRequest request) throws COSException;
    DeleteObjectResponse deleteObject(final DeleteObjectRequest request) throws COSException;
    ListBucketResponse getBucket(final ListBucketRequest request) throws COSException;
    DeleteBucketResponse deleteBucket(final DeleteBucketRequest request) throws COSException;
}
