package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.callback.AuthCallBackResource
import com.tencent.devops.auth.pojo.BkResult
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthCallBackResourceImpl(): AuthCallBackResource {

    override fun healthz(): BkResult<Boolean> {
        return BkResult(true)
    }
}