package com.tencent.devops.common.cos.model.enums;

import org.apache.commons.lang3.StringUtils;

public enum HttpMethodEnum {
    PUT("put"),
    DELETE("delete"),
    POST("post"),
    GET("get"),
    HEAD("head"),
    OPTION("option");

    private String type;

    HttpMethodEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static HttpMethodEnum parse(String type) {
        for (HttpMethodEnum t : HttpMethodEnum.values()) {
            if (!StringUtils.isEmpty(type) && type.equals(t.getType())) {
                return t;
            }
        }
        return null;
    }
}
