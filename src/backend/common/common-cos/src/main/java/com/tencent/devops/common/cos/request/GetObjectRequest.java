/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.exception.COSException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

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
