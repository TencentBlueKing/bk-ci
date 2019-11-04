package com.tencent.devops.common.cos.response;

import com.tencent.devops.common.cos.model.enums.ObjectTypeEnum;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HeadObjectResponse extends BaseResponse {
    private Map<String, String> metaMap;
    private Long size;
    private String sha1;
    private ObjectTypeEnum objectType;

    @Override
    public void parseResponse(Response response) {
        if (response.isSuccessful()) {
            setSuccess(true);
            final Map<String, String> metaMap = new HashMap<>();
            final Headers headers = response.headers();
            Set<String> headerNames = headers.names();
            for (String headerName : headerNames) {
                if (headerName.startsWith("X-COS-META-")) {
                    final String metaName = StringUtils.substringAfter(headerName, "X-COS-META-").toLowerCase();
                    metaMap.put(metaName, headers.get(headerName));
                } else if (headerName.equalsIgnoreCase("SIZE")) {
                    long size;
                    try {
                        size = Long.parseLong(headers.get(headerName));
                    } catch (NumberFormatException e) {
                        size = -1;
                    }
                    setSize(size);
                } else if (headerName.equalsIgnoreCase("ETAG")) {
                    setSha1(headers.get(headerName));
                } else if (headerName.equalsIgnoreCase("x-cos-object-type")) {
                    setObjectType(ObjectTypeEnum.parse(headers.get(headerName).toLowerCase()));
                }
            }
            setMetaMap(metaMap);
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    public Map<String, String> getMetaMap() {
        return metaMap;
    }

    public void setMetaMap(Map<String, String> metaMap) {
        this.metaMap = metaMap;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public ObjectTypeEnum getObjectType() {
        return objectType;
    }

    public void setObjectType(ObjectTypeEnum objectType) {
        this.objectType = objectType;
    }
}
