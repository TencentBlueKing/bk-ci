package com.tencent.devops.common.auth.code

import com.tencent.devops.common.auth.api.BkAuthServiceCode

class BSCommonAuthServiceCode : AuthServiceCode {
    override fun id() = BkAuthServiceCode.COMMON.value
}
