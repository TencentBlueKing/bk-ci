package com.tencent.devops.common.auth.rbac.code

import com.tencent.devops.common.auth.code.PublicVarGroupAuthServiceCode

class RbacPublicVarGroupAuthServiceCode : PublicVarGroupAuthServiceCode {
    override fun id() = RbacAuthServiceCode.PIPELINE.value
}