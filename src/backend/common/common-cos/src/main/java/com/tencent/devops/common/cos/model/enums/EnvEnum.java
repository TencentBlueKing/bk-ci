package com.tencent.devops.common.cos.model.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by liangyuzhou on 2017/2/10.
 * Powered By Tencent
 */
public enum EnvEnum {
    OA_TEST("sztest.file.tencent-cloud.com"),
    IDC("gfp.tencent-cloud.com");

    private String type;

    EnvEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static EnvEnum parse(String type) {
        for (EnvEnum t : EnvEnum.values()) {
            if (!StringUtils.isEmpty(type) && type.equals(t.getType())) {
                return t;
            }
        }
        return null;
    }
}
