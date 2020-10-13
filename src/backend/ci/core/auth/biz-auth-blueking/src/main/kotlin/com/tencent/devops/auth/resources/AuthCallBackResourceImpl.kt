package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.callback.AuthCallBackResource
import com.tencent.devops.auth.pojo.BkResult
import com.tencent.devops.common.web.RestResource

@RestResource
class AuthCallBackResourceImpl() : AuthCallBackResource {

    override fun healthz(): BkResult<Boolean> {
        return BkResult(true)
    }
}