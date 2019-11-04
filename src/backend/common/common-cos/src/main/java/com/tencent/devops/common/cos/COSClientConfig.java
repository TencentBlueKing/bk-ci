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
