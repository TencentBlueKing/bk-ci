package com.tencent.devops.common.cos.model.enums;

import org.apache.commons.lang3.StringUtils;

public enum ObjectTypeEnum {
    NORMAL("normal"),
    APPENDABLE("appendable");

    private String type;

    ObjectTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ObjectTypeEnum parse(String type) {
        for (ObjectTypeEnum t : ObjectTypeEnum.values()) {
            if (!StringUtils.isEmpty(type) && type.equals(t.getType())) {
                return t;
            }
        }
        return null;
    }
}
