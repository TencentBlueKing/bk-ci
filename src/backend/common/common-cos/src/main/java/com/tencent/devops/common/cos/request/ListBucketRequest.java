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

/**
 * Created by schellingma on 2017/04/27.
 * Powered By Tencent
 */
public class ListBucketRequest extends AbstractRequest {

    private String prefix;
    private String delimiter;
    private String marker;
    private int maxKeys;

    public ListBucketRequest(final String bucketName, final String prefix, final String delimiter,
                             final String marker, int maxKeys) throws COSException {
        super(bucketName);

        this.prefix = prefix;
        this.delimiter = delimiter;
        this.marker = marker;
        this.maxKeys = maxKeys;
    }


    public ListBucketRequest(final String bucketName, final String prefix) throws COSException {
        super(bucketName);

        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public ListBucketRequest setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public ListBucketRequest setDelimiter(final String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public String getMarker() {
        return marker;
    }

    public ListBucketRequest setMarker(final String marker) {
        this.marker = marker;
        return this;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public ListBucketRequest setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
        return this;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        if(!StringUtils.isEmpty(prefix)) {
            queryParams.put("prefix", prefix);
        }
        if(!StringUtils.isEmpty(delimiter)) {
            queryParams.put("delimiter", delimiter);
        }
        if(!StringUtils.isEmpty(marker)) {
            queryParams.put("marker", String.valueOf(marker));
        }
        if(maxKeys > 0) {
            queryParams.put("max-keys", String.valueOf(maxKeys));
        }

        return queryParams;
    }

    @Override
    public String toString() {
        return String.format("bucket: %s, prefix: %s, delimiter: %s, marker: %s, maxKeys: %s",
                getBucketName(), prefix, delimiter, marker, maxKeys);
    }






}
