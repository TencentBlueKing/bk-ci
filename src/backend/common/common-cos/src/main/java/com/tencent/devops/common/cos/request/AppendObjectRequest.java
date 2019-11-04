/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by schellingma on 2017/04/09.
 * Powered By Tencent
 */
public class AppendObjectRequest extends AbstractRequest {
    private final String objectName;
    private final byte[] inputBytes;
    private final MediaType mediaType;
    private final Map<String, String> headerParams;
    private final Map<String, String> queryParams;

    public AppendObjectRequest(final String bucketName,
                               final String objectName,
                               final Map<String, String> objectMetaMap,
                               final byte[] bytes,
                               final long positionAppend,
                               final String contentType) throws COSException {
        super(bucketName);
        if(StringUtils.isBlank(objectName)) {
            throw new COSException("Invalid object name");
        }
        this.objectName = objectName;

        if(bytes == null) {
            throw new COSException("Invalid input bytes");
        }
        this.inputBytes = bytes;
        if(StringUtils.isBlank(contentType)) {
            this.mediaType = DEFAULT_MEDIA_TYPE;
        } else {
            this.mediaType = MediaType.parse(contentType);
        }

        this.headerParams = new HashMap<>();
        if(positionAppend == 0)
        {
            if(objectMetaMap != null && !objectMetaMap.isEmpty()) {
                this.headerParams.putAll(objectMetaMap);
            }
        }

        //此处使用 LinkedHashMap, 因为请求url参数也会进入签名计算过程，顺序相关
        this.queryParams = new LinkedHashMap<>();
        this.queryParams.put("append", "");
        this.queryParams.put("position", String.valueOf(positionAppend));
    }

    @Override
    public Map<String, String> getQueryParams() {
        return this.queryParams;
    }

    @Override
    public Map<String, String> getHeaderParams() {
        Map<String, String> headers = new HashMap<>();
        if (!headerParams.isEmpty()) {
            headerParams.forEach((k, v) -> headers.put(String.format("X-COS-META-%s", k.toUpperCase()), v));
        }
        if(inputBytes != null) {
            headers.put("Content-Length", String.valueOf(inputBytes.length));
        }
        return headers;
    }

    @Override
    public Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.POST, RequestBody.create(mediaType, inputBytes));
    }

    @Override
    public String getPath() {
        return String.format("/%s", StringUtils.strip(objectName, " /"));
    }

}
