package com.tencent.devops.common.cos.model.enums;

import org.apache.commons.lang3.StringUtils;

public enum BucketACLEnum {
    ACL_PRIVATE("prviate"),
    ACL_PUBLIC("public-read");

    private String acl;

    BucketACLEnum(String acl) {
        this.acl = acl;
    }

    public String getACL() {
        return acl;
    }

    public static BucketACLEnum parse(String acl) {
        for (BucketACLEnum t : BucketACLEnum.values()) {
            if (!StringUtils.isEmpty(acl) && acl.equals(t.getACL())) {
                return t;
            }
        }
        return null;
    }
}
