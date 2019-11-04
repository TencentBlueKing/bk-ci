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

package com.tencent.devops.common.cos.model.pojo;

import com.tencent.devops.common.cos.model.enums.ObjectTypeEnum;
import com.tencent.devops.common.cos.response.HeadObjectResponse;

import java.util.Map;

/**
 * Created by schellingma on 2017/05/18.
 * Powered By Tencent
 */
public class HeadObjectResult {
    private String objectName;
    private Map<String, String> metaMap;
    private Long size;
    private String sha1;
    private ObjectTypeEnum objectType;

    public String getObjectName() {
        return objectName;
    }

    public HeadObjectResult setObjectName(String objectName) {
        this.objectName = objectName;
        return this;
    }

    public Map<String, String> getMetaMap() {
        return metaMap;
    }

    public HeadObjectResult setMetaMap(Map<String, String> metaMap) {
        this.metaMap = metaMap;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public HeadObjectResult setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getSha1() {
        return sha1;
    }

    public HeadObjectResult setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public ObjectTypeEnum getObjectType() {
        return objectType;
    }

    public HeadObjectResult setObjectType(ObjectTypeEnum objectType) {
        this.objectType = objectType;
        return this;
    }

    public static HeadObjectResult fromHeadObjectResponse(final HeadObjectResponse headObjectResponse, final String objectName) {
        if(headObjectResponse == null) return null;
        return new HeadObjectResult()
                .setObjectName(objectName)
                .setMetaMap(headObjectResponse.getMetaMap())
                .setSize(headObjectResponse.getSize())
                .setSha1(headObjectResponse.getSha1())
                .setObjectType(headObjectResponse.getObjectType());
    }
}
