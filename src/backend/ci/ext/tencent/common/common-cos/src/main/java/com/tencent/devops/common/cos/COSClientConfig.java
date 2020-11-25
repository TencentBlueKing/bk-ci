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

package com.tencent.devops.common.cos;

import com.tencent.devops.common.cos.model.enums.EnvEnum;

public class COSClientConfig {
    private Long appId;
    private String secretId;
    private String secretKey;
    private String region;
    private EnvEnum env;
    private boolean fromSpm;

    private String spmBuId;
    private String spmSecretKey;

    public COSClientConfig(Long appId, String secretId, String secretKey, String region, EnvEnum env) {
        this.appId = appId;
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.env = env;
        this.fromSpm = false;
    }

    public COSClientConfig(Long appId, String spmBuId, String spmSecretKey) {
        this.appId = appId;
        this.region = "sz";
        this.env = EnvEnum.IDC;
        this.fromSpm = true;
        this.spmBuId = spmBuId;
        this.spmSecretKey = spmSecretKey;
    }

    public COSClientConfig appId(final Long appId) {
        this.appId = appId;
        return this;
    }

    public COSClientConfig secretId(final String secretId) {
        this.secretId = secretId;
        return this;
    }

    public COSClientConfig secretKey(final String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public COSClientConfig region(final String region) {
        this.region = region;
        return this;
    }

    public COSClientConfig env(final EnvEnum env) {
        this.env = env;
        return this;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public EnvEnum getEnv() {
        return env;
    }

    public void setEnv(EnvEnum env) {
        this.env = env;
    }

    public boolean isFromSpm() {
        return fromSpm;
    }

    public void setFromSpm(boolean fromSpm) {
        this.fromSpm = fromSpm;
    }

    public String getSpmBuId() {
        return spmBuId;
    }

    public void setSpmBuId(String spmBuId) {
        this.spmBuId = spmBuId;
    }

    public String getSpmSecretKey() {
        return spmSecretKey;
    }

    public void setSpmSecretKey(String spmSecretKey) {
        this.spmSecretKey = spmSecretKey;
    }
}
