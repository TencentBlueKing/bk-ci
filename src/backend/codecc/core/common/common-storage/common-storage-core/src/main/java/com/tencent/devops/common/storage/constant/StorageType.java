package com.tencent.devops.common.storage.constant;

public enum StorageType {
    BKREPO("bkrepo"),
    NFS("nfs"),
    OSS("oss"),
    COS("cos"),
    S3("s3"),
    ;
    private String code;

    StorageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
