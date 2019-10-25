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
